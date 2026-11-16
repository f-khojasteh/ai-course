package com.changeflow.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Independent immutable request values and lifecycle invariants. */
public record ChangeRequest(UUID id, String title, String description, String reason,
                            UUID requesterId, RequestState state, String latestExplanation,
                            Instant createdAt, Instant updatedAt) {
    public ChangeRequest {
        Objects.requireNonNull(id); Objects.requireNonNull(requesterId); Objects.requireNonNull(state);
        Objects.requireNonNull(createdAt); Objects.requireNonNull(updatedAt);
        stored(title, ContentRules.title(title));
        stored(description, ContentRules.description(description));
        stored(reason, ContentRules.reason(reason));
        if (latestExplanation != null) stored(latestExplanation, ContentRules.explanation(latestExplanation));
        if (state == RequestState.ChangesRequested && latestExplanation == null) {
            throw new IllegalArgumentException("ChangesRequested requires an explanation.");
        }
    }
    /** Presence is distinct from null: a supplied null is invalid content. */
    public record Edit(boolean titleSupplied, String title, boolean descriptionSupplied, String description,
                       boolean reasonSupplied, String reason) { }

    public static ChangeRequest draft(UUID requesterId, String title, String description, String reason, Instant now) {
        return new ChangeRequest(UUID.randomUUID(), ContentRules.title(title), ContentRules.description(description),
                ContentRules.reason(reason), requesterId, RequestState.Draft, null, now, now);
    }

    public ChangeRequest edit(Edit edit, Instant now) {
        requireOwnerEditableState();
        Objects.requireNonNull(edit); Objects.requireNonNull(now);
        // Validate every supplied value before constructing a changed aggregate.
        String nextTitle = edit.titleSupplied() ? ContentRules.title(edit.title()) : title;
        String nextDescription = edit.descriptionSupplied() ? ContentRules.description(edit.description()) : description;
        String nextReason = edit.reasonSupplied() ? ContentRules.reason(edit.reason()) : reason;
        if (title.equals(nextTitle) && description.equals(nextDescription) && reason.equals(nextReason)) return this;
        return new ChangeRequest(id, nextTitle, nextDescription, nextReason, requesterId, state, latestExplanation, createdAt, now);
    }

    public ChangeRequest submit(Instant now) {
        requireOwnerEditableState();
        // Submission validates, but never repairs or rewrites, the stored proposal.
        stored(title, ContentRules.title(title));
        stored(description, ContentRules.description(description));
        stored(reason, ContentRules.reason(reason));
        return new ChangeRequest(id, title, description, reason, requesterId, RequestState.PendingReview, latestExplanation, createdAt, now);
    }

    public ChangeRequest approve(UUID reviewerId, Instant now) {
        requireIndependentReview(reviewerId);
        return new ChangeRequest(id, title, description, reason, requesterId, RequestState.Approved, latestExplanation, createdAt, now);
    }

    public ChangeRequest requestChanges(UUID reviewerId, String explanation, Instant now) {
        requireIndependentReview(reviewerId);
        String feedback = ContentRules.explanation(explanation);
        return new ChangeRequest(id, title, description, reason, requesterId, RequestState.ChangesRequested, feedback, createdAt, now);
    }

    private void requireIndependentReview(UUID reviewerId) {
        Objects.requireNonNull(reviewerId);
        if (requesterId.equals(reviewerId)) throw new IllegalArgumentException("An independent reviewer is required.");
        if (state != RequestState.PendingReview) throw new IllegalStateException("The operation is not allowed in the current state.");
    }

    private void requireOwnerEditableState() {
        if (state != RequestState.Draft && state != RequestState.ChangesRequested) {
            throw new IllegalStateException("The operation is not allowed in the current state.");
        }
    }
    private static void stored(String value, String normalized) {
        if (!value.equals(normalized)) throw new IllegalArgumentException("Stored text must already be normalized.");
    }
}
