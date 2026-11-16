package com.changeflow.integration;

import com.changeflow.application.ApplicationFailure;
import com.changeflow.domain.UserRole;
import com.changeflow.integration.support.HttpTestSupport;
import com.changeflow.api.validation.NormalizedText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.dao.*;
import org.springframework.test.context.*;
import org.springframework.web.bind.annotation.*;
import java.net.http.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@Import(ProblemResponseTest.ProbeConfiguration.class)
@ActiveProfiles("foundation-probes")
class ProblemResponseTest extends HttpTestSupport {
    private String credential;
    @Autowired FaultingCredentialStore faultingCredentials;
    @Autowired org.springframework.security.web.FilterChainProxy securityChains;
    @Autowired jakarta.servlet.ServletContext servletContext;
    @BeforeEach void authenticateProbe() { credential = user(UserRole.Requester).value(); }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {
        @Bean Probes probes() { return new Probes(); }
        @Bean @Primary FaultingCredentialStore faultingCredentialStore(
                @org.springframework.beans.factory.annotation.Qualifier("jpaCredentialStore") com.changeflow.application.port.CredentialStore delegate) {
            return new FaultingCredentialStore(delegate);
        }
    }
    static class FaultingCredentialStore implements com.changeflow.application.port.CredentialStore {
        private final com.changeflow.application.port.CredentialStore delegate;
        volatile String failure;
        FaultingCredentialStore(com.changeflow.application.port.CredentialStore delegate) { this.delegate = delegate; }
        @Override public java.util.Optional<Credential> find(UUID id) {
            if ("busy".equals(failure)) throw new CannotAcquireLockException("Private filter storage timeout.");
            if ("unavailable".equals(failure)) throw new DataAccessResourceFailureException("Private filter storage failure.");
            return delegate.find(id);
        }
        @Override public java.util.Optional<Credential> lock(UUID id) { return delegate.lock(id); }
        @Override public void insert(UUID id, UUID user, byte[] hash, java.time.Instant at) { delegate.insert(id, user, hash, at); }
        @Override public void revokeLocked(UUID id, java.time.Instant at) { delegate.revokeLocked(id, at); }
    }
    @Profile("foundation-probes") @RestController @RequestMapping("/_foundation")
    static class Probes {
        record Proposal(@NormalizedText(max = 200) String title) { }
        @PostMapping(value = "/body", consumes = "application/json", produces = "application/json")
        Proposal body(@Valid @RequestBody Proposal body) { return body; }
        @GetMapping("/failure/{code}") void failure(@PathVariable ApplicationFailure.Code code) { throw new ApplicationFailure(code); }
        @GetMapping("/storage/{kind}") void storage(@PathVariable String kind) {
            switch (kind) {
                case "busy" -> throw new CannotAcquireLockException("Private SQL must not escape.");
                case "unavailable" -> throw new DataAccessResourceFailureException("Private file path must not escape.");
                case "optimistic" -> throw new OptimisticLockingFailureException("Private revision data.");
                case "commit" -> throw new org.springframework.transaction.TransactionSystemException("Private commit acknowledgement.");
                case "constraint" -> throw new DataIntegrityViolationException("Private constraint detail.");
                case "pool" -> throw new org.springframework.transaction.CannotCreateTransactionException("Private pool detail.", new java.sql.SQLTransientConnectionException("Private timeout."));
                case "provider-timeout" -> throw new org.springframework.orm.jpa.JpaSystemException(new org.hibernate.TransactionException("transaction timeout expired"));
                case "provider-other" -> throw new org.springframework.orm.jpa.JpaSystemException(new org.hibernate.TransactionException("Private unrelated provider failure."));
                default -> throw new IllegalStateException("Private internal detail.");
            }
        }
        @GetMapping("/parameter") int parameter(@RequestParam @Min(1) int limit) { return limit; }
        @GetMapping("/denied") void denied() { throw new org.springframework.security.access.AccessDeniedException("Private denial."); }
        @GetMapping("/time") java.time.Instant time() { return java.time.Instant.parse("2026-11-16T10:00:00.123456789Z"); }
    }
    private HttpResponse<String> call(String method, String path, String body, String contentType, String accept) throws Exception {
        return super.call(method, path, credential, body, contentType, accept);
    }
    private void problem(HttpResponse<String> response, int status, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        var value = json.readTree(response.body());
        assertThat(value.path("status").intValue()).isEqualTo(status);
        assertThat(value.path("code").textValue()).isEqualTo(code);
        assertThat(value.path("type").textValue()).startsWith("urn:changeflow:problem:");
        assertThat(value.path("title").textValue()).isNotBlank();
        assertThat(value.path("detail").textValue()).isNotBlank();
        assertThat(value.path("instance").textValue()).startsWith("/");
        assertThat(response.body()).doesNotContain("Private", "exception", "trace", "SQL", "rejectedValue");
    }
    @Test void malformedValidationAndUnknownControlledFields() throws Exception {
        problem(call("POST", "/_foundation/body", "{", "application/json", "application/json"), 400, "INVALID_JSON");
        problem(call("POST", "/_foundation/body", "{\"title\":\" \"}", "application/json", "application/json"), 400, "INVALID_CONTENT");
        for (String field : new String[]{"requesterId", "roles", "state"}) {
            problem(call("POST", "/_foundation/body", "{\"title\":\"valid\",\"" + field + "\":\"ignored\"}", "application/json", "application/json"), 400, "FORBIDDEN_FIELD");
        }
        problem(call("POST", "/_foundation/body", "{\"title\":\"valid\",\"typo\":1}", "application/json", "application/json"), 400, "UNKNOWN_FIELD");
    }
    @Test void stateVersionAndPreconditionAreDistinct() throws Exception {
        problem(call("GET", "/_foundation/failure/INVALID_STATE", null, null, "text/html"), 409, "INVALID_STATE");
        problem(call("GET", "/_foundation/failure/STALE_VERSION", null, null, "text/html"), 412, "STALE_VERSION");
        problem(call("GET", "/_foundation/failure/PRECONDITION_REQUIRED", null, null, "text/html"), 428, "PRECONDITION_REQUIRED");
    }
    @Test void storageAndUnexpectedFailuresAreSafeAndDistinct() throws Exception {
        problem(call("GET", "/_foundation/storage/busy", null, null, "application/json"), 503, "STORAGE_BUSY");
        problem(call("GET", "/_foundation/storage/unavailable", null, null, "application/json"), 503, "STORAGE_UNAVAILABLE");
        problem(call("GET", "/_foundation/storage/optimistic", null, null, "application/json"), 412, "STALE_VERSION");
        problem(call("GET", "/_foundation/storage/unexpected", null, null, "application/json"), 500, "INTERNAL_ERROR");
        problem(call("GET", "/_foundation/storage/commit", null, null, "application/json"), 503, "STORAGE_UNAVAILABLE");
        problem(call("GET", "/_foundation/storage/constraint", null, null, "application/json"), 500, "INTERNAL_ERROR");
        problem(call("GET", "/_foundation/storage/pool", null, null, "application/json"), 503, "STORAGE_BUSY");
    }
    @Test void methodMediaAndParameterFailuresUseProblemJson() throws Exception {
        problem(call("GET", "/_foundation/parameter?limit=0", null, null, "application/json"), 400, "INVALID_PARAMETER");
        problem(call("GET", "/_foundation/parameter?limit=bad", null, null, "application/json"), 400, "INVALID_PARAMETER");
        var method = call("GET", "/_foundation/body", null, null, "application/json");
        problem(method, 405, "METHOD_NOT_ALLOWED"); assertThat(method.headers().firstValue("Allow")).isPresent();
        problem(call("POST", "/_foundation/body", "text", "text/plain", "application/json"), 415, "UNSUPPORTED_MEDIA_TYPE");
        problem(call("POST", "/_foundation/body", "{\"title\":\"valid\"}", "application/json", "text/html"), 406, "NOT_ACCEPTABLE");
    }
    @Test void observedProviderDeadlineIsBusyWhileOtherProviderErrorsStayInternal() throws Exception {
        problem(call("GET", "/_foundation/storage/provider-timeout", null, null, "application/json"), 503, "STORAGE_BUSY");
        problem(call("GET", "/_foundation/storage/provider-other", null, null, "application/json"), 500, "INTERNAL_ERROR");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Integer.class)).isZero();
    }

    @Test void authenticationIsHeaderOnlyUniformAndStateless() throws Exception {
        String path = "/_foundation/time";
        String expected = null;
        for (String header : new String[]{null, "Basic ignored", "Bearer invalid", "Bearer " + credential + "="}) {
            var builder = request(path, null).header("Accept", "text/html");
            if (header != null) builder.header("Authorization", header);
            var response = client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
            problem(response, 401, "AUTHENTICATION_FAILED");
            assertThat(response.headers().firstValue("WWW-Authenticate").orElse("")).isEqualTo("Bearer");
            if (expected == null) expected = response.body(); else assertThat(response.body()).isEqualTo(expected);
            assertThat(response.body().contains(credential)).isFalse();
        }
        var cookie = request(path + "?access_token=" + credential, null).header("Cookie", "Authorization=" + credential).header("X-Roles", "Reviewer");
        problem(client.send(cookie.GET().build(), HttpResponse.BodyHandlers.ofString()), 401, "AUTHENTICATION_FAILED");
        var success = call("GET", path, null, null, "application/json");
        assertThat(success.statusCode()).isEqualTo(200);
        assertThat(success.headers().allValues("Set-Cookie")).isEmpty();
        var id = json.readTree(success.body()).asText();
        assertThat(id).isEqualTo("2026-11-16T10:00:00.123456789Z");
        var issued = user(UserRole.Reviewer); administration.revokeCredential(issued.credentialId());
        var revoked = super.call("GET", path, issued.value(), null, null, "application/json");
        problem(revoked, 401, "AUTHENTICATION_FAILED"); assertThat(revoked.body()).isEqualTo(expected);
        problem(call("GET", "/_foundation/denied", null, null, "text/html"), 403, "PERMISSION_DENIED");
    }

    @Test void normalizedCodePointsAndSafeValidationMessages() throws Exception {
        String title = "\uD83D\uDE80".repeat(200);
        var accepted = call("POST", "/_foundation/body", json.writeValueAsString(java.util.Map.of("title", " \t" + title + "\n")), "application/json", "application/json");
        assertThat(accepted.statusCode()).isEqualTo(200);
        assertThat(json.readTree(accepted.body()).path("title").asText().equals(title)).isTrue();
        var rejected = call("POST", "/_foundation/body", json.writeValueAsString(java.util.Map.of("title", title + "x")), "application/json", "application/json");
        problem(rejected, 400, "INVALID_CONTENT"); assertThat(rejected.body().contains(title)).isFalse();
        assertThat(json.readTree(rejected.body()).path("fieldErrors").get(0).path("field").asText()).isEqualTo("title");
        for (String invalid : new String[]{"{}", "{\"title\":null}", "{\"title\":25}", "[]", "null"}) {
            var response = call("POST", "/_foundation/body", invalid, "application/json", "application/json");
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        }
    }

    @Test void noPublicAdministrativeRoutesAndFilterRegisteredOnce() throws Exception {
        int users = jdbc.queryForObject("SELECT COUNT(*) FROM user_access", Integer.class);
        for (String path : new String[]{"/api/admin", "/api/signup", "/api/token"}) {
            problem(call("POST", path, "{}", "application/json", "application/json"), 404, "NOT_FOUND");
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_access", Integer.class)).isEqualTo(users);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review_decision", Integer.class)).isZero();
        assertThat(securityChains.getFilters("/_foundation/time").stream()
                .filter(filter -> filter instanceof com.changeflow.infrastructure.security.BearerAuthenticationFilter).count()).isEqualTo(1);
        assertThat(servletContext.getFilterRegistrations().values().stream()
                .anyMatch(registration -> registration.getClassName().equals(com.changeflow.infrastructure.security.BearerAuthenticationFilter.class.getName()))).isFalse();
    }

    @Test void filterLookupStorageFailuresUseMatching503AndLeaveStorageUnchanged() throws Exception {
        var before = jdbc.queryForList("SELECT credential_id, user_id, created_at, revoked_at FROM credential ORDER BY credential_id");
        try {
            faultingCredentials.failure = "busy";
            problem(call("GET", "/_foundation/time", null, null, "text/html"), 503, "STORAGE_BUSY");
            faultingCredentials.failure = "unavailable";
            problem(call("GET", "/_foundation/time", null, null, "text/html"), 503, "STORAGE_UNAVAILABLE");
        } finally { faultingCredentials.failure = null; }
        assertThat(jdbc.queryForList("SELECT credential_id, user_id, created_at, revoked_at FROM credential ORDER BY credential_id")).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Integer.class)).isZero();
    }

    @Test void helpersSeedEveryStateAndReadFreshSnapshotsAfterRejectedHttp() throws Exception {
        var owner = user(UserRole.Requester);
        var ids = new java.util.ArrayList<UUID>();
        try {
            for (var state : com.changeflow.domain.RequestState.values()) {
                var seeded = seed(owner.userId(), state); ids.add(seeded.request().id());
                var before = stored(seeded.request().id());
                assertThat(before.parent()).isEqualTo(seeded); assertThat(before.decisions()).isEmpty();
                problem(call("GET", "/_foundation/parameter?limit=0", null, null, "application/json"), 400, "INVALID_PARAMETER");
                assertThat(stored(seeded.request().id())).isEqualTo(before);
            }
        } finally { for (var id : ids) jdbc.update("DELETE FROM change_request WHERE id=?", id); }
    }
}
