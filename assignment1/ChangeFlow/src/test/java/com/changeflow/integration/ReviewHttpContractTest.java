package com.changeflow.integration;

import com.changeflow.application.RequestSnapshot;
import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.api.Test;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ReviewHttpContractTest extends HttpTestSupport {
    private String path(UUID id) { return "/api/change-requests/" + id; }
    private String tag(RequestSnapshot value) { return "\"cr-" + value.request().id() + "-v" + value.revision() + "\""; }
    private String body(String action) { return action.equals("approve") ? "{}" : "{\"explanation\":\"  Required  changes\\nplease  \"}"; }
    private HttpResponse<String> act(UUID id, String action, String token, String body, String media, String accept, String... tags) throws Exception {
        var builder = request(path(id) + "/" + action, token).header("Accept", accept);
        if (media != null) builder.header("Content-Type", media);
        for (String tag : tags) builder.header("If-Match", tag);
        return client.send(builder.POST(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private void problem(HttpResponse<String> response, int status, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo(code);
    }
    @Test void bothDecisionsReturnTheCommittedParentRepresentationAndMetadata() throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        for (String action : List.of("approve", "request-changes")) {
            var seed = seed(owner.userId(), RequestState.PendingReview, "Earlier feedback");
            var observed = call("GET", path(seed.request().id()), reviewer.value(), null, null, "application/json");
            assertThat(observed.statusCode()).isEqualTo(200);
            var response = act(seed.request().id(), action, reviewer.value(), body(action), "application/json", "application/json", observed.headers().firstValue("ETag").orElseThrow());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
            assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
            assertThat(response.headers().firstValue("Location")).isEmpty();
            var after = stored(seed.request().id()); var item = json.readTree(response.body());
            var names = new ArrayList<String>(); item.fieldNames().forEachRemaining(names::add);
            assertThat(names).containsExactly("id", "title", "description", "reason", "requesterId", "state", "latestExplanation", "createdAt", "updatedAt", "revision");
            assertThat(after.parent().revision()).isEqualTo(seed.revision() + 1);
            assertThat(response.headers().firstValue("ETag")).hasValue(tag(after.parent()));
            assertThat(item.path("revision").asLong()).isEqualTo(after.parent().revision());
            assertThat(item.path("state").asText()).isEqualTo(action.equals("approve") ? "Approved" : "ChangesRequested");
            assertThat(item.path("latestExplanation").asText()).isEqualTo(action.equals("approve") ? "Earlier feedback" : "Required  changes\nplease");
            assertThat(Instant.parse(item.path("updatedAt").asText())).isEqualTo(after.parent().request().updatedAt());
            assertThat(after.decisions()).hasSize(1);
            var decision = after.decisions().get(0);
            assertThat(decision.reviewerId()).isEqualTo(reviewer.userId());
            assertThat(decision.action()).isEqualTo(action.equals("approve") ? "Approve" : "RequestChanges");
            assertThat(decision.revision()).isEqualTo(after.parent().revision());
            assertThat(decision.decidedAt()).isEqualTo(after.parent().request().updatedAt());
            assertThat(call("GET", path(seed.request().id()), owner.value(), null, null, "application/json").body()).isEqualTo(response.body());
        }
    }
    @Test void parentIfMatchGrammarIsRequiredForBothActions() throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        for (String action : List.of("approve", "request-changes")) {
            var seed = seed(owner.userId(), RequestState.PendingReview); var before = stored(seed.request().id());
            problem(act(seed.request().id(), action, reviewer.value(), body(action), "application/json", "application/json"), 428, "PRECONDITION_REQUIRED");
            for (String invalid : List.of("unquoted", "\"cr-not-a-uuid-v0\"", "\"cr-" + seed.request().id() + "-v-1\"", "\"cr-" + seed.request().id() + "-v9223372036854775808\"")) {
                problem(act(seed.request().id(), action, reviewer.value(), body(action), "application/json", "application/json", invalid), 400, "INVALID_IF_MATCH");
            }
            for (String invalid : List.of("*", tag(seed) + "," + tag(seed))) problem(act(seed.request().id(), action, reviewer.value(), body(action), "application/json", "application/json", invalid), 400, "UNSUPPORTED_IF_MATCH");
            problem(act(seed.request().id(), action, reviewer.value(), body(action), "application/json", "application/json", tag(seed), tag(seed)), 400, "UNSUPPORTED_IF_MATCH");
            for (String stale : List.of("W/" + tag(seed), "\"cr-" + UUID.randomUUID() + "-v0\"", "\"cr-" + seed.request().id() + "-v99\"")) problem(act(seed.request().id(), action, reviewer.value(), body(action), "application/json", "application/json", stale), 412, "STALE_VERSION");
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
    }
    @Test void accessPrecedesVersionAndVersionPrecedesState() throws Exception {
        var owner = user(UserRole.Requester, UserRole.Reviewer); var reviewer = user(UserRole.Reviewer);
        for (String action : List.of("approve", "request-changes")) {
            var seed = seed(owner.userId(), RequestState.PendingReview); var before = stored(seed.request().id());
            problem(act(seed.request().id(), action, owner.value(), body(action), "application/json", "application/json", "W/" + tag(seed)), 403, "PERMISSION_DENIED");
            assertThat(stored(seed.request().id())).isEqualTo(before);
            assertThat(act(seed.request().id(), action, reviewer.value(), body(action), "application/json", "application/json", tag(seed)).statusCode()).isEqualTo(200);
            var decided = stored(seed.request().id());
            for (String retry : List.of("approve", "request-changes")) {
                problem(act(seed.request().id(), retry, reviewer.value(), body(retry), "application/json", "application/json", tag(seed)), 412, "STALE_VERSION");
                problem(act(seed.request().id(), retry, reviewer.value(), body(retry), "application/json", "application/json", tag(decided.parent())), 409, "INVALID_STATE");
                assertThat(stored(seed.request().id())).isEqualTo(decided);
            }
        }
    }
    @Test void approveAcceptsOnlyAbsentBytesOrAnEmptyJsonObject() throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        for (String bad : List.of("null", "[]", "1", "\"text\"", "{", "{}{}", "{\"explanation\":\"x\"}", "{\"state\":\"PendingReview\"}", "{\"requesterId\":\"" + owner.userId() + "\"}", "{\"roles\":[\"Reviewer\"]}")) {
            var seed = seed(owner.userId(), RequestState.PendingReview); var before = stored(seed.request().id());
            var response = act(seed.request().id(), "approve", reviewer.value(), bad, "application/json", "application/json", tag(seed));
            assertThat(response.statusCode()).isEqualTo(400);
            if (bad.contains("state") || bad.contains("requesterId") || bad.contains("roles")) problem(response, 400, "FORBIDDEN_FIELD");
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
        for (String media : Arrays.asList(null, "text/plain", "application/json")) {
            var seed = seed(owner.userId(), RequestState.PendingReview);
            assertThat(act(seed.request().id(), "approve", reviewer.value(), null, media, "*/*", tag(seed)).statusCode()).isEqualTo(200);
            assertThat(stored(seed.request().id()).decisions()).hasSize(1);
        }
    }
    @Test void mediaAndAbsentFeedbackFailuresNeverPersistAReview() throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        for (String action : List.of("approve", "request-changes")) {
            var seed = seed(owner.userId(), RequestState.PendingReview, "Existing feedback"); var before = stored(seed.request().id());
            for (String media : Arrays.asList(null, "text/plain", "application/problem+json")) problem(act(seed.request().id(), action, reviewer.value(), body(action), media, "application/json", tag(seed)), 415, "UNSUPPORTED_MEDIA_TYPE");
            problem(act(seed.request().id(), action, reviewer.value(), body(action), "application/json", "text/html", tag(seed)), 406, "NOT_ACCEPTABLE");
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
        var seed = seed(owner.userId(), RequestState.PendingReview); var before = stored(seed.request().id());
        problem(act(seed.request().id(), "request-changes", reviewer.value(), "{}", "application/json", "application/json", tag(seed)), 400, "INVALID_CONTENT");
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
}
