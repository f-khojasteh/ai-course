package com.changeflow.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class OwnerLifecycleTest {
    private final UUID owner = UUID.randomUUID();
    private final Instant at = Instant.parse("2026-11-16T10:00:00.123456789Z");
    private ChangeRequest draft() { return ChangeRequest.draft(owner, " Title ", " Details\nwith  spaces ", " Reason ", at); }
    private ChangeRequest.Edit title(String text) { return new ChangeRequest.Edit(true, text, false, null, false, null); }
    @Test void createNormalizesAndEstablishesServerIdentityAndDraft() {
        var value = draft();
        assertThat(value.id()).isNotNull();
        assertThat(value.id()).isNotEqualTo(draft().id());
        assertThat(value.requesterId()).isEqualTo(owner);
        assertThat(value.state()).isEqualTo(RequestState.Draft);
        assertThat(value.title()).isEqualTo("Title");
        assertThat(value.description()).isEqualTo("Details\nwith  spaces");
        assertThat(value.reason()).isEqualTo("Reason");
        assertThat(value.latestExplanation()).isNull();
        assertThat(value.createdAt()).isEqualTo(at); assertThat(value.updatedAt()).isEqualTo(at);
    }
    @Test void partialAndCompleteEditsKeepIdentityAndLeaveOriginalUntouched() {
        var value = draft();
        var next = value.edit(title(" New  title\nline "), at.plusSeconds(1));
        assertThat(next).isEqualTo(new ChangeRequest(value.id(), "New  title\nline", value.description(), value.reason(), owner, RequestState.Draft, null, at, at.plusSeconds(1)));
        var all = next.edit(new ChangeRequest.Edit(true, "All", true, "Details", true, "Reason"), at.plusSeconds(2));
        assertThat(all).isEqualTo(new ChangeRequest(value.id(), "All", "Details", "Reason", owner, RequestState.Draft, null, at, at.plusSeconds(2)));
        assertThat(value.title()).isEqualTo("Title");
    }
    @Test void emptyAndNormalizedIdenticalEditsAreUnchanged() {
        var value = draft();
        assertThat(value.edit(new ChangeRequest.Edit(false, null, false, null, false, null), at.plusSeconds(1))).isEqualTo(value);
        assertThat(value.edit(title(" \tTitle \n"), at.plusSeconds(1))).isEqualTo(value);
    }
    @Test void invalidSuppliedContentRejectsWithoutPartialMutation() {
        var value = draft();
        for (String bad : new String[]{null, "", " \n\u2003", "x".repeat(201)}) assertThatIllegalArgumentException().isThrownBy(() -> value.edit(title(bad), at.plusSeconds(1)));
        assertThatIllegalArgumentException().isThrownBy(() -> value.edit(new ChangeRequest.Edit(true, "Valid", true, null, false, null), at));
        assertThatIllegalArgumentException().isThrownBy(() -> ChangeRequest.draft(owner, "Valid", " ", "Reason", at));
        assertThatIllegalArgumentException().isThrownBy(() -> ChangeRequest.draft(owner, null, "Details", "Reason", at));
        assertThat(value.title()).isEqualTo("Title"); assertThat(value.updatedAt()).isEqualTo(at);
    }
    @Test void submissionPreservesAllProposalAndIdentityFields() {
        var value = draft();
        var submitted = value.submit(at.plusSeconds(1));
        assertThat(submitted).isEqualTo(new ChangeRequest(value.id(), value.title(), value.description(), value.reason(), owner, RequestState.PendingReview, null, at, at.plusSeconds(1)));
        assertThat(value.state()).isEqualTo(RequestState.Draft);
        assertThatIllegalStateException().isThrownBy(() -> submitted.submit(at.plusSeconds(2)));
        assertThatIllegalStateException().isThrownBy(() -> submitted.edit(title("Changed"), at.plusSeconds(2)));
    }
    @Test void approvedRejectsEveryOwnerMutationIncludingNoOp() {
        var value = new ChangeRequest(UUID.randomUUID(), "Title", "Details", "Reason", owner, RequestState.Approved, "Existing feedback", at, at);
        assertThatIllegalStateException().isThrownBy(() -> value.edit(title("Changed"), at.plusSeconds(1)));
        assertThatIllegalStateException().isThrownBy(() -> value.edit(new ChangeRequest.Edit(false, null, false, null, false, null), at));
        assertThatIllegalStateException().isThrownBy(() -> value.submit(at.plusSeconds(1)));
        assertThat(value.state()).isEqualTo(RequestState.Approved); assertThat(value.latestExplanation()).isEqualTo("Existing feedback"); assertThat(value.updatedAt()).isEqualTo(at);
    }
}
