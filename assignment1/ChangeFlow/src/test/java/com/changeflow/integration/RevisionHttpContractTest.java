package com.changeflow.integration;

import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class RevisionHttpContractTest extends HttpTestSupport {
    private static final Map<String, Integer> LIMITS = Map.of("title", 200, "description", 5000, "reason", 2000);
    private String path(UUID id) { return "/api/change-requests/" + id; }
    private String tag(StoredSnapshot value) { return "\"cr-" + value.parent().request().id() + "-v" + value.parent().revision() + "\""; }
    private HttpResponse<String> write(StoredSnapshot item, String token, boolean submit, String body, String... tags) throws Exception {
        var builder = request(path(item.parent().request().id()) + (submit ? "/submit" : ""), token).header("Accept", "application/json");
        if (body != null) builder.header("Content-Type", "application/json");
        for (String tag : tags) builder.header("If-Match", tag);
        return client.send(builder.method(submit ? "POST" : "PATCH", body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private void problem(HttpResponse<String> response, int status, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo(code);
    }
    @Test void sameRoutesDtoEtagsAndNoOpRulesApplyToReturnedRequests() throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.ChangesRequested, "Keep this feedback");
        var before = stored(seed.request().id());
        var read = call("GET", path(seed.request().id()), owner.value(), null, null, "application/json");
        assertThat(read.statusCode()).isEqualTo(200);
        for (String body : List.of("{}", json.writeValueAsString(Map.of("title", " \tSeed proposal ")))) {
            var response = write(before, owner.value(), false, body, tag(before));
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo(read.body()); assertThat(response.headers().firstValue("ETag")).hasValue(tag(before));
            assertThat(json.readTree(response.body()).path("updatedAt").asText()).isEqualTo("2026-11-16T10:00:00.123456789Z");
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
        var edited = write(before, owner.value(), false, "{\"title\":\"  New  title\\nline  \"}", tag(before));
        assertThat(edited.statusCode()).isEqualTo(200);
        var after = stored(seed.request().id()); var old = seed.request();
        assertThat(after.parent().request()).isEqualTo(new ChangeRequest(old.id(), "New  title\nline", old.description(), old.reason(), old.requesterId(), old.state(), old.latestExplanation(), old.createdAt(), after.parent().request().updatedAt()));
        assertThat(after.parent().revision()).isEqualTo(before.parent().revision() + 1); assertThat(after.decisions()).isEqualTo(before.decisions());
        var submitted = write(after, owner.value(), true, null, tag(after));
        assertThat(submitted.statusCode()).isEqualTo(200);
        var committed = stored(seed.request().id()); var item = json.readTree(submitted.body());
        var names = new ArrayList<String>(); item.fieldNames().forEachRemaining(names::add);
        assertThat(names).containsExactly("id", "title", "description", "reason", "requesterId", "state", "latestExplanation", "createdAt", "updatedAt", "revision");
        assertThat(committed.parent().request()).isEqualTo(new ChangeRequest(old.id(), after.parent().request().title(), old.description(), old.reason(), old.requesterId(), RequestState.PendingReview, old.latestExplanation(), old.createdAt(), committed.parent().request().updatedAt()));
        assertThat(committed.parent().revision()).isEqualTo(after.parent().revision() + 1); assertThat(committed.decisions()).isEqualTo(before.decisions());
        assertThat(submitted.headers().firstValue("ETag")).hasValue(tag(committed)); assertThat(submitted.headers().firstValue("Cache-Control")).hasValue("no-store");
        assertThat(Instant.parse(item.path("updatedAt").asText())).isEqualTo(committed.parent().request().updatedAt());
        assertThat(call("GET", path(old.id()), owner.value(), null, null, "application/json").body()).isEqualTo(submitted.body());
    }
    @Test void returnedMutationsRequireTheCurrentItemTagAndRejectInvalidFormsWithoutEffects() throws Exception {
        var owner = user(UserRole.Requester);
        for (boolean submit : List.of(false, true)) {
            var seed = seed(owner.userId(), RequestState.ChangesRequested); var before = stored(seed.request().id());
            problem(write(before, owner.value(), submit, "{}"), 428, "PRECONDITION_REQUIRED");
            problem(write(before, owner.value(), submit, "{}", "bad"), 400, "INVALID_IF_MATCH");
            for (String unsupported : List.of("*", tag(before) + "," + tag(before))) problem(write(before, owner.value(), submit, "{}", unsupported), 400, "UNSUPPORTED_IF_MATCH");
            for (String stale : List.of("W/" + tag(before), "\"cr-" + UUID.randomUUID() + "-v0\"", "\"cr-" + seed.request().id() + "-v99\"")) problem(write(before, owner.value(), submit, "{}", stale), 412, "STALE_VERSION");
            assertThat(stored(seed.request().id())).isEqualTo(before);
            assertThat(write(before, owner.value(), submit, "{}", tag(before)).statusCode()).isEqualTo(200);
        }
    }
    static Stream<Arguments> invalidFields() {
        return LIMITS.entrySet().stream().flatMap(field -> Stream.of("null", "empty", "blank", "excessive").map(kind -> Arguments.of(field.getKey(), field.getValue(), kind)));
    }
    @ParameterizedTest(name = "{0}: {2}") @MethodSource("invalidFields")
    void invalidReturnedPartialEditsAreAllOrNothing(String field, int maximum, String kind) throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.ChangesRequested); var before = stored(seed.request().id());
        Object bad = switch (kind) { case "null" -> null; case "empty" -> ""; case "blank" -> " \t\n\u2003 "; default -> "x".repeat(maximum + 1); };
        var body = new LinkedHashMap<String, Object>(); body.put(field.equals("title") ? "description" : "title", "Valid change"); body.put(field, bad);
        problem(write(before, owner.value(), false, json.writeValueAsString(body), tag(before)), 400, "INVALID_CONTENT");
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @Test void normalizedExactAndExcessiveReturnedContentCoversEveryColumnAndUnicode() throws Exception {
        var owner = user(UserRole.Requester);
        for (var field : LIMITS.entrySet()) for (String point : List.of("x", new String(Character.toChars(0x1F680)))) {
            var seed = seed(owner.userId(), RequestState.ChangesRequested); var before = stored(seed.request().id());
            String exact = point.repeat(field.getValue());
            var response = write(before, owner.value(), false, json.writeValueAsString(Map.of(field.getKey(), "\u2003 " + exact + " \n")), tag(before));
            assertThat(response.statusCode()).isEqualTo(200);
            var after = stored(seed.request().id());
            assertThat(json.valueToTree(after.parent().request()).path(field.getKey()).asText()).isEqualTo(exact);
            assertThat(after.parent().request().latestExplanation()).isEqualTo(before.parent().request().latestExplanation());
            assertThat(after.parent().request().requesterId()).isEqualTo(owner.userId()); assertThat(after.parent().request().state()).isEqualTo(RequestState.ChangesRequested);
            assertThat(after.parent().revision()).isEqualTo(before.parent().revision() + 1); assertThat(after.decisions()).isEqualTo(before.decisions());
            problem(write(after, owner.value(), false, json.writeValueAsString(Map.of(field.getKey(), exact + point)), tag(after)), 400, "INVALID_CONTENT");
            assertThat(stored(seed.request().id())).isEqualTo(after);
        }
    }
    @Test void controlledMatchingUnknownAndMalformedBodiesCannotChangeReturnedData() throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.ChangesRequested); var before = stored(seed.request().id());
        var fields = new LinkedHashMap<String, Object>();
        fields.put("requesterId", owner.userId()); fields.put("roles", List.of("Requester")); fields.put("state", "ChangesRequested"); fields.put("id", seed.request().id()); fields.put("revision", seed.revision());
        fields.put("createdAt", seed.request().createdAt()); fields.put("updatedAt", seed.request().updatedAt()); fields.put("latestExplanation", seed.request().latestExplanation()); fields.put("typo", 1);
        for (var field : fields.entrySet()) {
            var body = new LinkedHashMap<String, Object>(); body.put("title", "Valid change"); body.put(field.getKey(), field.getValue());
            problem(write(before, owner.value(), false, json.writeValueAsString(body), tag(before)), 400, field.getKey().equals("typo") ? "UNKNOWN_FIELD" : "FORBIDDEN_FIELD");
            assertThat(stored(seed.request().id())).isEqualTo(before);
            if (!field.getKey().equals("typo")) {
                problem(write(before, owner.value(), true, json.writeValueAsString(Map.of(field.getKey(), field.getValue())), tag(before)), 400, "FORBIDDEN_FIELD");
                assertThat(stored(seed.request().id())).isEqualTo(before);
            }
        }
        for (String body : List.of("null", "[]", "1", "{", "{}{}", "{\"title\":12}")) {
            assertThat(write(before, owner.value(), false, body, tag(before)).statusCode()).isEqualTo(400);
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
    }
    @Test void invalidStoredReturnedContentRejectsSubmissionWithoutRepair() throws Exception {
        var owner = user(UserRole.Requester);
        for (String field : LIMITS.keySet()) {
            var seed = seed(owner.userId(), RequestState.ChangesRequested); UUID id = seed.request().id(); var observed = stored(id);
            // Deliberate invalid legacy fixture; identifiers come only from the fixed LIMITS map.
            new TransactionTemplate(transactions).executeWithoutResult(status -> jdbc.update("UPDATE change_request SET " + field + "=? WHERE id=?", "  ", id));
            var before = new TransactionTemplate(transactions).execute(status -> jdbc.queryForMap("SELECT * FROM change_request WHERE id=?", id));
            problem(write(observed, owner.value(), true, null, tag(observed)), 400, "INVALID_CONTENT");
            var after = new TransactionTemplate(transactions).execute(status -> jdbc.queryForMap("SELECT * FROM change_request WHERE id=?", id));
            assertThat(after).isEqualTo(before); assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review_decision WHERE request_id=?", Long.class, id)).isZero();
        }
    }
}
