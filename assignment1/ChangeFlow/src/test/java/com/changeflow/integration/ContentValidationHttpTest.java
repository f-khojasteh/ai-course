package com.changeflow.integration;

import com.changeflow.application.RequestSnapshot;
import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.http.*;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class ContentValidationHttpTest extends HttpTestSupport {
    private static final String ROOT = "/api/change-requests";
    private static final Map<String, Integer> LIMITS = Map.of("title", 200, "description", 5000, "reason", 2000);
    private Map<String, Object> proposal() { return new LinkedHashMap<>(Map.of("title", "Proposal", "description", "Details", "reason", "Benefit")); }
    private HttpResponse<String> write(String token, RequestSnapshot seed, Object body) throws Exception {
        var builder = request(ROOT + (seed == null ? "" : "/" + seed.request().id()), token).header("Content-Type", "application/json");
        if (seed != null) builder.header("If-Match", "\"cr-" + seed.request().id() + "-v" + seed.revision() + "\"");
        return client.send(builder.method(seed == null ? "POST" : "PATCH", HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }
    private void invalid(HttpResponse<String> response, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo(code);
        assertThat(response.body()).doesNotContain("rejectedValue");
    }
    static Stream<Arguments> invalidFields() {
        return LIMITS.entrySet().stream().flatMap(field -> Stream.of("omitted", "null", "empty", "blank", "excessive").map(kind -> Arguments.of(field.getKey(), field.getValue(), kind)));
    }
    @ParameterizedTest(name = "{0}: {2}") @MethodSource("invalidFields")
    void invalidCreationAndMixedPartialEditAreAtomic(String field, int maximum, String kind) throws Exception {
        var owner = user(UserRole.Requester);
        var seed = seed(owner.userId(), RequestState.Draft);
        var before = stored(seed.request().id());
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class);
        var body = proposal();
        Object invalid = switch (kind) { case "null", "omitted" -> null; case "empty" -> ""; case "blank" -> " \t\n\u2003 "; default -> "x".repeat(maximum + 1); };
        if (kind.equals("omitted")) body.remove(field); else body.put(field, invalid);
        invalid(write(owner.value(), null, body), "INVALID_CONTENT");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(count);
        if (!kind.equals("omitted")) {
            var partial = new LinkedHashMap<String, Object>(); partial.put(field.equals("title") ? "description" : "title", "Valid change"); partial.put(field, invalid);
            invalid(write(owner.value(), seed, partial), "INVALID_CONTENT");
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
    }
    @Test void normalizedBoundariesAndSupplementaryUnicodeRoundTripWithoutTruncation() throws Exception {
        var owner = user(UserRole.Requester);
        for (var field : LIMITS.entrySet()) {
            for (String point : List.of("x", new String(Character.toChars(0x1F680)))) {
                String exact = point.repeat(field.getValue());
                var body = proposal(); body.put(field.getKey(), "\u2003 " + exact + " \n");
                var response = write(owner.value(), null, body);
                assertThat(response.statusCode()).isEqualTo(201);
                UUID id = UUID.fromString(json.readTree(response.body()).path("id").asText());
                assertThat(json.readTree(response.body()).path(field.getKey()).asText()).isEqualTo(exact);
                var seed = stored(id).parent();
                assertThat(json.valueToTree(seed.request()).path(field.getKey()).asText()).isEqualTo(exact);
                String another = point.equals("x") ? "y".repeat(field.getValue()) : new String(Character.toChars(0x1F600)).repeat(field.getValue());
                assertThat(write(owner.value(), seed, Map.of(field.getKey(), " \t" + another + " \n")).statusCode()).isEqualTo(200);
                var before = stored(id);
                assertThat(json.valueToTree(before.parent().request()).path(field.getKey()).asText()).isEqualTo(another);
                body.put(field.getKey(), exact + point);
                long count = jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class);
                invalid(write(owner.value(), null, body), "INVALID_CONTENT");
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(count);
                invalid(write(owner.value(), before.parent(), Map.of(field.getKey(), exact + point)), "INVALID_CONTENT");
                assertThat(stored(id)).isEqualTo(before);
            }
        }
    }
    @Test void partialEditPreservesOmissionAndInternalWhitespace() throws Exception {
        var owner = user(UserRole.Requester);
        var seed = seed(owner.userId(), RequestState.Draft);
        var response = write(owner.value(), seed, Map.of("title", "\t New  title\nline \u2003"));
        assertThat(response.statusCode()).isEqualTo(200);
        var after = stored(seed.request().id());
        var old = seed.request();
        assertThat(after.parent().request()).isEqualTo(new ChangeRequest(old.id(), "New  title\nline", old.description(), old.reason(), old.requesterId(), old.state(), old.latestExplanation(), old.createdAt(), after.parent().request().updatedAt()));
        assertThat(after.parent().revision()).isEqualTo(seed.revision() + 1);
        assertThat(after.decisions()).isEmpty();
    }
    @Test void matchingControlledFieldsAndUnknownFieldsRejectWholeCreateAndEdit() throws Exception {
        var owner = user(UserRole.Requester, UserRole.Reviewer);
        var seed = seed(owner.userId(), RequestState.Draft);
        var before = stored(seed.request().id());
        var controlled = new LinkedHashMap<String, Object>();
        controlled.put("requesterId", owner.userId()); controlled.put("requesterIdentity", owner.userId()); controlled.put("roles", List.of("Requester", "Reviewer")); controlled.put("state", "Draft");
        controlled.put("id", seed.request().id()); controlled.put("revision", seed.revision()); controlled.put("createdAt", seed.request().createdAt()); controlled.put("updatedAt", seed.request().updatedAt()); controlled.put("latestExplanation", null);
        controlled.put("typo", "unknown");
        for (var field : controlled.entrySet()) {
            String code = field.getKey().equals("typo") ? "UNKNOWN_FIELD" : "FORBIDDEN_FIELD";
            long count = jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class);
            var body = proposal(); body.put(field.getKey(), field.getValue());
            invalid(write(owner.value(), null, body), code);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(count);
            var edit = new LinkedHashMap<String, Object>(); edit.put("title", "Valid change"); edit.put(field.getKey(), field.getValue());
            invalid(write(owner.value(), seed, edit), code);
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
    }
    @Test void nonObjectAndNonTextInputCannotChangeBusinessData() throws Exception {
        var owner = user(UserRole.Requester);
        var seed = seed(owner.userId(), RequestState.Draft);
        var before = stored(seed.request().id());
        for (Object body : Arrays.asList(null, List.of(), 1, "text", Map.of("title", 12, "description", "D", "reason", "R"))) {
            long count = jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class);
            assertThat(write(owner.value(), null, body).statusCode()).isEqualTo(400);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(count);
            assertThat(write(owner.value(), seed, body).statusCode()).isEqualTo(400);
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
    }
    @Test void submitRejectsCorruptedStoredContentWithoutRepairOrAnyWrite() throws Exception {
        var owner = user(UserRole.Requester);
        for (String field : LIMITS.keySet()) {
            var seed = seed(owner.userId(), RequestState.Draft);
            UUID id = seed.request().id();
            // Deliberately invalid legacy/storage fixture. Column names come only from LIMITS.
            new TransactionTemplate(transactions).executeWithoutResult(status -> jdbc.update("UPDATE change_request SET " + field + "=? WHERE id=?", "  ", id));
            var before = new TransactionTemplate(transactions).execute(status -> jdbc.queryForMap("SELECT * FROM change_request WHERE id=?", id));
            var builder = request(ROOT + "/" + id + "/submit", owner.value()).header("If-Match", "\"cr-" + id + "-v" + seed.revision() + "\"");
            invalid(client.send(builder.POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString()), "INVALID_CONTENT");
            var after = new TransactionTemplate(transactions).execute(status -> jdbc.queryForMap("SELECT * FROM change_request WHERE id=?", id));
            assertThat(after).isEqualTo(before);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review_decision WHERE request_id=?", Long.class, id)).isZero();
        }
    }
}
