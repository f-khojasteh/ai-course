package com.changeflow.integration;

import com.changeflow.application.RequestSnapshot;
import com.changeflow.domain.*;
import com.changeflow.integration.support.*;
import com.changeflow.integration.support.ConcurrencyTestConfiguration.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Import(ConcurrencyTestConfiguration.class)
class OptimisticPersistenceTest extends HttpTestSupport {
    @Autowired Control control;
    record Result(RequestSnapshot snapshot, Throwable failure) { }
    @AfterEach void clear() { control.reset(); }
    private Result managedWrite(UUID id, String title) {
        try {
            return new Result(new TransactionTemplate(transactions).execute(status -> {
                var current = requests.find(id).orElseThrow();
                return requests.update(current.request().edit(new ChangeRequest.Edit(true, title, false, null, false, null), Instant.now()), current.revision());
            }), null);
        } catch (Throwable error) { return new Result(null, error); }
    }
    private HttpResponse<String> patch(StoredSnapshot value, String token, String body) throws Exception {
        var id = value.parent().request().id();
        return client.send(request("/api/change-requests/" + id, token).header("Content-Type", "application/json").header("If-Match", "\"cr-" + id + "-v" + value.parent().revision() + "\"")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void realManagedUpdatesInDistinctContextsCannotBothCommitTheObservedVersion() throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.Draft); UUID id = seed.request().id();
        var overlap = new Overlap("BEFORE_UPDATE", id); control.hook = overlap::accept; var pool = Executors.newFixedThreadPool(2);
        Result a, b;
        try { var first = pool.submit(() -> managedWrite(id, "Managed A")); var second = pool.submit(() -> managedWrite(id, "Managed B")); a = first.get(15, TimeUnit.SECONDS); b = second.get(15, TimeUnit.SECONDS); }
        finally { pool.shutdownNow(); control.hook = event -> { }; }
        assertThat(overlap.arrivals).hasSize(2); assertThat(overlap.arrivals.get(0).context()).isNotSameAs(overlap.arrivals.get(1).context());
        assertThat(a.failure() == null ^ b.failure() == null).isTrue();
        var loser = a.failure() == null ? b.failure() : a.failure(); boolean optimistic = false;
        for (Throwable cause = loser; cause != null; cause = cause.getCause()) optimistic |= cause instanceof org.springframework.dao.OptimisticLockingFailureException || cause instanceof jakarta.persistence.OptimisticLockException || cause instanceof org.hibernate.StaleStateException;
        assertThat(optimistic).isTrue();
        var winner = a.failure() == null ? a.snapshot() : b.snapshot(); var after = stored(id);
        assertThat(after.parent()).isEqualTo(winner); assertThat(after.parent().revision()).isEqualTo(seed.revision() + 1); assertThat(after.decisions()).isEmpty();
        var old = seed.request(); assertThat(winner.request()).isEqualTo(new ChangeRequest(id, a.failure() == null ? "Managed A" : "Managed B", old.description(), old.reason(), owner.userId(), old.state(), old.latestExplanation(), old.createdAt(), winner.request().updatedAt()));
    }
    @Test void noOpCommitChecksItsVersionWithoutIncrementAndRejectsARealInterveningWrite() throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.Draft); UUID id = seed.request().id(); var before = stored(id);
        var unchanged = patch(before, owner.value(), "{}"); assertThat(unchanged.statusCode()).isEqualTo(200); assertThat(stored(id)).isEqualTo(before);
        var pool = Executors.newFixedThreadPool(2);
        try (var pause = new Pause("UNCHANGED_CHECKED", id)) {
            control.hook = pause::accept;
            var noOp = pool.submit(() -> patch(before, owner.value(), "{}")); pause.awaitArrival();
            // Direct test-owned managed writer intentionally tests provider behavior independently of owner access serialization.
            var winner = pool.submit(() -> managedWrite(id, "Committed while no-op was waiting")).get(8, TimeUnit.SECONDS);
            assertThat(winner.failure()).isNull(); pause.close();
            var response = noOp.get(15, TimeUnit.SECONDS); control.hook = event -> { };
            assertThat(response.statusCode()).isEqualTo(412); assertThat(response.headers().firstValue("ETag")).isEmpty();
            assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo("STALE_VERSION");
            var after = stored(id); assertThat(after.parent()).isEqualTo(winner.snapshot()); assertThat(after.parent().revision()).isEqualTo(seed.revision() + 1); assertThat(after.decisions()).isEmpty();
            var writerEvent = control.events.stream().filter(event -> event.stage().equals("UPDATED") && event.id().equals(id)).findFirst().orElseThrow();
            assertThat(writerEvent.context()).isNotSameAs(pause.arrivals.get(0).context());
        } finally { pool.shutdownNow(); }
    }
    @Test void writeResponseRetainsItsOwnImmutableSnapshotAfterALaterWriter() throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.Draft); var before = stored(seed.request().id());
        var first = patch(before, owner.value(), "{\"title\":\"HTTP snapshot\"}"); assertThat(first.statusCode()).isEqualTo(200);
        var immutable = stored(seed.request().id()).parent(); String body = first.body();
        var later = managedWrite(seed.request().id(), "Later writer"); assertThat(later.failure()).isNull();
        var after = stored(seed.request().id());
        assertThat(immutable.request().title()).isEqualTo("HTTP snapshot"); assertThat(immutable.revision()).isEqualTo(seed.revision() + 1);
        assertThat(json.readTree(body).path("title").asText()).isEqualTo("HTTP snapshot"); assertThat(json.readTree(body).path("revision").asLong()).isEqualTo(immutable.revision());
        assertThat(after.parent()).isEqualTo(later.snapshot()); assertThat(after.parent().revision()).isEqualTo(immutable.revision() + 1);
        assertThat(after.parent().request().title()).isEqualTo("Later writer"); assertThat(after.decisions()).isEmpty();
    }
}
