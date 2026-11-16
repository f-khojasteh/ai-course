package com.changeflow.integration;

import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.api.Test;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class OwnerHttpContractTest extends HttpTestSupport {
    private static final String ROOT = "/api/change-requests";
    private static final String BODY = "{\"title\":\"Proposal\",\"description\":\"Details\",\"reason\":\"Benefit\"}";
    private String tag(com.changeflow.application.RequestSnapshot value) {
        return "\"cr-" + value.request().id() + "-v" + value.revision() + "\"";
    }
    private HttpResponse<String> mutate(String method, String path, String token, String body, String... tags) throws Exception {
        var builder = request(path, token).header("Accept", "application/json");
        if (body != null) builder.header("Content-Type", "application/json");
        for (String tag : tags) builder.header("If-Match", tag);
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private void problem(HttpResponse<String> response, int status, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo(code);
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
    }
    @Test void fiveRoutesUseDeterministicDtosAndParentEtags() throws Exception {
        var owner = user(UserRole.Requester);
        var created = mutate("POST", ROOT, owner.value(), BODY);
        assertThat(created.statusCode()).isEqualTo(201);
        var item = json.readTree(created.body());
        var names = new ArrayList<String>(); item.fieldNames().forEachRemaining(names::add);
        assertThat(names).containsExactly("id", "title", "description", "reason", "requesterId", "state", "latestExplanation", "createdAt", "updatedAt", "revision");
        UUID id = UUID.fromString(item.path("id").asText());
        String path = ROOT + "/" + id;
        var saved = stored(id);
        assertThat(created.headers().firstValue("Location")).hasValue(path);
        assertThat(created.headers().firstValue("ETag")).hasValue(tag(saved.parent()));
        assertThat(Instant.parse(item.path("createdAt").asText())).isEqualTo(saved.parent().request().createdAt());
        assertThat(Instant.parse(item.path("updatedAt").asText())).isEqualTo(saved.parent().request().updatedAt());
        var read = call("GET", path, owner.value(), null, null, "application/json");
        assertThat(read.statusCode()).isEqualTo(200);
        assertThat(read.body()).isEqualTo(created.body());
        var list = call("GET", ROOT, owner.value(), null, null, "application/json");
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(json.readTree(list.body()).path("items").get(0)).isEqualTo(item);
        assertThat(list.headers().firstValue("ETag")).isEmpty();
        var edit = mutate("PATCH", path, owner.value(), "{\"title\":\"Revised\"}", tag(saved.parent()));
        assertThat(edit.statusCode()).isEqualTo(200);
        var submitted = mutate("POST", path + "/submit", owner.value(), null, edit.headers().firstValue("ETag").orElseThrow());
        assertThat(submitted.statusCode()).isEqualTo(200);
        assertThat(json.readTree(submitted.body()).path("state").asText()).isEqualTo("PendingReview");
        assertThat(submitted.headers().firstValue("ETag")).hasValue(tag(stored(id).parent()));
        assertThat(call("GET", path, owner.value(), null, null, "application/json").body()).isEqualTo(submitted.body());
        assertThat(stored(id).decisions()).isEmpty();
    }
    @Test void ifMatchGrammarAndStatePrecedenceOnBothMutations() throws Exception {
        var owner = user(UserRole.Requester);
        for (String operation : List.of("PATCH", "submit")) {
            var seed = seed(owner.userId(), RequestState.Draft);
            String path = ROOT + "/" + seed.request().id() + (operation.equals("submit") ? "/submit" : "");
            String method = operation.equals("submit") ? "POST" : "PATCH";
            var before = stored(seed.request().id());
            problem(mutate(method, path, owner.value(), "{}"), 428, "PRECONDITION_REQUIRED");
            for (String malformed : List.of("unquoted", "\"cr-bad-v0\"", "\"cr-" + seed.request().id() + "-v-1\"", "\"cr-" + seed.request().id() + "-v9223372036854775808\"")) {
                problem(mutate(method, path, owner.value(), "{}", malformed), 400, "INVALID_IF_MATCH");
            }
            for (String unsupported : List.of("*", tag(seed) + ", " + tag(seed))) {
                problem(mutate(method, path, owner.value(), "{}", unsupported), 400, "UNSUPPORTED_IF_MATCH");
            }
            problem(mutate(method, path, owner.value(), "{}", tag(seed), tag(seed)), 400, "UNSUPPORTED_IF_MATCH");
            var staleTags = new ArrayList<>(List.of("W/" + tag(seed), "W/\"other\"", "W/\"other,tag\"", "\"cr-" + UUID.randomUUID() + "-v0\"", "\"cr-" + seed.request().id() + "-v99\"", "\"cr-" + seed.request().id() + "-v00\""));
            String uppercaseId = seed.request().id().toString().toUpperCase(Locale.ROOT);
            if (!uppercaseId.equals(seed.request().id().toString())) staleTags.add("\"cr-" + uppercaseId + "-v0\"");
            for (String stale : staleTags) {
                problem(mutate(method, path, owner.value(), "{}", stale), 412, "STALE_VERSION");
            }
            assertThat(stored(seed.request().id())).isEqualTo(before);
            assertThat(mutate(method, path, owner.value(), "{}", "  " + tag(seed) + "  ").statusCode()).isEqualTo(200);
        }
        var pending = seed(owner.userId(), RequestState.PendingReview);
        String path = ROOT + "/" + pending.request().id();
        var before = stored(pending.request().id());
        for (String suffix : List.of("", "/submit")) {
            String method = suffix.isEmpty() ? "PATCH" : "POST";
            problem(mutate(method, path + suffix, owner.value(), "{}", "\"cr-" + pending.request().id() + "-v99\""), 412, "STALE_VERSION");
            problem(mutate(method, path + suffix, owner.value(), "{}", tag(pending)), 409, "INVALID_STATE");
        }
        assertThat(stored(pending.request().id())).isEqualTo(before);
    }
    @Test void permissionsAndVisibilityPrecedeFailedStrongComparison() throws Exception {
        var owner = user(UserRole.Requester);
        var outsider = user(UserRole.Reviewer);
        for (RequestState state : List.of(RequestState.Draft, RequestState.PendingReview)) {
            var seed = seed(owner.userId(), state);
            var before = stored(seed.request().id());
            for (String suffix : List.of("", "/submit")) {
                problem(mutate(suffix.isEmpty() ? "PATCH" : "POST", ROOT + "/" + seed.request().id() + suffix, outsider.value(), "{}", "W/" + tag(seed)), state == RequestState.Draft ? 404 : 403, state == RequestState.Draft ? "NOT_FOUND" : "PERMISSION_DENIED");
            }
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
    }
    @Test void mediaParametersAndUnsupportedRoutesHaveSafeErrors() throws Exception {
        var owner = user(UserRole.Requester);
        var seed = seed(owner.userId(), RequestState.Draft);
        String path = ROOT + "/" + seed.request().id();
        var before = stored(seed.request().id());
        problem(call("POST", ROOT, owner.value(), BODY, "text/plain", "application/json"), 415, "UNSUPPORTED_MEDIA_TYPE");
        problem(call("GET", path, owner.value(), null, null, "text/html"), 406, "NOT_ACCEPTABLE");
        problem(call("GET", ROOT + "/not-a-uuid", owner.value(), null, null, "application/json"), 400, "INVALID_PARAMETER");
        for (String query : List.of("offset=-1", "offset=oops", "offset=2147483648", "limit=0", "limit=101", "limit=oops", "limit=", "limit=1&limit=2", "typo=1")) {
            problem(call("GET", ROOT + "?" + query, owner.value(), null, null, "application/json"), 400, "INVALID_PARAMETER");
        }
        var deleted = call("DELETE", path, owner.value(), null, null, "application/json");
        problem(deleted, 405, "METHOD_NOT_ALLOWED");
        assertThat(deleted.headers().firstValue("Allow").orElse("")).contains("GET", "PATCH");
        for (String action : List.of("cancel", "withdraw", "execute", "publish", "history")) {
            problem(call("POST", path + "/" + action, owner.value(), "{}", "application/json", "application/json"), 404, "NOT_FOUND");
        }
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @Test void submitAcceptsOnlyAbsentOrEmptyObjectBodies() throws Exception {
        var owner = user(UserRole.Requester);
        for (String body : List.of("null", "[]", "\"text\"", "1", "{", "{}{}", "{} trailing", "{\"title\":\"x\"}", "{\"state\":\"Draft\"}", "{\"roles\":[]}", "{\"requesterId\":\"" + owner.userId() + "\"}")) {
            var seed = seed(owner.userId(), RequestState.Draft);
            var before = stored(seed.request().id());
            var response = mutate("POST", ROOT + "/" + seed.request().id() + "/submit", owner.value(), body, tag(seed));
            assertThat(response.statusCode()).isEqualTo(400);
            if (body.contains("state") || body.contains("roles") || body.contains("requesterId")) problem(response, 400, "FORBIDDEN_FIELD");
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
        var seed = seed(owner.userId(), RequestState.Draft);
        var builder = request(ROOT + "/" + seed.request().id() + "/submit", owner.value()).header("If-Match", tag(seed)).header("Content-Type", "text/plain");
        var before = stored(seed.request().id());
        problem(client.send(builder.POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString()), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @Test void submitRejectsNonJsonStructuredMedia() throws Exception {
        var owner = user(UserRole.Requester);
        var seed = seed(owner.userId(), RequestState.Draft);
        var before = stored(seed.request().id());
        var builder = request(ROOT + "/" + seed.request().id() + "/submit", owner.value())
                .header("If-Match", tag(seed)).header("Content-Type", "application/problem+json");
        problem(client.send(builder.POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString()), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @Test void emptySubmitNeedsNoMediaAndJsonPermitsCharsetParameters() throws Exception {
        var owner = user(UserRole.Requester);
        for (String media : Arrays.asList(null, "text/plain", "application/json")) {
            var seed = seed(owner.userId(), RequestState.Draft);
            var builder = request(ROOT + "/" + seed.request().id() + "/submit", owner.value()).header("If-Match", tag(seed));
            if (media != null) builder.header("Content-Type", media);
            assertThat(client.send(builder.POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
            assertThat(stored(seed.request().id()).parent().request().state()).isEqualTo(RequestState.PendingReview);
        }
        var seed = seed(owner.userId(), RequestState.Draft);
        var builder = request(ROOT + "/" + seed.request().id() + "/submit", owner.value()).header("If-Match", tag(seed)).header("Content-Type", "application/json; charset=UTF-8");
        assertThat(client.send(builder.POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        assertThat(stored(seed.request().id()).parent().request().state()).isEqualTo(RequestState.PendingReview);
    }
}
