package com.changeflow.integration;

import com.changeflow.ChangeFlowApplication;
import com.changeflow.application.*;
import com.changeflow.application.port.ChangeRequestStore;
import com.changeflow.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class RestartPersistenceTest {
    @TempDir Path directory;
    private final String username = "u" + UUID.randomUUID(), password = UUID.randomUUID().toString();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(ChangeFlowApplication.class).run(
                "--server.port=0", "--spring.datasource.url=jdbc:h2:file:" + directory.resolve("restart").toAbsolutePath().toString().replace('\\', '/'),
                "--spring.datasource.username=" + username, "--spring.datasource.password=" + password,
                "--spring.main.banner-mode=off", "--logging.level.root=ERROR");
    }
    private HttpResponse<String> call(ConfigurableApplicationContext context, String method, String path, String token, String body, String tag) throws Exception {
        int port = ((WebServerApplicationContext) context).getWebServer().getPort();
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(15));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (tag != null) builder.header("If-Match", tag);
        if (body != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private UUID create(ConfigurableApplicationContext context, String token) throws Exception {
        var response = call(context, "POST", "/api/change-requests", token, "{\"title\":\"Restart proposal\",\"description\":\"Restart details\",\"reason\":\"Restart reason\"}", null);
        assertThat(response.statusCode()).isEqualTo(201); return UUID.fromString(context.getBean(ObjectMapper.class).readTree(response.body()).path("id").asText());
    }
    private HttpResponse<String> action(ConfigurableApplicationContext context, UUID id, String token, String action, String body) throws Exception {
        var read = call(context, "GET", "/api/change-requests/" + id, token, null, null); assertThat(read.statusCode()).isEqualTo(200);
        var response = call(context, action.equals("edit") ? "PATCH" : "POST", "/api/change-requests/" + id + (action.equals("edit") ? "" : "/" + action), token, body, read.headers().firstValue("ETag").orElseThrow());
        assertThat(response.statusCode()).isEqualTo(200); return response;
    }
    private record DatabaseImage(String requests, String decisions, String access, String credentials, String migrations) {
        @Override public String toString() { return "DatabaseImage[private credential material redacted]"; }
    }
    private DatabaseImage image(ConfigurableApplicationContext context) throws Exception {
        var jdbc = context.getBean(JdbcTemplate.class); var json = context.getBean(ObjectMapper.class);
        return new DatabaseImage(json.writeValueAsString(jdbc.queryForList("SELECT * FROM change_request ORDER BY id")),
                json.writeValueAsString(jdbc.queryForList("SELECT * FROM review_decision ORDER BY request_id, resulting_request_version")),
                json.writeValueAsString(jdbc.queryForList("SELECT * FROM user_access ORDER BY user_id")),
                json.writeValueAsString(jdbc.queryForList("SELECT * FROM credential ORDER BY credential_id")),
                json.writeValueAsString(jdbc.queryForList("SELECT * FROM \"flyway_schema_history\" ORDER BY \"installed_rank\"")));
    }
    @Test void closingBothAppAndPoolThenReopeningTheSameFilePreservesEveryTableAndRepresentation() throws Exception {
        LocalAdministrationService.Provisioned owner, reviewer, remaining, revoked;
        UUID approvedId, returnedId, pendingId, nanosId;
        String approvedBody, approvedTag, nanosBody, nanosTag;
        DatabaseImage expected;
        HikariDataSource firstPool; EntityManagerFactory firstFactory;
        var first = start();
        try (first) {
            var jdbc = first.getBean(JdbcTemplate.class);
            for (String table : List.of("change_request", "review_decision", "user_access", "credential")) assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isZero();
            var admin = first.getBean(LocalAdministrationService.class);
            owner = admin.provision(Set.of(UserRole.Requester, UserRole.Reviewer)); reviewer = admin.provision(Set.of(UserRole.Reviewer)); remaining = admin.provision(Set.of(UserRole.Requester, UserRole.Reviewer));
            revoked = admin.issueCredential(owner.userId());
            approvedId = create(first, owner.value());
            action(first, approvedId, owner.value(), "submit", null);
            action(first, approvedId, reviewer.value(), "request-changes", "{\"explanation\":\"Restart feedback\"}");
            action(first, approvedId, owner.value(), "edit", "{\"title\":\"Revised restart proposal\"}");
            action(first, approvedId, owner.value(), "submit", "{}");
            var approval = action(first, approvedId, reviewer.value(), "approve", null); approvedBody = approval.body(); approvedTag = approval.headers().firstValue("ETag").orElseThrow();
            returnedId = create(first, owner.value()); action(first, returnedId, owner.value(), "submit", null); action(first, returnedId, reviewer.value(), "request-changes", "{\"explanation\":\"Still needs work\"}");
            pendingId = create(first, owner.value()); action(first, pendingId, owner.value(), "submit", null);
            nanosId = UUID.randomUUID(); Instant at = Instant.parse("2026-11-16T10:00:00.123456789Z");
            final UUID fixedId = nanosId; final UUID ownerId = owner.userId();
            new TransactionTemplate(first.getBean(PlatformTransactionManager.class)).executeWithoutResult(status -> first.getBean(ChangeRequestStore.class).insert(new ChangeRequest(fixedId, "Nanosecond title", "Details", "Reason", ownerId, RequestState.Draft, null, at, at)));
            var nanos = call(first, "GET", "/api/change-requests/" + nanosId, owner.value(), null, null); nanosBody = nanos.body(); nanosTag = nanos.headers().firstValue("ETag").orElseThrow();
            assertThat(nanosBody).contains("2026-11-16T10:00:00.123456789Z");
            admin.revokeCredential(revoked.credentialId()); admin.removeRole(owner.userId(), UserRole.Reviewer); admin.removeRole(reviewer.userId(), UserRole.Reviewer); admin.removeRole(remaining.userId(), UserRole.Requester);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Integer.class)).isEqualTo(4); assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review_decision", Integer.class)).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM credential", Integer.class)).isEqualTo(4); assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_access", Integer.class)).isEqualTo(3);
            expected = image(first); firstPool = first.getBean(HikariDataSource.class); firstFactory = first.getBean(EntityManagerFactory.class);
        }
        assertThat(first.isActive()).isFalse(); assertThat(firstPool.isClosed()).isTrue(); assertThat(firstFactory.isOpen()).isFalse();
        try (var second = start()) {
            assertThat(second.getBean(HikariDataSource.class)).isNotSameAs(firstPool); assertThat(second.getBean(EntityManagerFactory.class)).isNotSameAs(firstFactory);
            // Boolean equality deliberately prevents credential digests from appearing in assertion output.
            assertThat(expected.equals(image(second))).isTrue();
            var approved = call(second, "GET", "/api/change-requests/" + approvedId, owner.value(), null, null);
            assertThat(approved.statusCode()).isEqualTo(200); assertThat(approved.body()).isEqualTo(approvedBody); assertThat(approved.headers().firstValue("ETag")).hasValue(approvedTag);
            var json = second.getBean(ObjectMapper.class); assertThat(json.readTree(approved.body()).path("latestExplanation").asText()).isEqualTo("Restart feedback");
            var nanos = call(second, "GET", "/api/change-requests/" + nanosId, owner.value(), null, null); assertThat(nanos.body()).isEqualTo(nanosBody); assertThat(nanos.headers().firstValue("ETag")).hasValue(nanosTag);
            assertThat(json.readTree(call(second, "GET", "/api/change-requests/" + returnedId, owner.value(), null, null).body()).path("state").asText()).isEqualTo("ChangesRequested");
            assertThat(json.readTree(call(second, "GET", "/api/change-requests/" + pendingId, owner.value(), null, null).body()).path("state").asText()).isEqualTo("PendingReview");
            var denied = call(second, "GET", "/api/change-requests/" + approvedId, revoked.value(), null, null); assertThat(denied.statusCode()).isEqualTo(401); assertThat(denied.headers().firstValue("WWW-Authenticate")).hasValue("Bearer");
            assertThat(call(second, "GET", "/api/change-requests/" + approvedId, reviewer.value(), null, null).statusCode()).isEqualTo(404);
            assertThat(call(second, "GET", "/api/change-requests/" + approvedId, remaining.value(), null, null).statusCode()).isEqualTo(200);
            assertThat(call(second, "GET", "/api/change-requests/" + nanosId, remaining.value(), null, null).statusCode()).isEqualTo(404);
            assertThat(second.getBean(JdbcTemplate.class).queryForObject("SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"success\"=TRUE AND \"version\"='1'", Integer.class)).isEqualTo(1);
            assertThat(expected.equals(image(second))).isTrue();
        }
    }
}
