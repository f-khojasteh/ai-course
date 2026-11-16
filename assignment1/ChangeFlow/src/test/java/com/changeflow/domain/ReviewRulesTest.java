package com.changeflow.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class ReviewRulesTest {
    private final UUID owner = UUID.randomUUID(), reviewer = UUID.randomUUID();
    private final Instant at = Instant.parse("2026-11-16T10:00:00.123456789Z");
    private ChangeRequest value(RequestState state, String feedback) { return new ChangeRequest(UUID.randomUUID(), "Title", "Details\nwith  spaces", "Reason", owner, state, feedback, at, at); }
    @Test void approvalPreservesProposalIdentityAndAnyPriorExplanation() {
        for (String feedback : new String[]{null, "Earlier feedback"}) {
            var before = value(RequestState.PendingReview, feedback);
            assertThat(before.approve(reviewer, at.plusSeconds(1))).isEqualTo(new ChangeRequest(before.id(), before.title(), before.description(), before.reason(), owner, RequestState.Approved, feedback, at, at.plusSeconds(1)));
            assertThat(before.state()).isEqualTo(RequestState.PendingReview); assertThat(before.updatedAt()).isEqualTo(at);
        }
    }
    @Test void requestChangesNormalizesAndReplacesOnlyLatestFeedback() {
        var before = value(RequestState.PendingReview, "Earlier feedback");
        assertThat(before.requestChanges(reviewer, " \u2003 New  feedback\nline \t", at.plusSeconds(1))).isEqualTo(new ChangeRequest(before.id(), before.title(), before.description(), before.reason(), owner, RequestState.ChangesRequested, "New  feedback\nline", at, at.plusSeconds(1)));
        assertThat(before.latestExplanation()).isEqualTo("Earlier feedback"); assertThat(before.state()).isEqualTo(RequestState.PendingReview);
    }
    @Test void neitherReviewCanBePerformedByTheOwner() {
        var before = value(RequestState.PendingReview, "Existing feedback");
        assertThatIllegalArgumentException().isThrownBy(() -> before.approve(owner, at.plusSeconds(1)));
        assertThatIllegalArgumentException().isThrownBy(() -> before.requestChanges(owner, "Valid feedback", at.plusSeconds(1)));
        assertThat(before.state()).isEqualTo(RequestState.PendingReview); assertThat(before.latestExplanation()).isEqualTo("Existing feedback"); assertThat(before.updatedAt()).isEqualTo(at);
    }
    @Test void onlyPendingReviewAllowsDecisionsIncludingRepeatedDecisions() {
        for (var state : new RequestState[]{RequestState.Draft, RequestState.ChangesRequested, RequestState.Approved}) {
            var before = value(state, state == RequestState.ChangesRequested ? "Feedback" : null);
            assertThatIllegalStateException().isThrownBy(() -> before.approve(reviewer, at.plusSeconds(1)));
            assertThatIllegalStateException().isThrownBy(() -> before.requestChanges(reviewer, "Valid", at.plusSeconds(1)));
            assertThat(before.state()).isEqualTo(state); assertThat(before.updatedAt()).isEqualTo(at);
        }
        var pending = value(RequestState.PendingReview, "Earlier feedback");
        for (var decided : new ChangeRequest[]{pending.approve(reviewer, at.plusSeconds(1)), pending.requestChanges(reviewer, "Latest feedback", at.plusSeconds(1))}) {
            assertThatIllegalStateException().isThrownBy(() -> decided.approve(reviewer, at.plusSeconds(2)));
            assertThatIllegalStateException().isThrownBy(() -> decided.requestChanges(reviewer, "Another feedback", at.plusSeconds(2)));
        }
    }
    @Test void feedbackMustBeValidBeforeAnyNewValueIsCreated() {
        var before = value(RequestState.PendingReview, "Existing feedback");
        for (String bad : new String[]{null, "", " \t\n\u2003", "x".repeat(2001), new String(Character.toChars(0x1F680)).repeat(2001)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> before.requestChanges(reviewer, bad, at.plusSeconds(1)));
            assertThat(before.state()).isEqualTo(RequestState.PendingReview); assertThat(before.latestExplanation()).isEqualTo("Existing feedback"); assertThat(before.updatedAt()).isEqualTo(at);
        }
        for (String exact : new String[]{"x".repeat(2000), new String(Character.toChars(0x1F680)).repeat(2000)}) assertThat(before.requestChanges(reviewer, " " + exact + " ", at.plusSeconds(1)).latestExplanation()).isEqualTo(exact);
    }
    @Test void approvedCannotBeChangedByAnyDomainMutation() {
        var approved = value(RequestState.PendingReview, "Preserved feedback").approve(reviewer, at.plusSeconds(1));
        assertThatIllegalStateException().isThrownBy(() -> approved.edit(new ChangeRequest.Edit(true, "Changed", false, null, false, null), at.plusSeconds(2)));
        assertThatIllegalStateException().isThrownBy(() -> approved.submit(at.plusSeconds(2)));
        assertThatIllegalStateException().isThrownBy(() -> approved.approve(reviewer, at.plusSeconds(2)));
        assertThatIllegalStateException().isThrownBy(() -> approved.requestChanges(reviewer, "Changed feedback", at.plusSeconds(2)));
        assertThat(approved.latestExplanation()).isEqualTo("Preserved feedback"); assertThat(approved.state()).isEqualTo(RequestState.Approved); assertThat(approved.updatedAt()).isEqualTo(at.plusSeconds(1));
    }
}
