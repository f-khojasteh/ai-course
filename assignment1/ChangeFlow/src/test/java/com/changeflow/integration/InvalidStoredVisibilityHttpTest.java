package com.changeflow.integration;

import com.changeflow.application.LocalAdministrationService.Provisioned;
import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.http.*;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

/** T082: invalid legacy content cannot reveal an otherwise invisible record. */
class InvalidStoredVisibilityHttpTest extends HttpTestSupport {
    static Stream<Arguments> hiddenCases() {
        return Arrays.stream(RequestState.values()).flatMap(state -> Stream.of("title", "description", "reason")
                .flatMap(field -> Stream.of("otherRequester", "ownerRequesterRemoved", "reviewerRemoved",
                                "otherReviewer", "otherDualRole", "ownerRequesterRemovedReviewerRemains")
                        .filter(actor -> state == RequestState.Draft || !Set.of("otherReviewer", "otherDualRole", "ownerRequesterRemovedReviewerRemains").contains(actor))
                        .map(actor -> Arguments.of(state, field, actor))));
    }

    private record StoredRows(List<List<String>> parent, List<List<String>> decisions) { }
    private StoredRows raw(UUID id) {
        var tx = new TransactionTemplate(transactions);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setReadOnly(true);
        return tx.execute(status -> new StoredRows(rows("SELECT * FROM change_request WHERE id=?", id),
                rows("SELECT * FROM review_decision WHERE request_id=? ORDER BY resulting_request_version", id)));
    }
    private List<List<String>> rows(String sql, UUID id) {
        return jdbc.query(sql, (row, number) -> {
            var values = new ArrayList<String>();
            for (int column = 1; column <= row.getMetaData().getColumnCount(); column++) values.add(row.getString(column));
            return values;
        }, id);
    }
    private void corrupt(UUID id, String field) {
        assertThat(Set.of("title", "description", "reason")).contains(field);
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                jdbc.update("UPDATE change_request SET " + field + "=? WHERE id=?", "  ", id));
    }
    private HttpResponse<String> attempt(UUID id, long revision, Provisioned actor, String operation) throws Exception {
        var builder = request("/api/change-requests/" + id + (Set.of("GET", "PATCH").contains(operation) ? "" : "/" + operation), actor.value());
        if (operation.equals("GET")) return client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
        builder.header("If-Match", "\"cr-" + id + "-v" + revision + "\"").header("Content-Type", "application/json");
        String body = switch (operation) {
            case "PATCH" -> "{\"title\":\"Valid proposed edit\"}";
            case "request-changes" -> "{\"explanation\":\"Valid independent feedback\"}";
            default -> "{}";
        };
        return client.send(builder.method(operation.equals("PATCH") ? "PATCH" : "POST", HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @ParameterizedTest(name = "{0}, invalid {1}, {2}") @MethodSource("hiddenCases")
    void invisibleInvalidContentIsIndistinguishableFromMissingAcrossAllItemRoutes(RequestState state, String field, String kind) throws Exception {
        var owner = kind.equals("ownerRequesterRemovedReviewerRemains") ? user(UserRole.Requester, UserRole.Reviewer) : user(UserRole.Requester);
        Provisioned actor = switch (kind) {
            case "ownerRequesterRemoved", "ownerRequesterRemovedReviewerRemains" -> owner;
            case "otherReviewer", "reviewerRemoved" -> user(UserRole.Reviewer);
            case "otherDualRole" -> user(UserRole.Requester, UserRole.Reviewer);
            default -> user(UserRole.Requester);
        };
        var seed = seed(owner.userId(), state);
        if (kind.startsWith("ownerRequesterRemoved")) administration.removeRole(owner.userId(), UserRole.Requester);
        if (kind.equals("reviewerRemoved")) administration.removeRole(actor.userId(), UserRole.Reviewer);
        assertThat(attempt(seed.request().id(), seed.revision(), actor, "GET").statusCode()).isEqualTo(404);
        corrupt(seed.request().id(), field);
        var before = raw(seed.request().id());
        UUID missing = UUID.randomUUID();
        for (String operation : List.of("GET", "PATCH", "submit", "approve", "request-changes")) {
            var absent = attempt(missing, seed.revision(), actor, operation);
            var hidden = attempt(seed.request().id(), seed.revision(), actor, operation);
            assertThat(absent.statusCode()).isEqualTo(404);
            assertThat(hidden.statusCode()).as("hidden %s must match missing", operation).isEqualTo(404);
            var expected = json.readTree(absent.body()); var actual = json.readTree(hidden.body());
            assertThat(actual.path("code").asText()).isEqualTo("NOT_FOUND");
            // instance necessarily names the requested URI; the remaining problem is identical.
            ((com.fasterxml.jackson.databind.node.ObjectNode) expected).remove("instance");
            ((com.fasterxml.jackson.databind.node.ObjectNode) actual).remove("instance");
            assertThat(actual).isEqualTo(expected);
            assertThat(hidden.headers().firstValue("ETag")).isEmpty();
            assertThat(hidden.body()).doesNotContain("INVALID_CONTENT", "Seed feedback", "Seed proposal");
            assertThat(raw(seed.request().id())).isEqualTo(before);
        }
    }

    static Stream<Arguments> invalidSubmissions() {
        return Stream.of(RequestState.Draft, RequestState.ChangesRequested).flatMap(state ->
                Stream.of("title", "description", "reason").map(field -> Arguments.of(state, field)));
    }
    @ParameterizedTest @MethodSource("invalidSubmissions")
    void authorizedInvalidSubmissionStillRejectsWithoutRepair(RequestState state, String field) throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), state);
        corrupt(seed.request().id(), field); var before = raw(seed.request().id());
        var response = attempt(seed.request().id(), seed.revision(), owner, "submit");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo("INVALID_CONTENT");
        assertThat(raw(seed.request().id())).isEqualTo(before);
    }
}
