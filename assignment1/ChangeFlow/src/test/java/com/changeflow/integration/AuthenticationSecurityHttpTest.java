package com.changeflow.integration;

import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AuthenticationSecurityHttpTest extends HttpTestSupport {
    @Autowired org.springframework.security.web.FilterChainProxy chains;
    @ParameterizedTest @ValueSource(strings = {"missing", "malformed", "invalid", "revoked", "duplicate"})
    void uniformBearerFailuresPrecedeEveryProtectedLookupAndWrite(String kind) throws Exception {
        var owner = user(UserRole.Requester); var credential = user(UserRole.Reviewer);
        if (kind.equals("revoked")) administration.revokeCredential(credential.credentialId());
        var seed = seed(owner.userId(), RequestState.PendingReview, "Private feedback"); var before = stored(seed.request().id());
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class);
        com.fasterxml.jackson.databind.JsonNode expected = null;
        for (String suffix : List.of("", "/" + seed.request().id(), "/" + UUID.randomUUID(), "/" + seed.request().id() + "/submit", "/" + seed.request().id() + "/approve", "/" + seed.request().id() + "/request-changes")) {
            for (String method : List.of("GET", "POST", "PATCH")) {
                var builder = request("/api/change-requests" + suffix, null).header("Accept", "text/html").header("Content-Type", "application/json").header("X-Roles", "Reviewer,Requester");
                if (kind.equals("malformed")) builder.header("Authorization", "Basic unsupported");
                if (kind.equals("invalid")) builder.header("Authorization", "Bearer not-an-issued-credential");
                if (kind.equals("revoked") || kind.equals("duplicate")) builder.header("Authorization", "Bearer " + credential.value());
                if (kind.equals("duplicate")) builder.header("Authorization", "Bearer " + credential.value());
                var response = client.send(builder.method(method, HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(401); assertThat(response.headers().firstValue("WWW-Authenticate")).hasValue("Bearer");
                assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
                var problem = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(response.body()); problem.remove("instance");
                if (expected == null) expected = problem; else assertThat(problem).isEqualTo(expected);
                assertThat(response.body().contains(credential.value())).isFalse();
                assertThat(response.body()).doesNotContain("Private feedback", "\"roles\"", "secretHash", "credentialId", "SQL", "trace");
                assertThat(stored(seed.request().id())).isEqualTo(before);
            }
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(count);
    }
    @Test void cookieQueryAndForgedRoleHeadersNeverSupplyAuthenticationOrPermission() throws Exception {
        var owner = user(UserRole.Requester); var none = user(); var seed = seed(owner.userId(), RequestState.Draft); var before = stored(seed.request().id());
        var cookie = request("/api/change-requests/" + seed.request().id(), null).header("Cookie", "access_token=" + owner.value() + "; JSESSIONID=untrusted").GET().build();
        assertThat(client.send(cookie, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        try {
            var query = request("/api/change-requests?access_token=" + owner.value(), null).GET().build();
            assertThat(client.send(query, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        } catch (java.io.IOException failure) { throw new AssertionError("Query-credential transport failed; credential value withheld."); }
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class);
        var forged = request("/api/change-requests", none.value()).header("X-Roles", "Requester").header("X-Requester-Id", owner.userId().toString()).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"Valid\",\"description\":\"Details\",\"reason\":\"Reason\"}")).build();
        assertThat(client.send(forged, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(count); assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @Test void explicitBearerWritesNeedNoCsrfTokenAndNeverEstablishASession() throws Exception {
        var owner = user(UserRole.Requester);
        var created = call("POST", "/api/change-requests", owner.value(), "{\"title\":\"Bearer proposal\",\"description\":\"Details\",\"reason\":\"Reason\"}", "application/json", "application/json");
        assertThat(created.statusCode()).isEqualTo(201); assertThat(created.headers().firstValue("Set-Cookie")).isEmpty();
        UUID id = UUID.fromString(json.readTree(created.body()).path("id").asText()); var before = stored(id);
        assertThat(client.send(request("/api/change-requests/" + id, null).header("Cookie", "JSESSIONID=untrusted").GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        var valid = client.send(request("/api/change-requests/" + id, owner.value()).header("Cookie", "JSESSIONID=untrusted").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(valid.statusCode()).isEqualTo(200); assertThat(valid.headers().firstValue("Set-Cookie")).isEmpty();
        assertThat(stored(id)).isEqualTo(before);
        var names = chains.getFilters("/api/change-requests").stream().map(filter -> filter.getClass().getSimpleName()).toList();
        assertThat(names.stream().filter(name -> name.equals("BearerAuthenticationFilter")).count()).isEqualTo(1);
        assertThat(names).doesNotContain("BasicAuthenticationFilter", "UsernamePasswordAuthenticationFilter", "CsrfFilter");
    }
    @Test void noPublicProvisioningIssuerHistoryOrAdministrationSurfaceExists() throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.Draft); var before = stored(seed.request().id());
        for (String path : List.of("/api/admin", "/api/signup", "/api/token", "/api/introspect", "/api/credentials", "/api/change-requests/" + seed.request().id() + "/history")) {
            assertThat(call("POST", path, owner.value(), "{}", "application/json", "application/json").statusCode()).isEqualTo(404);
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
    }
}
