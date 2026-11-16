package com.changeflow.integration;

import com.changeflow.application.*;
import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.annotation.DirtiesContext;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VisibilitySecurityHttpTest extends HttpTestSupport {
    private JsonNode get(String path, String token, int status) throws Exception {
        var response = client.send(request(path, token).header("X-Roles", "Requester,Reviewer").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(status); assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
        assertThat(response.body()).doesNotContain("secretHash", "credentialId", "\"roles\"", "hibernateLazyInitializer");
        if (status == 401) assertThat(response.headers().firstValue("WWW-Authenticate")).hasValue("Bearer");
        return json.readTree(response.body());
    }
    private List<UUID> ids(JsonNode page) { var ids = new ArrayList<UUID>(); page.path("items").forEach(item -> ids.add(UUID.fromString(item.path("id").asText()))); return ids; }
    @ParameterizedTest @EnumSource(UserRole.class)
    void currentRoleRemovalChangesVisibilityBeforePaginationAndNeverChangesBusinessRows(UserRole removed) throws Exception {
        var dual = user(UserRole.Requester, UserRole.Reviewer); var other = user(UserRole.Requester);
        var values = new ArrayList<RequestSnapshot>(); var representatives = new ArrayList<RequestSnapshot>();
        for (var owner : List.of(dual, other)) for (var state : RequestState.values()) { var value = seed(owner.userId(), state, state == RequestState.Draft ? null : "Private latest feedback"); values.add(value); representatives.add(value); }
        for (int index = 0; index < 106; index++) { values.add(seed(dual.userId(), RequestState.Draft)); values.add(seed(other.userId(), RequestState.PendingReview, "Private latest feedback")); }
        var before = new LinkedHashMap<UUID, StoredSnapshot>(); values.forEach(value -> before.put(value.request().id(), stored(value.request().id())));
        for (int step = 0; step < 3; step++) {
            if (step == 1) administration.removeRole(dual.userId(), removed);
            if (step == 2) administration.removeRole(dual.userId(), removed == UserRole.Requester ? UserRole.Reviewer : UserRole.Requester);
            boolean requester = step == 0 || step == 1 && removed != UserRole.Requester;
            boolean reviewer = step == 0 || step == 1 && removed != UserRole.Reviewer;
            var expected = values.stream().filter(value -> requester && value.request().requesterId().equals(dual.userId()) || reviewer && value.request().state() != RequestState.Draft)
                    .map(value -> value.request().id()).sorted(Comparator.comparing(UUID::toString)).toList();
            var defaultPage = get("/api/change-requests", dual.value(), 200);
            assertThat(defaultPage.path("offset").asInt()).isZero(); assertThat(defaultPage.path("limit").asInt()).isEqualTo(50);
            assertThat(ids(defaultPage)).containsExactlyElementsOf(expected.subList(0, Math.min(50, expected.size())));
            assertThat(defaultPage.path("hasMore").asBoolean()).isEqualTo(expected.size() > 50);
            var all = new ArrayList<UUID>();
            for (int offset = 0; ; offset += 100) {
                var page = get("/api/change-requests?offset=" + offset + "&limit=100", dual.value(), 200); all.addAll(ids(page));
                assertThat(page.path("hasMore").asBoolean()).isEqualTo(expected.size() > offset + 100);
                if (!page.path("hasMore").asBoolean()) break;
            }
            assertThat(all).containsExactlyElementsOf(expected);
            if (!expected.isEmpty()) assertThat(ids(get("/api/change-requests?offset=3&limit=1", dual.value(), 200))).containsExactly(expected.get(3));
            for (var item : representatives) {
                boolean visible = expected.contains(item.request().id());
                var body = get("/api/change-requests/" + item.request().id(), dual.value(), visible ? 200 : 404);
                if (visible) {
                    if (item.request().latestExplanation() == null) assertThat(body.path("latestExplanation").isNull()).isTrue(); else assertThat(body.path("latestExplanation").asText()).isEqualTo("Private latest feedback");
                } else {
                    var missing = (com.fasterxml.jackson.databind.node.ObjectNode) get("/api/change-requests/" + UUID.randomUUID(), dual.value(), 404);
                    var hidden = (com.fasterxml.jackson.databind.node.ObjectNode) body; missing.remove("instance"); hidden.remove("instance");
                    assertThat(hidden).isEqualTo(missing); assertThat(hidden.toString()).doesNotContain("Private latest feedback", "requesterId", "Seed proposal");
                }
            }
        }
        for (String path : List.of("/api/change-requests", "/api/change-requests/" + representatives.get(0).request().id(), "/api/change-requests/" + UUID.randomUUID())) get(path, null, 401);
        before.forEach((id, value) -> assertThat(stored(id)).isEqualTo(value));
    }
    @Test void remainingRolesKeepOnlyTheirNormalWritesAndOwnershipStillForbidsReview() throws Exception {
        var formerRequester = user(UserRole.Requester, UserRole.Reviewer); var owner = user(UserRole.Requester);
        var ownDraft = seed(formerRequester.userId(), RequestState.Draft); var draftBefore = stored(ownDraft.request().id());
        var ownPending = seed(formerRequester.userId(), RequestState.PendingReview, "Own feedback"); var ownBefore = stored(ownPending.request().id());
        var foreign = seed(owner.userId(), RequestState.PendingReview, "Previous feedback"); var foreignBefore = stored(foreign.request().id());
        administration.removeRole(formerRequester.userId(), UserRole.Requester);
        get("/api/change-requests/" + ownDraft.request().id(), formerRequester.value(), 404);
        assertThat(call("POST", "/api/change-requests", formerRequester.value(), "{\"title\":\"Denied\",\"description\":\"Details\",\"reason\":\"Reason\"}", "application/json", "application/json").statusCode()).isEqualTo(403);
        for (String action : List.of("approve", "request-changes")) {
            var response = client.send(request("/api/change-requests/" + ownPending.request().id() + "/" + action, formerRequester.value()).header("Content-Type", "application/json").header("If-Match", "\"cr-" + ownPending.request().id() + "-v" + ownPending.revision() + "\"")
                    .POST(HttpRequest.BodyPublishers.ofString(action.equals("approve") ? "{}" : "{\"explanation\":\"Self feedback\"}")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(403); assertThat(stored(ownPending.request().id())).isEqualTo(ownBefore);
        }
        var approved = client.send(request("/api/change-requests/" + foreign.request().id() + "/approve", formerRequester.value()).header("If-Match", "\"cr-" + foreign.request().id() + "-v" + foreign.revision() + "\"").POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(approved.statusCode()).isEqualTo(200); var after = stored(foreign.request().id()); var old = foreign.request();
        assertThat(after.parent().request()).isEqualTo(new ChangeRequest(old.id(), old.title(), old.description(), old.reason(), old.requesterId(), RequestState.Approved, old.latestExplanation(), old.createdAt(), after.parent().request().updatedAt()));
        assertThat(after.parent().revision()).isEqualTo(foreignBefore.parent().revision() + 1); assertThat(after.decisions()).hasSize(1);
        var decision = after.decisions().get(0); assertThat(decision.id()).isNotNull(); assertThat(decision.reviewerId()).isEqualTo(formerRequester.userId());
        assertThat(decision.action()).isEqualTo("Approve"); assertThat(decision.revision()).isEqualTo(after.parent().revision()); assertThat(decision.decidedAt()).isEqualTo(after.parent().request().updatedAt());
        assertThat(stored(ownDraft.request().id())).isEqualTo(draftBefore);
        var formerReviewer = user(UserRole.Requester, UserRole.Reviewer); administration.removeRole(formerReviewer.userId(), UserRole.Reviewer);
        var created = call("POST", "/api/change-requests", formerReviewer.value(), "{\"title\":\"Allowed\",\"description\":\"Details\",\"reason\":\"Reason\"}", "application/json", "application/json");
        assertThat(created.statusCode()).isEqualTo(201); UUID createdId = UUID.fromString(json.readTree(created.body()).path("id").asText()); var createdValue = stored(createdId);
        assertThat(createdValue.parent().request().requesterId()).isEqualTo(formerReviewer.userId()); assertThat(createdValue.parent().request().state()).isEqualTo(RequestState.Draft); assertThat(createdValue.decisions()).isEmpty();
        get("/api/change-requests/" + foreign.request().id(), formerReviewer.value(), 404); assertThat(stored(foreign.request().id())).isEqualTo(after);
    }
}
