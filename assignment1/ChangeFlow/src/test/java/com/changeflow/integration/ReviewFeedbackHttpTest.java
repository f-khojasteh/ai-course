package com.changeflow.integration;

import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ReviewFeedbackHttpTest extends HttpTestSupport {
    private HttpResponse<String> changes(StoredSnapshot item, String token, String body) throws Exception {
        var id = item.parent().request().id();
        return client.send(request("/api/change-requests/" + id + "/request-changes", token)
                .header("Content-Type", "application/json").header("If-Match", "\"cr-" + id + "-v" + item.parent().revision() + "\"")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private void bad(HttpResponse<String> response, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo(code);
        assertThat(response.body()).doesNotContain("rejectedValue", "Private prior feedback");
    }
    @ParameterizedTest @ValueSource(strings = {"omitted", "null", "empty", "blank", "excessive", "number", "array"})
    void invalidExplanationNeverChangesParentOrDecisions(String kind) throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        var seed = seed(owner.userId(), RequestState.PendingReview, "Private prior feedback"); var before = stored(seed.request().id());
        Object text = switch (kind) { case "omitted", "null" -> null; case "empty" -> ""; case "blank" -> " \t\n\u2003"; case "number" -> 42; case "array" -> List.of("text"); default -> "x".repeat(2001); };
        var body = new LinkedHashMap<String, Object>(); if (!kind.equals("omitted")) body.put("explanation", text);
        var response = changes(before, reviewer.value(), json.writeValueAsString(body));
        if (kind.equals("number") || kind.equals("array")) assertThat(response.statusCode()).isEqualTo(400);
        else bad(response, "INVALID_CONTENT");
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @Test void exactNormalizedUnicodeBoundariesAndInternalWhitespaceArePreserved() throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        for (String feedback : List.of("Please  revise\nthis part", "x".repeat(2000), new String(Character.toChars(0x1F680)).repeat(2000))) {
            var seed = seed(owner.userId(), RequestState.PendingReview, "Private prior feedback"); var before = stored(seed.request().id());
            var response = changes(before, reviewer.value(), json.writeValueAsString(Map.of("explanation", "\u2003 \t" + feedback + " \n")));
            assertThat(response.statusCode()).isEqualTo(200);
            var after = stored(seed.request().id());
            assertThat(after.parent().request().latestExplanation()).isEqualTo(feedback);
            assertThat(json.readTree(response.body()).path("latestExplanation").asText()).isEqualTo(feedback);
            assertThat(after.parent().request().state()).isEqualTo(RequestState.ChangesRequested);
            assertThat(after.parent().revision()).isEqualTo(before.parent().revision() + 1);
            assertThat(after.decisions()).hasSize(1);
            var second = seed(owner.userId(), RequestState.PendingReview, "Private prior feedback"); var unchanged = stored(second.request().id());
            String over = (feedback.codePointCount(0, feedback.length()) == 2000 ? feedback : "x".repeat(2000)) + "x";
            bad(changes(unchanged, reviewer.value(), json.writeValueAsString(Map.of("explanation", over))), "INVALID_CONTENT");
            assertThat(stored(second.request().id())).isEqualTo(unchanged);
        }
    }
    @Test void forbiddenMatchingAndUnknownFieldsCannotStorePartialFeedback() throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        var seed = seed(owner.userId(), RequestState.PendingReview, "Private prior feedback"); var before = stored(seed.request().id());
        var fields = new LinkedHashMap<String, Object>();
        fields.put("requesterId", owner.userId()); fields.put("requesterIdentity", owner.userId()); fields.put("roles", List.of("Reviewer")); fields.put("state", "PendingReview");
        fields.put("id", seed.request().id()); fields.put("revision", seed.revision()); fields.put("createdAt", seed.request().createdAt()); fields.put("updatedAt", seed.request().updatedAt()); fields.put("latestExplanation", seed.request().latestExplanation()); fields.put("typo", 1);
        for (var field : fields.entrySet()) {
            var body = new LinkedHashMap<String, Object>(); body.put("explanation", "New valid feedback"); body.put(field.getKey(), field.getValue());
            bad(changes(before, reviewer.value(), json.writeValueAsString(body)), field.getKey().equals("typo") ? "UNKNOWN_FIELD" : "FORBIDDEN_FIELD");
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
        for (String invalid : List.of("null", "[]", "1", "\"text\"", "{", "{\"explanation\":\"Valid\"}{}")) {
            assertThat(changes(before, reviewer.value(), invalid).statusCode()).isEqualTo(400);
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
    }
    @Test void latestFeedbackFollowsParentVisibilityAndMetadataHasNoExplanationColumn() throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        var seed = seed(owner.userId(), RequestState.PendingReview, "Private prior feedback");
        assertThat(changes(stored(seed.request().id()), reviewer.value(), "{\"explanation\":\"Visible latest feedback\"}").statusCode()).isEqualTo(200);
        var before = stored(seed.request().id());
        for (var actor : List.of(owner, reviewer, user(UserRole.Requester, UserRole.Reviewer))) {
            var response = call("GET", "/api/change-requests/" + seed.request().id(), actor.value(), null, null, "application/json");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(json.readTree(response.body()).path("latestExplanation").asText()).isEqualTo("Visible latest feedback");
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
        for (var actor : List.of(user(UserRole.Requester), user())) {
            var hidden = call("GET", "/api/change-requests/" + seed.request().id(), actor.value(), null, null, "application/json");
            var missing = call("GET", "/api/change-requests/" + UUID.randomUUID(), actor.value(), null, null, "application/json");
            assertThat(hidden.statusCode()).isEqualTo(404); assertThat(missing.statusCode()).isEqualTo(404);
            assertThat(hidden.body()).doesNotContain("feedback", "requesterId", "latestExplanation");
            var a = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(hidden.body()); a.remove("instance");
            var b = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(missing.body()); b.remove("instance");
            assertThat(a).isEqualTo(b);
            var list = call("GET", "/api/change-requests", actor.value(), null, null, "application/json");
            assertThat(list.statusCode()).isEqualTo(200); assertThat(json.readTree(list.body()).path("items").isEmpty()).isTrue();
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
        var columns = jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='REVIEW_DECISION'", String.class);
        assertThat(columns).containsExactlyInAnyOrder("ID", "REQUEST_ID", "REVIEWER_ID", "ACTION", "DECIDED_AT", "RESULTING_REQUEST_VERSION");
        var rows = jdbc.queryForList("SELECT * FROM review_decision WHERE request_id=?", seed.request().id());
        assertThat(rows).hasSize(1);
        assertThat(rows.toString()).doesNotContain("Private prior feedback", "Visible latest feedback");
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
}
