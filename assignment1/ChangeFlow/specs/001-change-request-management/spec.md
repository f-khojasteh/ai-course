# Feature Specification: Change-Request Management

**Feature Branch**: Not created; no branch-creation hook is configured.

**Feature Directory**: `specs/001-change-request-management`

**Created**: 2026-11-16

**Status**: Implemented through T083; current correction verification recorded in
[validation.md](validation.md#phase-8-convergence-corrections-evidence).
Follow-up Converge and project-owner final acceptance remain separate workflow steps.

**Input**: Initial change-request management with Requester and Reviewer roles, owned
Draft preparation and submission, review, revision, immutable approval, authorization,
and safe concurrent operations. Product decisions are resolved only through explicit
user answers; unanswered decisions remain open.

**Governance**: [ChangeFlow constitution](../../.specify/memory/constitution.md).
This document defines business behavior; technical choices belong in Plan/Research.

## Clarifications

### Session 2026-11-16

- Q: Who should be able to list and read ChangeFlow requests, including Drafts? (OPEN-001)
  → A: Option A. Requesters see their own requests in every state. Reviewers see everyone's
  PendingReview, ChangesRequested, and Approved requests. Dual-role users get both kinds
  of access. Unauthenticated users have no access. Lists omit inaccessible requests;
  opening an inaccessible request gives the same "not found" result as a missing request.
- Q: May a user who holds both roles request changes on their own PendingReview request?
  (OPEN-002) → A: Option A. Reject self-request-changes, including for dual-role users;
  stored business data remains unchanged.
- Q: Should users be able to delete requests in the initial version of ChangeFlow?
  (OPEN-003) → A: Option A. No deletion in the initial version, regardless of role or state.
- Q: Should Requesters be able to cancel or withdraw their own requests in the initial
  version? (OPEN-004) → A: Option A. No cancellation or withdrawal in the initial version.
- Q: What access should a user retain after their Requester or Reviewer role is removed?
  (OPEN-005) → A: Option A. The removed role grants no further access, even to owned
  requests. Any remaining role still grants its normal access. Removal must be respected
  if it takes effect before a write completes; only a write still permitted under the
  remaining access may complete. Previously completed writes remain valid.

Second clarification pass (new invocation on 2026-11-16; up to five questions):

- Q: If two Reviewers act on the same PendingReview request at the same time, what should
  ChangeFlow report? (OPEN-008) → A: Option A. The first valid decision to complete wins.
  Every competing decision reports a conflict, even when both Reviewers choose the same
  action. A conflicting attempt leaves the winning decision and stored content unchanged.
- Q: How should ChangeFlow handle a repeated action when the caller retries because they
  missed the original confirmation? (OPEN-006) → A: Option A. Each valid creation attempt
  creates a new Draft. Repeated submission or review is evaluated against current data
  and rejected if no longer allowed. There is no special retry tracking and previous
  success is not replayed. Current authorization and concurrency rules still apply.
- Q: Which content policy should ChangeFlow use for a request's title, description, and
  reason? (OPEN-007) → A: Option A. All three fields must contain nonblank text from
  creation onward; edits cannot make them blank, and submission requires complete
  content. Partial edits preserve omitted fields; supplied null values are rejected.
  Trim leading/trailing whitespace and preserve internal whitespace. After trimming,
  maximum lengths are title 200, description 5,000, and reason 2,000 characters. Reject
  excessive text without truncation and reject invalid operations in full without
  changing stored data.
- Q: Should ChangeFlow reject a write when its submitted content includes requester
  identity, roles, or a request state that the system controls? (OPEN-009) → A: Option A.
  Reject the whole write whenever these system-controlled fields are included, even
  when their values match the correct values. Create nothing and leave existing stored
  business data unchanged. The selected permitted action still controls the transition.
- Q: Should a Reviewer have to explain what needs changing when returning a request to
  its owner? (OPEN-010) → A: Option A. An explanation is required whenever a Reviewer
  requests changes. It must be nonblank after trimming, at most 2,000 characters, with
  internal whitespace preserved. Save the latest explanation with the decision and
  expose it to everyone allowed to read the request. Preserve it through revisions,
  resubmission, and approval; a later successful request-changes decision replaces it.
  Keep no full feedback history initially. Invalid input rejects the whole operation
  without changing stored data.

**Authentication transport refinement - 2026-11-16 (T001):** The clarification answers
above remain a historical record of the user's actual choices. The later explicit
planning instruction and Phase 1 implementation request refine only authentication
failure transport: missing/malformed/invalid/revoked credentials receive uniform JSON
401 with WWW-Authenticate: Bearer before lookup; authenticated missing/inaccessible
items remain identical 404. FR-015, US3-AS11, US3-AS14 and SC-007 reflect that refinement.
All other recorded product decisions, ownership rules, and stable identifiers are unchanged.

## User Scenarios & Testing *(mandatory)*

For every rejected operation below, verify both the reported rejection and stored
business data: no request is created, deleted, or modified by that operation. Compare
all stored request fields, including requester identity and state. During concurrent
activity, preserve any changes legitimately made by a successful competing operation;
rejection must not restore an earlier snapshot over those changes (FR-014).
Scenario IDs are stable within this feature, as are FR and SC IDs.

### User Story 1 - Prepare and Submit an Owned Request (Priority: P1)

As a Requester, I want to prepare my own change request and submit it for review so
that a Reviewer can decide on a specific proposed change.

**Why this priority**: A reviewable proposal is the entry point to the workflow.

**Independent Test**: An authenticated Requester creates, edits, and submits a request;
inspect its stored content, owner, and state after each operation. This journey provides
value without implementing review decisions.

**Acceptance Scenarios**:

1. **US1-AS01** (FR-001-003, FR-012, FR-022): **Given** an authenticated Requester and title,
   description, and reason satisfying FR-022, **When** they create a
   request, **Then** one identifiable request is stored in Draft with those values and
   the authenticated user as requester, and creation success is reported.
2. **US1-AS02** (FR-004, FR-012, FR-022): **Given** that Requester owns a Draft,
   **When** they edit its title, description, and reason with valid content,
   **Then** the supplied changes are stored together,
   its requester identity and Draft state are preserved, and success is reported.
3. **US1-AS03** (FR-005, FR-012, FR-022): **Given** that Requester owns a Draft with valid
   content, **When** they submit it, **Then** its state becomes PendingReview,
   its content and requester identity are
   preserved, and success is reported.
4. **US1-AS04** (FR-010, FR-014): **Given** an unauthenticated user or an authenticated user
   without creation permission, **When** they attempt creation, **Then** the operation
   is rejected and no request is created, even if supplied roles claim Requester access.
5. **US1-AS05** (FR-004-005, FR-010, FR-014): **Given** a Requester who does not own another
   user's Draft, **When** they attempt to edit or submit it, **Then** each attempt is
   rejected and the stored request remains unchanged.
6. **US1-AS06** (FR-003, FR-010, FR-014, FR-023): **Given** an authenticated Requester, **When** they
   attempt to assign a different requester identity or a non-Draft initial state during
   creation, **Then** the entire operation is rejected and no request is created.
   Supplied roles MUST NOT confer authority; including them also rejects the operation.
7. **US1-AS07** (FR-003, FR-012, FR-021): **Given** a Requester successfully created a Draft
   but did not receive its confirmation, **When** they repeat the otherwise valid
   creation with identical content, **Then** a second, separately identifiable Draft is
   created and its creation succeeds. The first Draft remains unchanged; identical
   content is not treated as evidence of a duplicate attempt.
8. **US1-AS08** (FR-005, FR-011, FR-014, FR-021): **Given** an owned Draft or ChangesRequested
   request has already been submitted and is still PendingReview, **When** its Requester
   repeats submission, **Then** the attempt is rejected under the current-state rule,
   previous success is not replayed, and stored business data remains unchanged.
9. **US1-AS09** (FR-003, FR-022): **Given** an authenticated Requester supplies valid text
   with leading/trailing whitespace and internal spaces or line breaks, **When** they
   create a request, **Then** outer whitespace is removed from each stored content field,
   internal whitespace is preserved, and a Draft is successfully created.
10. **US1-AS10** (FR-004, FR-022): **Given** a Requester owns a valid Draft or
    ChangesRequested request, **When** they edit only its title with valid text,
    **Then** the normalized title is stored, omitted description and reason remain
    unchanged, and request identity, owner, and state remain unchanged.
11. **US1-AS11** (FR-003, FR-014, FR-022): **Given** an otherwise authorized creation,
    **When** any title, description, or reason is omitted, null, empty, whitespace-only,
    or over its allowed length after trimming, **Then** creation is rejected in full
    and no request is stored. Evaluate each invalid condition for each field.
12. **US1-AS12** (FR-004, FR-014, FR-022): **Given** a Requester owns a valid Draft or
    ChangesRequested request, **When** an edit supplies any null, empty, whitespace-only,
    or excessively long content field, even alongside valid changes to other fields,
    **Then** the whole edit is rejected and all stored fields and state remain unchanged.
    Omitted fields alone are not invalid, because partial edits preserve them.
13. **US1-AS13** (FR-003-004, FR-014, FR-022): **Given** otherwise valid creation or editing,
    **When** the normalized title has 200 characters, description 5,000, or reason 2,000,
    **Then** that boundary value is accepted. **Given** the same operation with any one
    field one character above its limit, **When** it is evaluated, **Then** the operation
    is rejected without truncation, new request creation, or changes to an existing
    request. Evaluate each field independently, keeping other fields valid.
14. **US1-AS14** (FR-005, FR-014, FR-022): **Given** an owned Draft or ChangesRequested
    request whose stored content fails FR-022, **When** the otherwise authorized owner
    submits it, **Then** submission is rejected and all stored fields and state remain
    unchanged. Submission must not repair, truncate, or silently rewrite the content.
15. **US1-AS15** (FR-003, FR-014, FR-023): **Given** an otherwise valid Draft creation,
    **When** its submitted content includes requester identity, roles, or state, even
    matching the authenticated requester, actual roles, or Draft state, **Then** the
    entire creation is rejected and no request is stored. Evaluate each field separately.
16. **US1-AS16** (FR-004, FR-014, FR-023): **Given** a Requester owns an editable request,
    **When** an edit contains valid title, description, or reason changes together with
    any system-controlled requester identity, roles, or state field, **Then** the whole
    edit is rejected and all stored business data remains unchanged, even when the
    system-controlled values match existing values.

---

### User Story 2 - Review a Submitted Request (Priority: P1)

As a Reviewer, I want to approve a submitted request or return it for changes so that
proposals progress through an explicit review decision.

**Why this priority**: Review decisions are the central control over submitted changes.

**Independent Test**: Start with a PendingReview request owned by another user; exercise
both decisions on separate requests and inspect stored state, unchanged proposal content,
and the latest explanation saved when changes are requested.

**Acceptance Scenarios**:

1. **US2-AS01** (FR-006, FR-012): **Given** an authenticated Reviewer with current approval
   permission and another user's PendingReview request, **When** they approve it,
   **Then** it becomes Approved, content and requester identity remain unchanged, and
   any existing latest explanation is preserved, and approval success is reported.
2. **US2-AS02** (FR-007, FR-012, FR-024): **Given** an authenticated Reviewer with current
   permission and another user's PendingReview request, **When** they request changes
   with a valid explanation, **Then** it becomes ChangesRequested and the normalized
   explanation is saved together with that decision. Proposal content and requester
   identity remain unchanged, and success is reported.
3. **US2-AS03** (FR-008, FR-014): **Given** a PendingReview request and its owner who holds
   both roles and approval permission, **When** that owner attempts approval, **Then**
   self-approval is rejected and the stored request remains unchanged.
4. **US2-AS04** (FR-006-007, FR-010, FR-014): **Given** an unauthenticated user or a user
   without the required review permission, **When** they approve or request changes,
   **Then** each operation is rejected and the stored request remains unchanged.
5. **US2-AS05** (FR-006-007, FR-011, FR-014): **Given** a Draft or ChangesRequested request,
   **When** a Reviewer attempts a new approval or request-changes decision, **Then** the
   decision is rejected because the request is not PendingReview and stored data remains
   unchanged. Repeating a previous command is evaluated afresh, without replaying prior
   success (FR-021).
6. **US2-AS06** (FR-001, FR-006-007): **Given** a user holding both roles with current
   review permissions and PendingReview requests owned by other users, **When** they
   approve one and request changes with a valid explanation on another,
   **Then** each action succeeds with the
   same stored outcomes as US2-AS01-02; holding Requester does not remove review access.
7. **US2-AS07** (FR-007-008, FR-014): **Given** a PendingReview request whose owner holds
   both roles and current request-changes permission, **When** the owner requests changes
   on that request, **Then** the operation is rejected and all stored request fields,
   including content, requester identity, and PendingReview state, remain unchanged.
8. **US2-AS08** (FR-013-014, FR-020): **Given** two otherwise eligible Reviewers approving
   the same PendingReview submission concurrently, **When** the first valid approval
   completes, **Then** exactly one approval succeeds and the request is Approved. The
   competing approval reports a conflict, and its rejection preserves the winning
   state, content, and requester identity.
9. **US2-AS09** (FR-013-014, FR-020): **Given** two otherwise eligible Reviewers requesting
   changes with different valid explanations on the same PendingReview submission
   concurrently, **When** the first valid
   decision completes, **Then** exactly one decision succeeds and the request becomes
   ChangesRequested with the winner's explanation. The competing decision reports a
   conflict; its explanation is not saved, and the winning state, proposal content,
   requester identity, and latest explanation remain unchanged.
10. **US2-AS10** (FR-013-014, FR-020): **Given** two otherwise eligible Reviewers acting on
    the same PendingReview submission, one approving and one requesting changes,
    **When** they compete, **Then** only the first valid decision to complete succeeds.
    The request becomes Approved if approval wins or ChangesRequested if requesting
    changes wins. The other decision reports a conflict and leaves the winning state,
    content, and requester identity unchanged. If requesting changes wins, its valid
    explanation is saved; if approval wins, the losing explanation is not saved and
    any previously stored explanation is preserved. Neither action has automatic priority.
11. **US2-AS11** (FR-011, FR-014, FR-021): **Given** a successful approval or request-changes
    decision left a request in Approved or ChangesRequested, **When** the Reviewer retries
    that review action while the request remains in that state, **Then** it is rejected
    because the current state is not PendingReview, previous success is not replayed,
    and all stored business data remains unchanged.
12. **US2-AS12** (FR-007, FR-014, FR-024): **Given** an otherwise authorized request-changes
    operation on another user's PendingReview request, **When** its explanation is
    omitted, null, empty, whitespace-only, or over 2,000 characters after trimming,
    **Then** the whole operation is rejected. State, proposal content, requester identity,
    and any prior explanation remain unchanged; excessive text is not truncated.
13. **US2-AS13** (FR-007, FR-012, FR-024): **Given** an otherwise authorized request-changes
    operation with an explanation containing outer whitespace and internal spaces or
    line breaks, **When** it completes, **Then** only outer whitespace is removed and
    the explanation is saved with ChangesRequested. A normalized nonblank explanation
    of exactly 2,000 characters succeeds; one of 2,001 is rejected under US2-AS12.
14. **US2-AS14** (FR-012, FR-014, FR-024): **Given** a valid request-changes operation,
    **When** a failure prevents completion, **Then** neither its state change nor its
    explanation is stored, no success is reported, and earlier stored data remains
    unchanged apart from any legitimate successful competing operation.
15. **US2-AS15** (FR-015-016, FR-024): **Given** a request with a saved explanation,
    **When** its authorized owner or an authorized Reviewer reads it, **Then** they can read the
    latest explanation under the existing visibility rules. **Given** a caller lacking
    request access, **When** they try to read its explanation, **Then** no explanation
    or existence information is disclosed and stored data remains unchanged.
16. **US2-AS16** (FR-007, FR-012, FR-024): **Given** a previously returned request has been
    revised and resubmitted with its earlier explanation preserved, **When** another
    valid request-changes decision succeeds with a new explanation, **Then** that new
    explanation replaces the earlier one atomically with ChangesRequested. No full
    feedback history is maintained in the initial version.

---

### User Story 3 - Preserve Trustworthy Decisions and Content (Priority: P1)

As a participant, I want unauthorized, conflicting, and failed actions to leave reliable
request data so that a successful operation never hides lost work or broken rules.

**Why this priority**: All lifecycle journeys depend on trustworthy ownership and state.

**Independent Test**: Use requests in each named state, unauthorized users, controlled
operation failures, and two participants acting from the same earlier request contents.
Observe results and stored business data independently of the other stories' delivery.

**Acceptance Scenarios**:

1. **US3-AS01** (FR-009, FR-014): **Given** an Approved request, **When** its owner, a
   Reviewer, or a dual-role user attempts an edit or a new state change, **Then** each
   attempt is rejected and all stored request fields remain unchanged. Approval cannot
   be undone by removing the request. Deletion is unavailable in every state (FR-017).
2. **US3-AS02** (FR-010, FR-014): **Given** a user without a required current permission,
   **When** they supply a role or another user's identity to edit, submit, or review,
   **Then** the claimed authority does not grant access; the operation is rejected and
   stored data remains unchanged. A removed role cannot authorize the operation, and
   ownership alone does not restore that role's permissions (FR-019).
3. **US3-AS03** (FR-013-014): **Given** two edits prepared from the same Draft contents and
   one edit has successfully stored newer content, **When** the other attempts to replace
   that content using its earlier view, **Then** it cannot silently overwrite the newer
   content; an unresolved conflict is reported to the caller, and the successful edit
   remains stored.
4. **US3-AS04** (FR-011, FR-013-014): **Given** an owner edit and submission competing on
   the same Draft, **When** they execute concurrently, **Then** every reported success
   is consistent with an allowed sequence of operations, submitted content is coherent,
   and any conflicting rejected operation leaves successful changes intact. The
   system must not report two incompatible successes or apply an edit to PendingReview.
5. **US3-AS05** (FR-012, FR-014): **Given** an otherwise permitted creation, edit, or state change,
   **When** a failure prevents completion, **Then** no part of that operation is stored,
   failure is reported, and there is no success report for an incomplete operation.
   Failed creation leaves no new request; failed changes preserve existing request data.
6. **US3-AS06** (FR-015, FR-014): **Given** an attempted edit, submission, or review of a
   request that does not exist, **When** the action is evaluated, **Then** it cannot
   succeed or create a request and stored data remains unchanged. If the caller cannot
   access a request, the result MUST NOT distinguish it from a missing request (FR-015).
7. **US3-AS07** (FR-010, FR-014): **Given** an unauthenticated caller and an existing request,
   **When** they attempt editing, submission, approval, or requesting changes, **Then**
   every attempt is rejected and stored business data remains unchanged.
8. **US3-AS08** (FR-016): **Given** an authenticated Requester and their own requests in
   each of the four states, **When** they list or read requests, **Then** they can see
   all their own requests, including Drafts, and cannot see other users' requests
   through Requester access alone. Stored business data remains unchanged.
9. **US3-AS09** (FR-016): **Given** an authenticated Reviewer and requests owned by other
   users in each state, **When** they list or read requests, **Then** PendingReview,
   ChangesRequested, and Approved requests are visible, and Drafts are omitted from
   lists and cannot be read. Stored business data remains unchanged.
10. **US3-AS10** (FR-001, FR-016): **Given** an authenticated dual-role user, **When** they
    list or read requests, **Then** they see all their own requests and everyone's
    non-Draft requests, while other users' Drafts remain inaccessible. Stored business
    data remains unchanged.
11. **US3-AS11** (FR-014-016): **Given** an authenticated caller who cannot access a
    request, **When** they try to read it, **Then** they receive the same 404 "not found"
    result as for a missing request, with no stored changes. **Given** missing, malformed,
    invalid, or revoked credentials, **When** any protected request is attempted,
    **Then** a uniform JSON 401 with WWW-Authenticate: Bearer is returned before resource
    lookup, whether or not the requested item exists. Unauthenticated listing returns
    no request data and no attempted operation changes stored business data.
12. **US3-AS12** (FR-014, FR-017): **Given** an existing request in Draft, PendingReview,
    ChangesRequested, or Approved and any caller, including its owner, a Reviewer, or
    a dual-role user, **When** they attempt deletion, **Then** deletion does not succeed
    and the request remains stored with all fields unchanged. Rejection must also
    respect the existence-disclosure rule in FR-015.
13. **US3-AS13** (FR-011, FR-014, FR-018): **Given** an existing request in any of the four
    states and any caller, including its owner, a Reviewer, or a dual-role user, **When**
    they attempt cancellation or withdrawal, **Then** the operation does not succeed
    and the request remains stored with all fields and its state unchanged. No request
    returns to Draft through withdrawal and no Cancelled state is introduced. Rejection
    must respect the existence-disclosure rule in FR-015.
14. **US3-AS14** (FR-010, FR-014-016, FR-019): **Given** an authenticated request owner
    with an active credential whose Requester role has been removed and who has no
    remaining role, **When** they try to read,
    edit, or submit their existing request or create another, **Then** no access or
    creation is granted and stored data remains unchanged. Read attempts give the
    same "not found" result as missing requests; listing returns no request data.
15. **US3-AS15** (FR-010, FR-012, FR-014, FR-019): **Given** an authorized edit,
    submission, approval, or request-changes operation has begun, **When** removal of
    the required role takes effect before the write completes and no remaining role
    permits the action, **Then** the write is rejected, no part of its business effect
    is stored, and any successful competing changes remain intact.
16. **US3-AS16** (FR-001, FR-016, FR-019): **Given** a dual-role user loses one role,
    **When** they act using the remaining role, **Then** only that role's normal access
    applies. A remaining Requester can manage their own Draft or ChangesRequested
    requests but cannot review others' requests. A remaining Reviewer can read all
    non-Draft requests and review others' PendingReview requests, but cannot create,
    edit, submit, or read owned Drafts through former Requester access. Ownership still
    forbids both forms of self-review. Rejected attempts leave stored data unchanged.
17. **US3-AS17** (FR-012, FR-019): **Given** an authorized write completed before role
    removal took effect, **When** that role is later removed, **Then** its completed
    business effect remains stored and is not undone because of the removal.
18. **US3-AS18** (FR-010, FR-014, FR-019, FR-021): **Given** a caller completed a write and
    subsequently lost the permission needed for that action, **When** they retry it,
    **Then** current authorization rejects the attempt rather than returning a prior
    success, and the retry leaves stored business data unchanged.
19. **US3-AS19** (FR-010-011, FR-014, FR-023): **Given** an otherwise authorized submission,
    approval, or request-changes operation in its allowed state, **When** its submitted
    content includes requester identity, roles, or system-controlled state, **Then**
    the operation is rejected in full with no stored changes. Evaluate each operation
    and field, including values matching the actual identity, roles, or state.

---

### User Story 4 - Revise and Resubmit Returned Work (Priority: P2)

As a Requester, I want to revise my returned request and submit it again so that review
can continue without losing the proposal's identity or ownership.

**Why this priority**: This completes the feedback loop after initial submission and review.

**Independent Test**: Start with an owned ChangesRequested request; revise, resubmit, and
inspect content and state. Repeat with a non-owner to verify rejection.

**Acceptance Scenarios**:

1. **US4-AS01** (FR-004, FR-012): **Given** an authenticated Requester with current edit
   permission who owns a ChangesRequested request, **When** they revise its content,
   **Then** changes are stored together, its identity and requester are preserved, its
   state remains ChangesRequested, and success is reported.
2. **US4-AS02** (FR-005, FR-012): **Given** that Requester owns a revised ChangesRequested
   request, **When** they submit it again, **Then** it becomes PendingReview with the
   revised content and original requester identity, and success is reported.
3. **US4-AS03** (FR-004-005, FR-010, FR-014): **Given** another user's ChangesRequested
   request, **When** a non-owner attempts revision or resubmission, **Then** each attempt
   is rejected and stored data remains unchanged, even if they hold both roles.
4. **US4-AS04** (FR-004, FR-011, FR-014): **Given** a PendingReview request, **When** its
   owner attempts to edit the content without a request-changes decision, **Then** the
   edit is rejected and stored data remains unchanged.
5. **US4-AS05** (FR-004-006, FR-024): **Given** an owned ChangesRequested request with a
   saved explanation, **When** its authorized owner revises and resubmits it and an
   independent authorized Reviewer subsequently approves it, **Then** the same latest
   explanation is preserved throughout the successful sequence, including in Approved.
   Owner content edits do not replace or clear the Reviewer's explanation.

### Edge Cases

- Dual-role ownership: neither approval nor requesting changes is permitted on one's
  own request, even with both roles and current review permissions. Rejection leaves
  stored business data unchanged (US2-AS03/07; OPEN-002 resolved).
- Changes between preparation and action: current permissions and state must be checked;
  removed roles cannot authorize writes that have not completed. Ownership alone does
  not preserve former role access; remaining roles keep their normal permissions.
  Earlier completed writes are not undone (FR-010-011/019, US3-AS14-17; OPEN-005 resolved).
- Two stale edits or edit-versus-submit: no silent loss, partial change, or incompatible
  successes (US3-AS03-04). No particular conflict-detection mechanism is prescribed.
- Competing approve/approve, request-changes/request-changes, and approve/request-changes:
  the first valid completed decision wins; every competing decision reports a conflict,
  including matching actions. The losing attempt preserves the winning state and content
  (FR-020, US2-AS08-10; OPEN-008 resolved). Retries use the current-data rules in FR-021.
- Repeated create, submit, approve, or request-changes after a retry or lost confirmation:
  each valid creation produces a distinct Draft; submission and review are evaluated
  against current permissions, data, and state without replaying previous success.
  Rejected repeats preserve stored data (FR-021, US1-AS07-08, US2-AS11, US3-AS18;
  OPEN-006 resolved).
- Missing creation fields, supplied nulls, blank text after trimming, and excessive
  lengths are rejected atomically. Partial edits preserve omitted fields; outer whitespace
  is trimmed, internal whitespace preserved, and no text is truncated (FR-022,
  US1-AS09-14; OPEN-007 resolved).
- Missing versus inaccessible requests: lists omit inaccessible requests. Authenticated
  callers opening one receive the same 404 "not found" result as for a missing request.
  Authentication failures receive uniform JSON 401 with WWW-Authenticate: Bearer before
  resource lookup. No denied operation changes stored data (FR-014-016, US3-AS06/08-11;
  OPEN-001 resolved, with the later explicit authentication refinement).
- Deletion is unavailable in the initial version, regardless of role or state; attempts
  leave stored data unchanged (FR-017, US3-AS12; OPEN-003 resolved). Cancellation and
  withdrawal are also unavailable and leave stored data unchanged (FR-018, US3-AS13;
  OPEN-004 resolved). A failed operation must not damage other requests.
- Requester identity cannot be transferred through content editing (FR-003-004).
- Supplying system-controlled requester identity, roles, or state in write content
  rejects the whole operation, including matching values and otherwise valid content;
  these fields are never silently ignored (FR-023, US1-AS06/15-16, US3-AS19;
  OPEN-009 resolved).
- Failures after some work has begun must not leave partial business changes (US3-AS05).
- A request-changes decision requires valid feedback and saves its state and explanation
  together. Invalid input, failure, or a losing concurrent decision leaves prior feedback
  unchanged. Revisions, resubmission, and approval preserve the latest explanation;
  only a later successful request-changes decision replaces it (FR-024, US2-AS09-16,
  US4-AS05; OPEN-010 resolved).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST recognize Requester and Reviewer as distinct roles and allow
  one user to hold both. A role combination MUST NOT bypass any ownership or state rule.
- **FR-002**: Each request MUST be individually identifiable and have a title, description,
  reason, requester identity, and exactly one state: Draft, PendingReview,
  ChangesRequested, or Approved. Content MUST satisfy FR-022 from creation onward.
- **FR-003**: An authenticated user with current Requester creation permission MUST be able
  to create a Draft owned by that authenticated user. Caller-supplied roles or requester
  identity MUST NOT confer authority or assign ownership to someone else. Submitted
  system-controlled fields MUST reject the entire write as defined in FR-023.
- **FR-004**: An authenticated Requester with current edit permission MUST be able to edit
  the title, description, and reason of their own Draft or ChangesRequested request.
  Editing MUST preserve request identity, requester identity, and state. Other states
  MUST NOT permit content edits. Partial edits and validation MUST follow FR-022.
- **FR-005**: An authenticated Requester with current submission permission MUST be able to
  submit their own Draft or ChangesRequested request to PendingReview. Submission MUST
  preserve content and requester identity and MUST reject content that fails FR-022.
- **FR-006**: An authenticated Reviewer with current approval permission MUST be able to
  approve another user's PendingReview request, changing only its state to Approved.
- **FR-007**: An authenticated Reviewer with current request-changes permission MUST be
  able to return another user's PendingReview request to ChangesRequested, preserving
  proposal content and requester identity and saving a required valid explanation with
  the decision as defined in FR-024. The request owner MUST NOT request changes on their
  own request.
- **FR-008**: The system MUST reject both self-approval and self-request-changes for every
  user, including a user holding both roles or current review permissions. These
  rejections MUST leave all stored business data unchanged.
- **FR-009**: An Approved request MUST remain immutable: no edits, subsequent state changes,
  or deletion may alter or remove it, regardless of the actor's roles or ownership.
- **FR-010**: Every write MUST use authenticated identity and current permissions and MUST
  check the applicable ownership rule. Unauthenticated or unauthorized writes MUST be
  rejected. Supplied roles or requester identities MUST NOT replace these checks.
  Removed roles MUST NOT grant access, including to owned requests; current permissions
  MUST remain sufficient for the write through completion as defined in FR-019.
- **FR-011**: Every state-changing operation MUST validate current state. New transitions
  defined here are Draft to PendingReview, ChangesRequested to PendingReview,
  PendingReview to Approved, and PendingReview to ChangesRequested. Other new transitions
  MUST be rejected. Deletion, cancellation, and withdrawal are unavailable (FR-017-018).
  Repeated commands MUST be evaluated against current data as defined in FR-021.
- **FR-012**: Every successful business operation MUST store its full business effect
  together or none of it. Success MUST be reported only for a completed operation.
  Failure MUST leave no partial effect from that operation.
- **FR-013**: Concurrent actions MUST NOT silently overwrite newer stored content, produce
  a state inconsistent with the allowed lifecycle, or bypass authorization or immutability.
  Conflicting actions that cannot complete consistently MUST report a conflict rather
  than report an incompatible success. Competing review decisions MUST follow FR-020.
- **FR-014**: Every rejected or failed operation MUST report a non-success outcome and
  leave stored business data unchanged by that operation, including fields, identity,
  state, and existence. Rejection MUST NOT undo a successful concurrent operation.
- **FR-015**: Editing, submitting, or reviewing a nonexistent request MUST NOT succeed,
  create a request, or modify existing data. Authenticated callers opening an inaccessible
  request MUST receive the same 404 "not found" result as for a missing request. Missing,
  malformed, invalid, or revoked credentials MUST produce uniform JSON 401 with
  WWW-Authenticate: Bearer before protected-resource lookup, regardless of existence.
  Other rejected operations MUST NOT disclose whether an inaccessible request exists.
- **FR-016**: Authenticated Requesters MUST be able to list and read their own requests
  in every state. Authenticated Reviewers MUST be able to list and read everyone's
  PendingReview, ChangesRequested, and Approved requests. A dual-role user's access
  MUST be the union of these permissions: their own Drafts are visible, but other
  users' Drafts are not. Lists MUST omit inaccessible requests. Unauthenticated users
  MUST have no request access; unauthenticated listing MUST be rejected without
  returning request data. Reading or listing MUST NOT change stored business data.
  Access MUST use the user's current roles, applying removals as defined in FR-019.
- **FR-017**: The initial version MUST NOT provide request deletion. No user, regardless
  of roles, permissions, ownership, or request state, may delete a request. Any deletion
  attempt MUST fail without removing the request or changing stored business data.
- **FR-018**: The initial version MUST NOT provide cancellation or withdrawal. Such an
  attempt MUST fail for every caller and request state without changing stored business
  data. Requests MUST NOT return to Draft through withdrawal, and the initial lifecycle
  MUST NOT include a Cancelled state.
- **FR-019**: Removing a role MUST remove all access granted by that role, including
  access to owned requests. Any remaining role MUST retain only its normal permissions;
  ownership MUST NOT substitute for a removed role. If removal takes effect before a
  write's business changes are fully stored, the operation MUST respect that removal
  and MUST fail without partial effects unless the remaining permissions still allow
  it. A write fully completed before removal MUST remain valid and MUST NOT be undone
  because of the removal. Request identity and ownership MUST NOT change upon removal.
- **FR-020**: For otherwise eligible review decisions competing on the same PendingReview
  submission, exactly one decision MUST succeed: the first valid decision to complete.
  Every competing decision MUST report a conflict, including a matching approve or
  request-changes decision. The system MUST NOT give either action automatic priority.
  A conflicting attempt MUST preserve the winner's stored state, content, and requester identity.
  This rule MUST NOT bypass authorization, ownership, or current-state checks.
- **FR-021**: The initial version MUST NOT provide special duplicate-command or retry
  tracking or replay previous success. Each otherwise valid creation attempt MUST
  create a distinct Draft, even when the caller and content match an earlier attempt.
  Repeated submission or review MUST be evaluated afresh against current identity,
  permissions, ownership, data, and state; if no longer allowed, it MUST be rejected
  with no stored changes. Existing concurrency safeguards and FR-020 MUST still apply.
- **FR-022**: Creation MUST supply title, description, and reason. Each MUST contain
  nonblank text from creation onward, including after every edit and at submission.
  Supplied null values MUST be rejected. Creating or editing MUST trim leading/trailing
  whitespace from supplied text while preserving internal whitespace. After trimming,
  maximum lengths MUST be 200 characters for title, 5,000 for description, and 2,000 for
  reason, inclusive. Excessive text MUST be rejected without truncation. Partial edits
  MUST preserve omitted fields unchanged. Any invalid operation MUST be rejected in full
  without storing a new request or changing any existing stored business data.
  Submission MUST validate stored content without rewriting it.
- **FR-023**: Every write MUST reject submitted content containing requester identity,
  roles, or system-controlled request state, even if those values are correct. The
  entire operation MUST fail without creating a request or changing stored business
  data. Such fields MUST NOT be silently ignored. Trusted authentication information
  remains the authority for identity and current roles; the chosen business action
  determines a permitted transition rather than accepting a caller-selected state field.
- **FR-024**: Every request-changes decision MUST supply an explanation that is nonblank
  after trimming leading/trailing whitespace and no more than 2,000 characters,
  inclusive. Internal whitespace MUST be preserved. Missing, null, blank, or excessive
  explanations MUST reject the whole operation without truncation or stored changes.
  A successful decision MUST save the normalized explanation atomically with the state
  change as the request's latest explanation. It MUST be readable by everyone permitted
  to read the request, subject to FR-015-016. Revisions, resubmission, and approval MUST
  preserve it; owner edits MUST NOT replace or clear it. A later successful
  request-changes decision MUST replace it; other operations MUST preserve it.
  The initial version MUST NOT maintain a
  full feedback history. Before the first successful request-changes decision, there
  is no saved explanation. Approved-request immutability continues to apply.

### Confirmed Lifecycle

| Action | Actor and ownership | Starting state | Result |
| --- | --- | --- | --- |
| Create | Requester; self becomes owner | No request | Draft |
| Edit | Requester; owner | Draft | Draft with changed content |
| Submit | Requester; owner | Draft | PendingReview |
| Approve | Reviewer; not owner | PendingReview | Approved |
| Request changes | Reviewer; not owner | PendingReview | ChangesRequested |
| Revise | Requester; owner | ChangesRequested | ChangesRequested with changed content |
| Resubmit | Requester; owner | ChangesRequested | PendingReview |

All rows require authenticated identity and current applicable permissions. This table
records confirmed positive journeys; it does not settle the open policies below.

### Key Entities *(include if feature involves data)*

- **User**: An authenticated individual with a stable identity and current permissions
  associated with Requester, Reviewer, or both roles. User provisioning is not specified.
- **Change Request**: An identifiable proposal with title, description, reason, requester
  identity, current state, and the latest explanation from a successful request-changes
  decision, if one exists. Its requester is the owner throughout this workflow.
- **Review Decision**: The business action to approve or request changes on PendingReview.
  Requesting changes requires an explanation stored with the request. This term does
  not require a separate stored decision record or a full feedback-history feature.

### Unresolved Product Decisions

None remain among OPEN-001 through OPEN-010. Each decision was resolved by the user's
explicit answer, recorded in Clarifications; recommendations alone were not approval.
The first pass resolved OPEN-001-005; the second pass resolved OPEN-008/006/007/009/010.
These identifiers remain stable for traceability.

| Resolved ID | Recorded decision | Affected requirements |
| --- | --- | --- |
| OPEN-001 | Owner visibility, Reviewer access to non-Drafts, combined dual-role access, and hidden inaccessible requests | FR-015-016 |
| OPEN-002 | Reject self-request-changes | FR-007-008 |
| OPEN-003 | No deletion in the initial version | FR-017 |
| OPEN-004 | No cancellation or withdrawal in the initial version | FR-011/018 |
| OPEN-005 | Removed roles grant no access; remaining roles keep normal permissions; respect removal before write completion | FR-010/016/019 |
| OPEN-006 | No special retry tracking or success replay; fresh checks; valid repeated creation produces another Draft | FR-011/021 |
| OPEN-007 | Complete nonblank content, partial edits, trimming, inclusive length limits, and atomic rejection | FR-002/004/005/022 |
| OPEN-008 | First valid completed review wins; every competing decision reports a conflict | FR-013/020 |
| OPEN-009 | Reject writes containing system-controlled requester identity, roles, or state, including matching values | FR-003/023 |
| OPEN-010 | Required explanation; save and expose latest feedback; preserve it through revision, resubmission, and approval | FR-007/024 |

### Acceptance Traceability

Executed evidence for every individual FR, SC and scenario is in the
[final evidence matrix](validation.md#t081--final-evidence-matrix-and-disposition).
The table below retains the original business traceability.

| Requirements | Acceptance scenario references |
| --- | --- |
| FR-001-003 | US1-AS01/04/06/15, US2-AS03; creation and rejection of supplied authority/state fields |
| FR-004-005 | US1-AS02-03, US1-AS05, US4-AS01-04 |
| FR-006-008 | US2-AS01-07/12-16, US4-AS05; independent review, self-review rejection, and required feedback |
| FR-009 | US3-AS01; no deletion/cancellation policy may weaken this invariant |
| FR-010-011 | US1-AS04-06, US2-AS04-05, US3-AS02/04/14-16, US4-AS03-04; current-role checks and revocation |
| FR-012 | US1-AS01-03, US2-AS01-02, US3-AS05, US4-AS01-02 |
| FR-013 | US3-AS03-04, US2-AS08-10; concurrency safeguards also apply to retries (FR-021) |
| FR-014 | All rejected/failure scenarios and the stored-data rule preceding the stories |
| FR-015 | US3-AS06/09/11; missing and inaccessible requests do not disclose existence |
| FR-016 | US3-AS08-11; requester, reviewer, dual-role, and unauthenticated visibility |
| FR-017 | US3-AS12; deletion unavailable across all roles, ownership cases, and states |
| FR-018 | US3-AS13; cancellation and withdrawal unavailable across all callers and states |
| FR-019 | US3-AS14-17; removed-role access, in-progress rejection, remaining-role access, and completed writes |
| FR-020 | US2-AS08-10; approve/approve, request-changes/request-changes, and mixed review conflicts |
| FR-021 | US1-AS07-08, US2-AS11, US3-AS18; distinct creations and current-data checks for repeated commands |
| FR-022 | US1-AS01-03/09-14; complete content, trimming, partial edits, length boundaries, and atomic validation rejection |
| FR-023 | US1-AS06/15-16, US3-AS19; reject system-controlled write fields, including matching values |
| FR-024 | US2-AS02/09-10/12-16, US4-AS05; feedback validation, atomicity, visibility, retention, replacement, and concurrency |

## Success Criteria *(mandatory)*

### Measurable Outcomes

These are the unchanged acceptance targets. Actual executed results and their limits
are recorded separately in [validation.md](validation.md#t081--final-evidence-matrix-and-disposition).

- **SC-001**: 100% of the defined successful create/edit/submit, independent-review,
  and revise/resubmit journeys reach the stated final state with the expected content
  and unchanged ownership (US1-AS01-03, US2-AS01-02, US4-AS01-02).
- **SC-002**: Zero self-approvals or self-request-changes succeed across Requester-only,
  Reviewer-only owner, and dual-role owner cases; 100% of rejected attempts preserve
  stored request data (US2-AS03/07).
- **SC-003**: 100% of defined authorization, ownership, state, missing-request, and
  approved-immutability rejection cases produce no change to stored business data.
  Zero deletion, cancellation, or withdrawal attempts succeed across all four states
  and caller-role combinations; 100% leave the request stored and unchanged (US3-AS12-13).
- **SC-004**: Across at least 100 repetitions of each defined concurrent edit/edit,
  edit/submit, approve/approve, request-changes/request-changes, and mixed review scenario,
  zero silent lost updates, incompatible successful outcomes,
  or partial effects occur; every reported unresolved conflict preserves successful
  competing changes. Each otherwise eligible competing review pair produces exactly
  one success and one conflict, preserving the winner's stored decision (US2-AS08-10).
- **SC-005**: In 100% of controlled operation-failure scenarios, participants receive no
  false success and stored business data contains none of the failed operation's effects.
- **SC-006**: Each permitted journey has a distinguishable completed outcome and each
  rejected journey a distinguishable non-success outcome, allowing participants to know
  whether their change took effect in 100% of defined acceptance scenarios. Repeated
  commands follow FR-021; competing review outcomes follow FR-020. In 100% of defined
  retry cases, valid creation produces a distinct Draft and rejected repeats leave
  stored data unchanged without replaying earlier success.
- **SC-007**: 100% of visibility scenarios across the four states and Requester-only,
  Reviewer-only, dual-role, and unauthenticated callers return only permitted request
  data. Zero inaccessible requests appear in lists. For authenticated callers, 100% of
  inaccessible reads produce the same 404 as missing-request reads. In 100% of missing,
  malformed, invalid, or revoked credential cases, protected requests return uniform
  JSON 401 with WWW-Authenticate: Bearer before resource lookup (US3-AS08-11).
- **SC-008**: In 100% of role-removal scenarios, removed roles grant no further access,
  including for owned requests; remaining roles grant exactly their normal permissions.
  Zero writes lacking required permission at completion succeed, zero rejected writes
  leave partial effects, and 100% of writes completed before removal retain their
  business effects (US3-AS14-17).
- **SC-009**: 100% of defined content-validation cases enforce all three nonblank fields,
  trimming, unchanged omitted edit fields, and inclusive length limits. Zero invalid
  creation, edit, or submission attempts produce stored changes; zero excessive values
  are silently truncated (US1-AS09-14).
- **SC-010**: 100% of defined write attempts containing requester identity, roles, or
  system-controlled state are rejected in full, including matching values. Zero such
  attempts create requests, apply partial content changes, or alter stored state
  (US1-AS06/15-16, US3-AS19).
- **SC-011**: 100% of successful request-changes decisions save a valid explanation
  atomically with ChangesRequested, and 100% of defined invalid, failed, or conflicting
  attempts preserve prior stored business data. All authorized request readers can read
  the latest explanation; zero unauthorized readers receive it. In 100% of defined
  revision, resubmission, and approval sequences, it remains unchanged until a later
  successful request-changes decision replaces it (US2-AS02/09-10/12-16, US4-AS05).

## Assumptions

- Trusted user identity and current permission information will be available to evaluate
  operations. Their source and integration are planning dependencies; no token type or
  authentication implementation is selected here.
- Required journeys assume the actors retain the permissions stated in their preconditions.
  Removal of a required role follows FR-019; ownership does not preserve that role's access.
- Business records are retained between operations so their current contents can be
  inspected in acceptance evidence. No storage technology or version mechanism is assumed.
- Positive content scenarios use values satisfying the user's agreed policy in FR-022;
  negative and boundary scenarios verify that policy without selecting technical mechanisms.
- The constitution governs technical and first-version scope constraints by reference;
  this specification does not reproduce them or define implementation structure.
- The original specification/clarification step produced specification artifacts only.
  Subsequent implementation/verification is tracked in tasks.md and validation.md.
  Visibility, self-request-changes,
  deletion, cancellation/withdrawal, and role removal are resolved by OPEN-001-005 answers.
  Competing review decisions are resolved by the OPEN-008 answer in the second pass.
  Repeated commands are resolved by the OPEN-006 answer in the second pass.
  Content validation is resolved by the OPEN-007 answer in the second pass.
  Submitted system-controlled fields are resolved by the OPEN-009 answer in the second pass.
  Required latest review feedback is resolved by the OPEN-010 answer in the second pass.
  None of OPEN-001 through OPEN-010 remain unanswered.
- The product behavior raised by constitution issue CF-OPEN-001 is specified here with
  requirement and decision references. CF-OPEN-002 technical choices belong in Plan/Research;
  this clarification command does not amend the constitution itself.
- No latency, throughput, or user-population target was supplied. The measurable criteria
  above cover workflow completion and integrity; no performance promise is inferred.
