package com.changeflow.integration;

import com.changeflow.application.port.*;
import com.changeflow.domain.*;
import com.changeflow.integration.support.*;
import com.changeflow.integration.support.ConcurrencyTestConfiguration.*;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.http.*;
import java.sql.Connection;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Import(ConcurrencyTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StorageFailureHttpTest extends HttpTestSupport {
    @MockitoSpyBean com.changeflow.api.ProblemFactory problems;
    @Autowired Control control;
    @Autowired CredentialStore credentials;
    @Autowired UserAccessStore access;
    @Autowired HikariDataSource pool;
    @AfterEach void clear() { control.reset(); }
    private HttpResponse<String> edit(StoredSnapshot before, String token) throws Exception {
        UUID id = before.parent().request().id();
        return client.send(request("/api/change-requests/" + id, token).timeout(Duration.ofSeconds(25)).header("Content-Type", "application/json")
                .header("If-Match", "\"cr-" + id + "-v" + before.parent().revision() + "\"")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"title\":\"Must not partially commit\"}")).build(), HttpResponse.BodyHandlers.ofString());
    }
    private void unavailable(HttpResponse<String> response, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.headers().firstValue("ETag")).isEmpty();
        assertThat(response.headers().firstValue("WWW-Authenticate")).isEmpty();
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo(code);
        assertThat(response.body()).doesNotContain("Private", "SQL", "Exception", "jdbc:", "password", "secretHash");
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void actualCredentialAndAccessLockTimeoutsAreBoundedMvcStorageBusy(boolean credential) throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.Draft); var before = stored(seed.request().id());
        assertThat(jdbc.queryForObject("CALL LOCK_TIMEOUT()", Long.class)).isEqualTo(2000L);
        var held = new CountDownLatch(1); var release = new CountDownLatch(1); var workers = Executors.newSingleThreadExecutor();
        var holder = workers.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            if (credential) credentials.lock(owner.credentialId()).orElseThrow(); else access.lock(owner.userId()).orElseThrow();
            held.countDown(); ConcurrencyTestConfiguration.await(release, 15);
        }));
        HttpResponse<String> response; long elapsed;
        try {
            ConcurrencyTestConfiguration.await(held, 8); long start = System.nanoTime(); response = edit(before, owner.value()); elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
        } finally { release.countDown(); holder.get(15, TimeUnit.SECONDS); workers.shutdownNow(); }
        System.out.println("MEASURED " + (credential ? "credential-lock" : "user-lock") + " ms=" + elapsed);
        assertThat(elapsed).isBetween(1800L, 6500L); unavailable(response, "STORAGE_BUSY"); assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @Test void actualPoolExhaustionIsBoundedStorageBusyInTheBearerFilter() throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.Draft); var before = stored(seed.request().id());
        assertThat(pool.getConnectionTimeout()).isEqualTo(3000L);
        var held = new ArrayList<Connection>(); HttpResponse<String> response; long elapsed;
        try {
            for (int index = 0; index < pool.getMaximumPoolSize(); index++) held.add(pool.getConnection());
            long start = System.nanoTime(); response = edit(before, owner.value()); elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
        } finally { for (var connection : held) connection.close(); }
        System.out.println("MEASURED pool-acquisition ms=" + elapsed);
        assertThat(elapsed).isBetween(2800L, 7500L); unavailable(response, "STORAGE_BUSY"); assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void injectedStorageUnavailabilityHasTheSameSafeShapeInMvcAndFilter(boolean filter) throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.Draft); var before = stored(seed.request().id());
        control.hook = event -> { if (event.stage().equals(filter ? "CREDENTIAL_FIND" : "BEFORE_ACCESS")) throw new DataAccessResourceFailureException("Private controlled storage loss."); };
        var response = edit(before, owner.value()); control.reset();
        unavailable(response, "STORAGE_UNAVAILABLE"); assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @Test void actualTransactionDeadlineIsEnforcedAfterATestControlledDelay() throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.Draft); var before = stored(seed.request().id());
        control.hook = event -> {
            if (!event.stage().equals("AFTER_ACCESS")) return;
            try { Thread.sleep(10500); } catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
        };
        long start = System.nanoTime(); var response = edit(before, owner.value()); long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis(); control.reset();
        System.out.println("MEASURED transaction-deadline ms=" + elapsed + " http=" + response.statusCode() + " code=" + json.readTree(response.body()).path("code").asText());
        var failure = org.mockito.ArgumentCaptor.forClass(Throwable.class);
        org.mockito.Mockito.verify(problems).classify(failure.capture());
        for (Throwable cause = failure.getValue(); cause != null; cause = cause.getCause()) System.out.println("DEADLINE_CAUSE " + cause.getClass().getName() + " expired=" + "transaction timeout expired".equals(cause.getMessage()));
        assertThat(elapsed).isBetween(10000L, 17000L); unavailable(response, "STORAGE_BUSY"); assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @Test void realClosedPoolIsUnavailableAndRecoveryInspectionFindsNoWrite() throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.Draft); UUID id = seed.request().id(); var before = jdbc.queryForMap("SELECT * FROM change_request WHERE id=?", id);
        var recovery = new JdbcTemplate(new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword()));
        pool.close();
        unavailable(edit(storedValue(seed), owner.value()), "STORAGE_UNAVAILABLE");
        assertThat(recovery.queryForMap("SELECT * FROM change_request WHERE id=?", id)).isEqualTo(before);
        assertThat(recovery.queryForObject("SELECT COUNT(*) FROM review_decision WHERE request_id=?", Long.class, id)).isZero();
    }
    private StoredSnapshot storedValue(com.changeflow.application.RequestSnapshot seed) { return new StoredSnapshot(seed, List.of()); }
}
