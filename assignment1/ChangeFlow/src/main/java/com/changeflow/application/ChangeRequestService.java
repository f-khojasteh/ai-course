package com.changeflow.application;

import com.changeflow.application.port.*;
import com.changeflow.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import static com.changeflow.application.ApplicationFailure.Code.*;

/** Owns authorization, revision ordering, and each use case's atomic boundary. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10, rollbackFor = Exception.class)
public class ChangeRequestService {
    private final ChangeRequestStore requests;
    private final CurrentAccessGuard access;
    private final ReviewDecisionLog decisions;
    private final Clock clock;

    public ChangeRequestService(ChangeRequestStore requests, CurrentAccessGuard access, ReviewDecisionLog decisions, Clock clock) {
        this.requests = requests; this.access = access; this.decisions = decisions; this.clock = clock;
    }
    public record Page(List<RequestSnapshot> items, int offset, int limit, boolean hasMore) {
        public Page { items = List.copyOf(items); }
    }

    public RequestSnapshot create(AuthenticatedCaller caller, String title, String description, String reason) {
        var current = access.lockAndVerify(caller);
        if (!current.requester()) throw new ApplicationFailure(PERMISSION_DENIED);
        return requests.insert(ChangeRequest.draft(caller.userId(), title, description, reason, clock.instant()));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED, timeout = 10, rollbackFor = Exception.class)
    public Page list(AuthenticatedCaller caller, int offset, int limit) {
        var current = access.current(caller);
        if (offset < 0 || limit < 1 || limit > 100) throw new ApplicationFailure(INVALID_PARAMETER);
        var rows = requests.listVisible(caller.userId(), current.requester(), current.reviewer(), offset, limit + 1);
        return new Page(rows.subList(0, Math.min(limit, rows.size())), offset, limit, rows.size() > limit);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED, timeout = 10, rollbackFor = Exception.class)
    public RequestSnapshot read(AuthenticatedCaller caller, UUID id) {
        var current = access.current(caller);
        return visible(current, id);
    }

    /** Revise an owned Draft or ChangesRequested proposal; review feedback is preserved by Domain. */
    public RequestSnapshot edit(AuthenticatedCaller caller, UUID id, long expectedRevision, ChangeRequest.Edit edit) {
        var observed = owned(caller, id, expectedRevision);
        ChangeRequest changed;
        try { changed = observed.request().edit(edit, clock.instant()); }
        catch (IllegalStateException invalid) { throw new ApplicationFailure(INVALID_STATE); }
        if (changed.equals(observed.request())) return requests.verifyUnchanged(id, expectedRevision);
        return requests.update(changed, expectedRevision);
    }

    /** Submit or resubmit through the same current-access, ownership and revision boundary. */
    public RequestSnapshot submit(AuthenticatedCaller caller, UUID id, long expectedRevision) {
        var observed = owned(caller, id, expectedRevision);
        ChangeRequest changed;
        try { changed = observed.request().submit(clock.instant()); }
        catch (IllegalStateException invalid) { throw new ApplicationFailure(INVALID_STATE); }
        return requests.update(changed, expectedRevision);
    }

    public RequestSnapshot approve(AuthenticatedCaller caller, UUID id, long expectedRevision) {
        var observed = reviewable(caller, id, expectedRevision);
        Instant now = clock.instant();
        ChangeRequest changed;
        try { changed = observed.request().approve(caller.userId(), now); }
        catch (IllegalStateException invalid) { throw new ApplicationFailure(INVALID_STATE); }
        return recordDecision(changed, expectedRevision, caller.userId(), ReviewAction.Approve, now);
    }

    public RequestSnapshot requestChanges(AuthenticatedCaller caller, UUID id, long expectedRevision, String explanation) {
        var observed = reviewable(caller, id, expectedRevision);
        Instant now = clock.instant();
        ChangeRequest changed;
        try { changed = observed.request().requestChanges(caller.userId(), explanation, now); }
        catch (IllegalStateException invalid) { throw new ApplicationFailure(INVALID_STATE); }
        return recordDecision(changed, expectedRevision, caller.userId(), ReviewAction.RequestChanges, now);
    }

    private RequestSnapshot reviewable(AuthenticatedCaller caller, UUID id, long expectedRevision) {
        var current = access.lockAndVerify(caller);
        var observed = visible(current, id);
        if (!current.reviewer() || observed.request().requesterId().equals(caller.userId())) throw new ApplicationFailure(PERMISSION_DENIED);
        if (observed.revision() != expectedRevision) throw new ApplicationFailure(STALE_VERSION);
        return observed;
    }

    private RequestSnapshot recordDecision(ChangeRequest changed, long expectedRevision, UUID reviewerId, ReviewAction action, Instant now) {
        var result = requests.update(changed, expectedRevision); // Flush captures Hibernate's generated revision.
        decisions.append(changed.id(), reviewerId, action, now, result.revision()); // Flush in the same transaction; no feedback text.
        return result; // API receives this snapshot only after the public transaction proxy commits.
    }

    private RequestSnapshot owned(AuthenticatedCaller caller, UUID id, long expectedRevision) {
        // Infrastructure locks and refreshes Credential, then UserAccess, in this transaction.
        var current = access.lockAndVerify(caller);
        var observed = visible(current, id);
        if (!current.requester() || !observed.request().requesterId().equals(caller.userId())) throw new ApplicationFailure(PERMISSION_DENIED);
        if (observed.revision() != expectedRevision) throw new ApplicationFailure(STALE_VERSION);
        return observed;
    }

    private RequestSnapshot visible(UserAccessStore.Access current, UUID id) {
        var observed = requests.findVisible(id, current.userId(), current.requester(), current.reviewer())
                .orElseThrow(() -> new ApplicationFailure(NOT_FOUND));
        var value = observed.request();
        boolean visible = (current.requester() && value.requesterId().equals(current.userId()))
                || (current.reviewer() && value.state() != RequestState.Draft);
        if (!visible) throw new ApplicationFailure(NOT_FOUND);
        return observed;
    }
}
