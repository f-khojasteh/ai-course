package com.changeflow.integration;

import com.changeflow.application.*;
import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.test.annotation.DirtiesContext;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VisibilityHttpTest extends HttpTestSupport {
    private static final String ROOT = "/api/change-requests";
    private JsonNode get(String path, String token, int status) throws Exception {
        var response = call("GET", path, token, null, null, "application/json");
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
        assertThat(response.body()).doesNotContain("\"credential\"", "\"roles\"", "\"secret\"", "hibernateLazyInitializer");
        return json.readTree(response.body());
    }
    private List<UUID> ids(JsonNode page) {
        var result = new ArrayList<UUID>(); page.path("items").forEach(item -> result.add(UUID.fromString(item.path("id").asText()))); return result;
    }
    @Test void allRoleAndOwnershipCombinationsAcrossEveryState() throws Exception {
        var requester = user(UserRole.Requester);
        var reviewer = user(UserRole.Reviewer);
        var dual = user(UserRole.Requester, UserRole.Reviewer);
        var neither = user();
        var values = new ArrayList<RequestSnapshot>();
        for (var owner : List.of(requester, reviewer, dual, neither)) {
            for (var state : RequestState.values()) values.add(seed(owner.userId(), state, state == RequestState.Draft ? null : "Latest feedback"));
        }
        var before = new LinkedHashMap<UUID, StoredSnapshot>(); values.forEach(value -> before.put(value.request().id(), stored(value.request().id())));
        for (var actor : List.of(requester, reviewer, dual, neither)) {
            boolean canRequest = actor == requester || actor == dual;
            boolean canReview = actor == reviewer || actor == dual;
            var expected = values.stream().filter(value -> (canRequest && value.request().requesterId().equals(actor.userId())) || (canReview && value.request().state() != RequestState.Draft)).map(value -> value.request().id()).sorted(Comparator.comparing(UUID::toString)).toList();
            var page = get(ROOT, actor.value(), 200);
            assertThat(page.path("offset").asInt()).isZero(); assertThat(page.path("limit").asInt()).isEqualTo(50); assertThat(page.path("hasMore").asBoolean()).isFalse();
            assertThat(ids(page)).containsExactlyElementsOf(expected);
            for (var value : values) {
                boolean visible = expected.contains(value.request().id());
                String path = ROOT + "/" + value.request().id();
                var item = get(path, actor.value(), visible ? 200 : 404);
                if (visible) {
                    assertThat(item.path("requesterId").asText()).isEqualTo(value.request().requesterId().toString());
                    if (value.request().latestExplanation() == null) assertThat(item.path("latestExplanation").isNull()).isTrue();
                    else assertThat(item.path("latestExplanation").asText()).isEqualTo(value.request().latestExplanation());
                } else {
                    var missing = get(ROOT + "/" + UUID.randomUUID(), actor.value(), 404);
                    var hiddenCopy = item.deepCopy(); var missingCopy = missing.deepCopy();
                    ((com.fasterxml.jackson.databind.node.ObjectNode) hiddenCopy).remove("instance");
                    ((com.fasterxml.jackson.databind.node.ObjectNode) missingCopy).remove("instance");
                    assertThat(hiddenCopy).isEqualTo(missingCopy);
                    assertThat(item.toString()).doesNotContain("Latest feedback", "Seed proposal", "requesterId");
                }
            }
        }
        before.forEach((id, snapshot) -> assertThat(stored(id)).isEqualTo(snapshot));
    }
    @Test void visibilityPredicateRunsBeforeArbitraryOffsetPagination() throws Exception {
        var owner = user(UserRole.Requester);
        var other = user(UserRole.Requester);
        var visible = new ArrayList<RequestSnapshot>();
        var all = new ArrayList<RequestSnapshot>();
        for (int index = 0; index < 106; index++) {
            all.add(seed(other.userId(), RequestState.Draft));
            var value = seed(owner.userId(), RequestState.Draft); visible.add(value); all.add(value);
        }
        var expected = visible.stream().map(value -> value.request().id()).sorted(Comparator.comparing(UUID::toString)).toList();
        var snapshots = new LinkedHashMap<UUID, StoredSnapshot>(); all.forEach(value -> snapshots.put(value.request().id(), stored(value.request().id())));
        var first = get(ROOT, owner.value(), 200);
        assertThat(ids(first)).containsExactlyElementsOf(expected.subList(0, 50));
        assertThat(first.path("hasMore").asBoolean()).isTrue();
        var max = get(ROOT + "?offset=3&limit=100", owner.value(), 200);
        assertThat(ids(max)).containsExactlyElementsOf(expected.subList(3, 103));
        assertThat(max.path("hasMore").asBoolean()).isTrue();
        var tail = get(ROOT + "?offset=103&limit=100", owner.value(), 200);
        assertThat(ids(tail)).containsExactlyElementsOf(expected.subList(103, 106));
        assertThat(tail.path("hasMore").asBoolean()).isFalse();
        assertThat(ids(get(ROOT + "?offset=106&limit=1", owner.value(), 200))).isEmpty();
        snapshots.forEach((id, snapshot) -> assertThat(stored(id)).isEqualTo(snapshot));
    }
    @Test void authenticationAndCurrentRolesDetermineReadsWithoutMutation() throws Exception {
        var owner = user(UserRole.Requester, UserRole.Reviewer);
        var draft = seed(owner.userId(), RequestState.Draft);
        var pending = seed(owner.userId(), RequestState.PendingReview);
        var draftBefore = stored(draft.request().id()); var pendingBefore = stored(pending.request().id());
        for (String path : List.of(ROOT, ROOT + "/" + draft.request().id(), ROOT + "/" + UUID.randomUUID())) {
            var response = call("GET", path, null, null, null, "text/html");
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.headers().firstValue("WWW-Authenticate")).hasValue("Bearer");
            assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo("AUTHENTICATION_FAILED");
        }
        administration.removeRole(owner.userId(), UserRole.Requester);
        get(ROOT + "/" + draft.request().id(), owner.value(), 404);
        get(ROOT + "/" + pending.request().id(), owner.value(), 200);
        administration.removeRole(owner.userId(), UserRole.Reviewer);
        get(ROOT + "/" + pending.request().id(), owner.value(), 404);
        assertThat(ids(get(ROOT, owner.value(), 200))).isEmpty();
        administration.revokeCredential(owner.credentialId());
        get(ROOT + "/" + pending.request().id(), owner.value(), 401);
        assertThat(stored(draft.request().id())).isEqualTo(draftBefore);
        assertThat(stored(pending.request().id())).isEqualTo(pendingBefore);
    }
}
