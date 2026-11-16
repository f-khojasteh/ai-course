package com.changeflow.integration;

import com.changeflow.application.LocalAdministrationService.Provisioned;
import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.net.http.*;
import java.util.*;
import java.util.stream.*;
import static org.assertj.core.api.Assertions.*;

class ForbiddenOperationHttpTest extends HttpTestSupport {
    private static final List<Set<UserRole>> ROLES = List.of(Set.of(), Set.of(UserRole.Requester), Set.of(UserRole.Reviewer), Set.of(UserRole.Requester, UserRole.Reviewer));
    static Stream<Arguments> matrix() { return Arrays.stream(RequestState.values()).flatMap(state -> IntStream.range(0, 4).boxed().flatMap(ownerRole -> IntStream.range(0, 5).mapToObj(actor -> Arguments.of(state, ownerRole, actor)))); }
    private HttpResponse<String> mutate(StoredSnapshot item, Provisioned actor, String action, long revision, Object body) throws Exception {
        UUID id = item.parent().request().id();
        return client.send(request("/api/change-requests/" + id + (action.equals("edit") ? "" : "/" + action), actor.value()).header("Content-Type", "application/json")
                .header("If-Match", "\"cr-" + id + "-v" + revision + "\"").method(action.equals("edit") ? "PATCH" : "POST", HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }
    @ParameterizedTest(name = "{0}, ownerRoles={1}, actor={2}") @MethodSource("matrix")
    void forbiddenWritesAndOldVersionsPreserveAllStatesForEveryRoleCombination(RequestState state, int ownerRole, int actorKind) throws Exception {
        var owner = user(ROLES.get(ownerRole).toArray(UserRole[]::new));
        boolean isOwner = actorKind == 0; var roles = ROLES.get(isOwner ? ownerRole : actorKind - 1);
        var actor = isOwner ? owner : user(roles.toArray(UserRole[]::new));
        var seed = seed(owner.userId(), state, state == RequestState.Draft ? null : "Private retained feedback"); UUID id = seed.request().id(); var before = stored(id);
        boolean visible = (isOwner && roles.contains(UserRole.Requester)) || (roles.contains(UserRole.Reviewer) && state != RequestState.Draft);
        for (String action : List.of("edit", "submit", "approve", "request-changes")) {
            boolean ownerAction = action.equals("edit") || action.equals("submit");
            boolean permitted = ownerAction ? isOwner && roles.contains(UserRole.Requester) : !isOwner && roles.contains(UserRole.Reviewer);
            Object body = action.equals("edit") ? Map.of("title", "Forbidden replacement") : action.equals("request-changes") ? Map.of("explanation", "Forbidden feedback") : Map.of();
            int staleStatus = !visible ? 404 : !permitted ? 403 : 412;
            assertThat(mutate(before, actor, action, 99, body).statusCode()).isEqualTo(staleStatus); assertThat(stored(id)).isEqualTo(before);
            boolean validState = ownerAction ? state == RequestState.Draft || state == RequestState.ChangesRequested : state == RequestState.PendingReview;
            if (!visible || !permitted || !validState) {
                assertThat(mutate(before, actor, action, seed.revision(), body).statusCode()).isEqualTo(!visible ? 404 : !permitted ? 403 : 409);
                assertThat(stored(id)).isEqualTo(before);
            }
        }
        var deletion = call("DELETE", "/api/change-requests/" + id, actor.value(), null, null, "application/json");
        assertThat(deletion.statusCode()).isEqualTo(405); assertThat(deletion.headers().firstValue("Allow").orElse("")).contains("GET", "PATCH"); assertThat(stored(id)).isEqualTo(before);
        for (String action : List.of("cancel", "withdraw", "execute", "publish", "history")) {
            assertThat(call("POST", "/api/change-requests/" + id + "/" + action, actor.value(), "{}", "application/json", "application/json").statusCode()).isEqualTo(404);
            assertThat(stored(id)).isEqualTo(before);
        }
    }
    @Test void matchingControlledFieldsRejectWholeSubmitAndReviewCommands() throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        for (String action : List.of("submit", "approve", "request-changes")) {
            var seed = seed(owner.userId(), action.equals("submit") ? RequestState.Draft : RequestState.PendingReview); var before = stored(seed.request().id());
            var actor = action.equals("submit") ? owner : reviewer;
            for (var field : Map.<String, Object>of("requesterId", owner.userId(), "roles", List.of(action.equals("submit") ? "Requester" : "Reviewer"), "state", seed.request().state().name()).entrySet()) {
                var body = new LinkedHashMap<String, Object>(); if (action.equals("request-changes")) body.put("explanation", "Valid feedback"); body.put(field.getKey(), field.getValue());
                var response = mutate(before, actor, action, seed.revision(), body);
                assertThat(response.statusCode()).isEqualTo(400); assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo("FORBIDDEN_FIELD");
                assertThat(stored(seed.request().id())).isEqualTo(before);
            }
        }
    }
}
