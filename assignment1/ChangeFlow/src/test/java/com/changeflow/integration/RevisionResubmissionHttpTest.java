package com.changeflow.integration;

import com.changeflow.application.*;
import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class RevisionResubmissionHttpTest extends HttpTestSupport {
    @Autowired ChangeRequestService service;
    @Autowired CredentialAuthenticationService authentication;
    private HttpResponse<String> write(StoredSnapshot item, String token, boolean submit, String body, long revision) throws Exception {
        var id = item.parent().request().id();
        return client.send(request("/api/change-requests/" + id + (submit ? "/submit" : ""), token).header("Content-Type", "application/json")
                .header("If-Match", "\"cr-" + id + "-v" + revision + "\"").method(submit ? "POST" : "PATCH", HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void ownerRequesterAndDualRoleCanReviseResubmitAndCannotReplay(boolean dualRole) throws Exception {
        var owner = dualRole ? user(UserRole.Requester, UserRole.Reviewer) : user(UserRole.Requester);
        var seed = seed(owner.userId(), RequestState.ChangesRequested, "Please retain my explanation"); var before = stored(seed.request().id());
        assertThat(write(before, owner.value(), false, "{\"title\":\"  Revised title  \",\"description\":\"Revised  details\\nline\",\"reason\":\"Revised reason\"}", before.parent().revision()).statusCode()).isEqualTo(200);
        var revised = stored(seed.request().id()); var old = seed.request();
        assertThat(revised.parent().request()).isEqualTo(new ChangeRequest(old.id(), "Revised title", "Revised  details\nline", "Revised reason", old.requesterId(), RequestState.ChangesRequested, old.latestExplanation(), old.createdAt(), revised.parent().request().updatedAt()));
        assertThat(revised.parent().revision()).isEqualTo(before.parent().revision() + 1); assertThat(revised.decisions()).isEqualTo(before.decisions());
        assertThat(write(revised, owner.value(), true, "{}", revised.parent().revision()).statusCode()).isEqualTo(200);
        var submitted = stored(seed.request().id());
        assertThat(submitted.parent().request()).isEqualTo(new ChangeRequest(old.id(), "Revised title", "Revised  details\nline", "Revised reason", old.requesterId(), RequestState.PendingReview, old.latestExplanation(), old.createdAt(), submitted.parent().request().updatedAt()));
        assertThat(submitted.parent().revision()).isEqualTo(revised.parent().revision() + 1); assertThat(submitted.decisions()).isEqualTo(before.decisions());
        assertThat(write(submitted, owner.value(), true, "{}", revised.parent().revision()).statusCode()).isEqualTo(412);
        assertThat(stored(seed.request().id())).isEqualTo(submitted);
        assertThat(write(submitted, owner.value(), true, "{}", submitted.parent().revision()).statusCode()).isEqualTo(409);
        assertThat(stored(seed.request().id())).isEqualTo(submitted);
        assertThat(write(submitted, owner.value(), false, "{\"title\":\"Unauthorized change\"}", submitted.parent().revision()).statusCode()).isEqualTo(409);
        assertThat(stored(seed.request().id())).isEqualTo(submitted);
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void nonOwnersCannotReviseOrResubmitEvenWithBothRoles(boolean submit) throws Exception {
        var owner = user(UserRole.Requester); var seed = seed(owner.userId(), RequestState.ChangesRequested); var before = stored(seed.request().id());
        var requester = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        var dual = user(UserRole.Requester, UserRole.Reviewer); var neither = user();
        for (var actor : List.of(requester, reviewer, dual, neither)) {
            assertThat(write(before, actor.value(), submit, "{}", 99).statusCode()).isEqualTo(actor == reviewer || actor == dual ? 403 : 404);
            assertThat(stored(seed.request().id())).isEqualTo(before);
        }
        assertThat(write(before, null, submit, "{}", before.parent().revision()).statusCode()).isEqualTo(401);
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void currentRemovedPermissionsAndRevocationOverrideAnEarlierAuthentication(boolean submit) throws Exception {
        var owner = user(UserRole.Requester, UserRole.Reviewer);
        var caller = authentication.authenticate(owner.value());
        var seed = seed(owner.userId(), RequestState.ChangesRequested); var before = stored(seed.request().id());
        org.assertj.core.api.ThrowableAssert.ThrowingCallable attempt = submit
                ? () -> service.submit(caller, seed.request().id(), 99)
                : () -> service.edit(caller, seed.request().id(), 99, new ChangeRequest.Edit(false, null, false, null, false, null));
        assertThatThrownBy(attempt).isInstanceOfSatisfying(ApplicationFailure.class, failure -> assertThat(failure.code()).isEqualTo(ApplicationFailure.Code.STALE_VERSION));
        administration.removeRole(owner.userId(), UserRole.Requester);
        assertThatThrownBy(attempt).isInstanceOfSatisfying(ApplicationFailure.class, failure -> assertThat(failure.code()).isEqualTo(ApplicationFailure.Code.PERMISSION_DENIED));
        assertThat(write(before, owner.value(), submit, "{}", 99).statusCode()).isEqualTo(403);
        assertThat(stored(seed.request().id())).isEqualTo(before);
        administration.removeRole(owner.userId(), UserRole.Reviewer);
        assertThat(write(before, owner.value(), submit, "{}", before.parent().revision()).statusCode()).isEqualTo(404);
        assertThat(stored(seed.request().id())).isEqualTo(before);
        administration.revokeCredential(owner.credentialId());
        assertThatThrownBy(attempt).isInstanceOfSatisfying(ApplicationFailure.class, failure -> assertThat(failure.code()).isEqualTo(ApplicationFailure.Code.AUTHENTICATION_FAILED));
        assertThat(write(before, owner.value(), submit, "{}", before.parent().revision()).statusCode()).isEqualTo(401);
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @Test void remainingRequesterRoleStillPermitsTheReturnedOwnerJourney() throws Exception {
        var owner = user(UserRole.Requester, UserRole.Reviewer); var seed = seed(owner.userId(), RequestState.ChangesRequested); var before = stored(seed.request().id());
        administration.removeRole(owner.userId(), UserRole.Reviewer);
        assertThat(write(before, owner.value(), false, "{\"reason\":\"Revised reason\"}", before.parent().revision()).statusCode()).isEqualTo(200);
        var revised = stored(seed.request().id());
        assertThat(revised.parent().request().latestExplanation()).isEqualTo(before.parent().request().latestExplanation());
        assertThat(write(revised, owner.value(), true, "{}", revised.parent().revision()).statusCode()).isEqualTo(200);
        var after = stored(seed.request().id());
        assertThat(after.parent().request().state()).isEqualTo(RequestState.PendingReview); assertThat(after.parent().request().reason()).isEqualTo("Revised reason");
        assertThat(after.parent().request().requesterId()).isEqualTo(owner.userId()); assertThat(after.parent().request().latestExplanation()).isEqualTo(before.parent().request().latestExplanation());
        assertThat(after.decisions()).isEqualTo(before.decisions());
    }
    @Test void pendingAndApprovedRejectBothOwnerOperationsIncludingNoOp() throws Exception {
        var owner = user(UserRole.Requester);
        for (var state : List.of(RequestState.PendingReview, RequestState.Approved)) {
            var seed = seed(owner.userId(), state, "Preserved feedback"); var before = stored(seed.request().id());
            for (boolean submit : List.of(false, true)) {
                assertThat(write(before, owner.value(), submit, "{}", before.parent().revision()).statusCode()).isEqualTo(409);
                assertThat(stored(seed.request().id())).isEqualTo(before);
            }
        }
    }
}
