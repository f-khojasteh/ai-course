package com.changeflow.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RevisionRulesTest {
    private final UUID owner = UUID.randomUUID(), reviewer = UUID.randomUUID();
    private final Instant at = Instant.parse("2026-11-16T10:00:00.123456789Z");
    private ChangeRequest returned() { return new ChangeRequest(UUID.randomUUID(), "Title", "Details\nwith  spaces", "Reason", owner, RequestState.ChangesRequested, "Latest feedback", at, at); }
    private ChangeRequest.Edit title(String text) { return new ChangeRequest.Edit(true, text, false, null, false, null); }
    @Test void returnedPartialRevisionPreservesIdentityOwnershipStateFeedbackAndOmission() {
        var before = returned();
        var after = assertDoesNotThrow(() -> before.edit(title("  Revised  title\nline \u2003"), at.plusSeconds(1)));
        assertThat(after).isEqualTo(new ChangeRequest(before.id(), "Revised  title\nline", before.description(), before.reason(), owner, RequestState.ChangesRequested, before.latestExplanation(), at, at.plusSeconds(1)));
        assertThat(before.title()).isEqualTo("Title"); assertThat(before.updatedAt()).isEqualTo(at);
    }
    @Test void returnedCompleteRevisionValidatesAndChangesProposalTogether() {
        var before = returned();
        var after = assertDoesNotThrow(() -> before.edit(new ChangeRequest.Edit(true, " New title ", true, " New details ", true, " New reason "), at.plusSeconds(1)));
        assertThat(after).isEqualTo(new ChangeRequest(before.id(), "New title", "New details", "New reason", owner, RequestState.ChangesRequested, "Latest feedback", at, at.plusSeconds(1)));
    }
    @Test void returnedNoOpsLeaveTheEntireValueUnchanged() {
        var before = returned();
        assertThat(assertDoesNotThrow(() -> before.edit(new ChangeRequest.Edit(false, null, false, null, false, null), at.plusSeconds(1)))).isEqualTo(before);
        assertThat(assertDoesNotThrow(() -> before.edit(title(" \tTitle \n"), at.plusSeconds(1)))).isEqualTo(before);
    }
    @Test void invalidReturnedEditsCannotPartiallyAlterProposalOrFeedback() {
        var before = returned();
        for (String invalid : new String[]{null, "", " \t\n\u2003", "x".repeat(201)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> before.edit(title(invalid), at.plusSeconds(1)));
            assertThat(before.title()).isEqualTo("Title"); assertThat(before.latestExplanation()).isEqualTo("Latest feedback"); assertThat(before.updatedAt()).isEqualTo(at);
        }
        assertThatIllegalArgumentException().isThrownBy(() -> before.edit(new ChangeRequest.Edit(true, "Valid", true, null, false, null), at.plusSeconds(1)));
        assertThatIllegalArgumentException().isThrownBy(() -> before.edit(new ChangeRequest.Edit(true, "Valid", false, null, true, "x".repeat(2001)), at.plusSeconds(1)));
    }
    @Test void resubmissionAndLaterApprovalRetainFeedbackAndOnlyNewRequestChangesReplacesIt() {
        var before = returned();
        var revised = assertDoesNotThrow(() -> before.edit(title("Revised"), at.plusSeconds(1)));
        var resubmitted = assertDoesNotThrow(() -> revised.submit(at.plusSeconds(2)));
        assertThat(resubmitted).isEqualTo(new ChangeRequest(before.id(), "Revised", before.description(), before.reason(), owner, RequestState.PendingReview, "Latest feedback", at, at.plusSeconds(2)));
        var approved = resubmitted.approve(reviewer, at.plusSeconds(3));
        assertThat(approved).isEqualTo(new ChangeRequest(before.id(), "Revised", before.description(), before.reason(), owner, RequestState.Approved, "Latest feedback", at, at.plusSeconds(3)));
        var returnedAgain = resubmitted.requestChanges(reviewer, "  New feedback  ", at.plusSeconds(3));
        assertThat(returnedAgain).isEqualTo(new ChangeRequest(before.id(), "Revised", before.description(), before.reason(), owner, RequestState.ChangesRequested, "New feedback", at, at.plusSeconds(3)));
        assertThat(resubmitted.latestExplanation()).isEqualTo("Latest feedback");
    }
    @Test void pendingAndApprovedNeverPermitOwnerEditsOrSubmission() {
        for (var state : new RequestState[]{RequestState.PendingReview, RequestState.Approved}) {
            var before = new ChangeRequest(UUID.randomUUID(), "Title", "Details", "Reason", owner, state, "Feedback", at, at);
            assertThatIllegalStateException().isThrownBy(() -> before.edit(title("Changed"), at.plusSeconds(1)));
            assertThatIllegalStateException().isThrownBy(() -> before.edit(new ChangeRequest.Edit(false, null, false, null, false, null), at.plusSeconds(1)));
            assertThatIllegalStateException().isThrownBy(() -> before.submit(at.plusSeconds(1)));
            assertThat(before.state()).isEqualTo(state); assertThat(before.latestExplanation()).isEqualTo("Feedback"); assertThat(before.updatedAt()).isEqualTo(at);
        }
    }
}
