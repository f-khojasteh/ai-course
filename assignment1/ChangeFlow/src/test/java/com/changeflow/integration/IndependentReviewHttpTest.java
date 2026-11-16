package com.changeflow.integration;

import com.changeflow.domain.*;
import com.changeflow.application.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class IndependentReviewHttpTest extends HttpTestSupport {
    @Autowired ChangeRequestService service;
    @Autowired CredentialAuthenticationService authentication;
    private String path(UUID id) { return "/api/change-requests/" + id; }
    private HttpResponse<String> review(StoredSnapshot before, String action, String token) throws Exception {
        return review(before, action, token, before.parent().revision());
    }
    private HttpResponse<String> review(StoredSnapshot before, String action, String token, long revision) throws Exception {
        var id = before.parent().request().id();
        var builder = request(path(id) + "/" + action, token).header("Content-Type", "application/json")
                .header("X-Roles", "Reviewer").header("X-Requester-Id", UUID.randomUUID().toString())
                .header("If-Match", "\"cr-" + id + "-v" + revision + "\"");
        String body = action.equals("approve") ? "{}" : "{\"explanation\":\"Please revise\"}";
        return client.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @ParameterizedTest @ValueSource(strings = {"approve", "request-changes"})
    void independentReviewerAndDualRoleNonOwnerPersistOneDecision(String action) throws Exception {
        var owner = user(UserRole.Requester);
        for (var actor : List.of(user(UserRole.Reviewer), user(UserRole.Requester, UserRole.Reviewer))) {
            var seed = seed(owner.userId(), RequestState.PendingReview, "Prior feedback");
            var before = stored(seed.request().id());
            assertThat(review(before, action, actor.value()).statusCode()).isEqualTo(200);
            var after = stored(seed.request().id()); var old = seed.request();
            assertThat(after.parent().request()).isEqualTo(new ChangeRequest(old.id(), old.title(), old.description(), old.reason(), old.requesterId(),
                    action.equals("approve") ? RequestState.Approved : RequestState.ChangesRequested,
                    action.equals("approve") ? old.latestExplanation() : "Please revise", old.createdAt(), after.parent().request().updatedAt()));
            assertThat(after.parent().request().updatedAt()).isAfterOrEqualTo(old.updatedAt());
            assertThat(after.parent().revision()).isEqualTo(seed.revision() + 1);
            assertThat(after.decisions()).hasSize(1);
            var decision = after.decisions().get(0);
            assertThat(decision.id()).isNotNull(); assertThat(decision.reviewerId()).isEqualTo(actor.userId());
            assertThat(decision.action()).isEqualTo(action.equals("approve") ? "Approve" : "RequestChanges");
            assertThat(decision.revision()).isEqualTo(after.parent().revision());
            assertThat(decision.decidedAt()).isEqualTo(after.parent().request().updatedAt());
        }
    }
    @ParameterizedTest @ValueSource(strings = {"approve", "request-changes"})
    void everyVisibleOwnerIsForbiddenFromSelfReviewRegardlessOfRoles(String action) throws Exception {
        for (var owner : List.of(user(UserRole.Requester), user(UserRole.Reviewer), user(UserRole.Requester, UserRole.Reviewer))) {
            var seed = seed(owner.userId(), RequestState.PendingReview, "Existing feedback"); var before = stored(seed.request().id());
            assertThat(review(before, action, owner.value()).statusCode()).isEqualTo(403);
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
    }
    @ParameterizedTest @ValueSource(strings = {"approve", "request-changes"})
    void missingPermissionSpoofedHeadersAndMissingResourcesHaveNoEffect(String action) throws Exception {
        var owner = user(UserRole.Requester);
        var seed = seed(owner.userId(), RequestState.PendingReview, "Private feedback"); var before = stored(seed.request().id());
        for (var actor : List.of(user(UserRole.Requester), user())) {
            assertThat(review(before, action, actor.value()).statusCode()).isEqualTo(404);
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
        assertThat(review(before, action, null).statusCode()).isEqualTo(401);
        assertThat(stored(seed.request().id())).isEqualTo(before);
        var reviewer = user(UserRole.Requester, UserRole.Reviewer);
        administration.removeRole(reviewer.userId(), UserRole.Reviewer);
        assertThat(review(before, action, reviewer.value()).statusCode()).isEqualTo(404);
        assertThat(stored(seed.request().id())).isEqualTo(before);
        administration.revokeCredential(reviewer.credentialId());
        assertThat(review(before, action, reviewer.value()).statusCode()).isEqualTo(401);
        assertThat(stored(seed.request().id())).isEqualTo(before);
        var active = user(UserRole.Reviewer);
        var builder = request(path(UUID.randomUUID()) + "/" + action, active.value()).header("Content-Type", "application/json").header("If-Match", "\"cr-" + UUID.randomUUID() + "-v0\"");
        assertThat(client.send(builder.POST(HttpRequest.BodyPublishers.ofString(action.equals("approve") ? "{}" : "{\"explanation\":\"Valid\"}")).build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(404);
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @ParameterizedTest @ValueSource(strings = {"approve", "request-changes"})
    void wrongStatesAndRepeatedCommandsNeverReplaySuccess(String action) throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        for (var state : List.of(RequestState.Draft, RequestState.ChangesRequested, RequestState.Approved)) {
            var seed = seed(owner.userId(), state); var before = stored(seed.request().id());
            assertThat(review(before, action, reviewer.value()).statusCode()).isEqualTo(state == RequestState.Draft ? 404 : 409);
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
        var seed = seed(owner.userId(), RequestState.PendingReview); var before = stored(seed.request().id());
        assertThat(review(before, action, reviewer.value()).statusCode()).isEqualTo(200);
        var after = stored(seed.request().id());
        assertThat(review(after, action, reviewer.value(), before.parent().revision()).statusCode()).isEqualTo(412);
        assertThat(stored(seed.request().id())).isEqualTo(after);
        assertThat(review(after, action, reviewer.value()).statusCode()).isEqualTo(409);
        assertThat(stored(seed.request().id())).isEqualTo(after);
    }
    @ParameterizedTest @ValueSource(strings = {"approve", "request-changes"})
    void approvedRequestCannotBeEditedSubmittedReviewedOrPublished(String attemptedReview) throws Exception {
        var owner = user(UserRole.Requester, UserRole.Reviewer); var reviewer = user(UserRole.Reviewer);
        var seed = seed(owner.userId(), RequestState.PendingReview, "Preserved feedback");
        assertThat(review(stored(seed.request().id()), "approve", reviewer.value()).statusCode()).isEqualTo(200);
        var approved = stored(seed.request().id());
        assertThat(review(approved, attemptedReview, reviewer.value()).statusCode()).isEqualTo(409);
        assertThat(stored(seed.request().id())).isEqualTo(approved);
        for (String suffix : List.of("", "/submit")) {
            var builder = request(path(seed.request().id()) + suffix, owner.value()).header("Content-Type", "application/json")
                    .header("If-Match", "\"cr-" + seed.request().id() + "-v" + approved.parent().revision() + "\"");
            assertThat(client.send(builder.method(suffix.isEmpty() ? "PATCH" : "POST", HttpRequest.BodyPublishers.ofString(suffix.isEmpty() ? "{\"title\":\"Changed\"}" : "{}")).build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(409);
            assertThat(stored(seed.request().id())).isEqualTo(approved);
        }
        assertThat(call("DELETE", path(seed.request().id()), owner.value(), null, null, "application/json").statusCode()).isEqualTo(405);
        assertThat(stored(seed.request().id())).isEqualTo(approved);
        for (String action : List.of("cancel", "withdraw", "publish", "execute", "history")) {
            assertThat(call("POST", path(seed.request().id()) + "/" + action, reviewer.value(), "{}", "application/json", "application/json").statusCode()).isEqualTo(404);
            assertThat(stored(seed.request().id())).isEqualTo(approved);
        }
    }
    @ParameterizedTest @ValueSource(strings = {"approve", "request-changes"})
    void applicationRechecksAnAlreadyAuthenticatedReviewerAfterAccessChanges(String action) {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Requester, UserRole.Reviewer);
        var caller = authentication.authenticate(reviewer.value());
        var seed = seed(owner.userId(), RequestState.PendingReview, "Private feedback"); var before = stored(seed.request().id());
        org.assertj.core.api.ThrowableAssert.ThrowingCallable attempt = action.equals("approve")
                ? () -> service.approve(caller, seed.request().id(), 99)
                : () -> service.requestChanges(caller, seed.request().id(), 99, "New feedback");
        assertThatThrownBy(attempt).isInstanceOfSatisfying(ApplicationFailure.class,
                failure -> assertThat(failure.code()).isEqualTo(ApplicationFailure.Code.STALE_VERSION));
        assertThat(stored(seed.request().id())).isEqualTo(before);
        administration.removeRole(reviewer.userId(), UserRole.Reviewer);
        assertThatThrownBy(attempt).isInstanceOfSatisfying(ApplicationFailure.class,
                failure -> assertThat(failure.code()).isEqualTo(ApplicationFailure.Code.NOT_FOUND));
        assertThat(stored(seed.request().id())).isEqualTo(before);
        administration.revokeCredential(reviewer.credentialId());
        assertThatThrownBy(attempt).isInstanceOfSatisfying(ApplicationFailure.class,
                failure -> assertThat(failure.code()).isEqualTo(ApplicationFailure.Code.AUTHENTICATION_FAILED));
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
}
