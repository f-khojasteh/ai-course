# ChangeFlow HTTP API Contract

Implemented seven-route contract; T074 runs owner/review/revision/security HTTP suites,
with actual evidence in [validation](../validation.md#t081--final-evidence-matrix-and-disposition).
See [specification](../spec.md),
[plan](../plan.md), [research](../research.md), and [data model](../data-model.md).
Base URL defaults to `http://127.0.0.1:8080`. Normal representations are JSON; errors
are `application/problem+json`. All routes require opaque bearer authentication.
Nonempty request bodies require Content-Type: application/json; unsupported body media
produce 415. Empty-body Submit/Approve requests need no Content-Type. Success negotiation
supports application/json or a matching wildcard; an incompatible Accept produces 406.
Successful JSON representations use canonical UTF-8. An Accept alternative requiring a
different charset is excluded; a UTF-8-compatible fallback may be used, otherwise 406
is returned before a mutation runs. Different encoded bytes cannot share a strong ETag.
Error handlers emit application/problem+json, including security errors, and must not
fall back to HTML when a client requests an unsupported success representation.

## Authentication and visibility precedence

Supply `Authorization: Bearer <locally provisioned credential>`. The placeholder is not
a credential. No role/requester headers, cookies, Basic, form login, query credentials,
public signup, token issuance, or introspection endpoint exists in this design.

Missing, malformed, invalid, or revoked credentials produce uniform JSON **401** and
`WWW-Authenticate: Bearer` before a protected resource is looked up. Authentication is
rechecked inside sensitive write transactions; a credential revoked after the filter
can still cause 401 without a write. Removed roles can cause forbidden/invisible outcomes
under current permissions; possession of an old credential does not restore a role.

**Explicit change from reviewed wording:** The user's later planning instruction requires
401 authentication failures. This narrows spec US3-AS11/FR-015's unauthenticated-not-found
wording. T001 completed that narrow synchronization before code while preserving the actual
clarification history. Authenticated callers still receive identical **404**
for missing and inaccessible items; lists omit inaccessible requests. No existence or
feedback details appear in either 404. A visible item with a forbidden action yields 403.
Item visibility is applied before stored-content rehydration/validation too: invalid
legacy content on a hidden row cannot distinguish it from a missing item. Authorized
invalid-content submission still rejects without rewriting the stored proposal.

Requester reads own requests in all four states. Reviewer reads all non-Drafts. Dual-role
access is the union. A user with neither role sees no request data. No Requester-only
access to other users' requests. Owning a request never permits self-approval or
self-request-changes, including when the owner also has Reviewer.

## Routes

| Method and path | Request DTO/body | Preconditions | Success |
| --- | --- | --- | --- |
| POST /api/change-requests | CreateRequest: title, description, reason | Current Requester | 201, Location, item DTO, ETag |
| GET /api/change-requests | Query offset/limit only | Current visibility | 200, visible collection |
| GET /api/change-requests/{id} | None | Visible item | 200, item DTO, ETag |
| PATCH /api/change-requests/{id} | EditRequest: any supplied proposal fields | Owner Requester; Draft or ChangesRequested; If-Match | 200, item DTO, ETag |
| POST /api/change-requests/{id}/submit | Empty body or empty JSON object | Owner Requester; Draft or ChangesRequested; If-Match | 200, PendingReview item, ETag |
| POST /api/change-requests/{id}/approve | Empty body or empty JSON object | Reviewer; not owner; PendingReview; If-Match | 200, Approved item, ETag |
| POST /api/change-requests/{id}/request-changes | RequestChanges: explanation | Reviewer; not owner; PendingReview; If-Match | 200, ChangesRequested item, ETag |

`id` is a UUID; invalid syntax is 400. No DELETE route or cancellation, withdrawal,
execution, publication, history, or public administration route. An unsupported method
on an existing path returns 405 (with Allow); unknown paths/actions return 404. Both
leave business data unchanged. Authentication still applies to protected paths.

Collection query defaults: offset 0, limit 50, allowed limit 1 through 100. Negative
offset, invalid numeric values, or excessive limit produce 400. Order by createdAt then
id ascending. Response fields are items, offset, limit, hasMore; each item is the item
DTO below. Apply visibility predicates before pagination, and use one extra visible row
to calculate hasMore. There is no collection ETag or cross-page snapshot promise; GET
an item to obtain its current ETag before changing it.

## DTO and normalization rules

DTOs are separate from Domain/JPA entities. Reject system-controlled requester identity,
roles, and state in **every** write body, including matching values (400 FORBIDDEN_FIELD).
Reject other unknown fields instead of silently ignoring misspellings. IDs, revision,
timestamps, and latestExplanation are response-only. The permitted action selects the
transition; clients cannot set state directly.

- Create requires a JSON object with title, description, and reason; no missing/null values.
- Edit distinguishes omitted fields from supplied null using presence-aware DTO fields.
  Omission preserves the old value; null rejects the entire edit. An empty object or
  identical normalized content is a successful no-op after normal authorization and
  revision checks, returning the unchanged representation and ETag. It still verifies
  the optimistic revision at commit and must not hide a concurrent update.
- For proposal text, strip leading/trailing Java whitespace, preserve internal whitespace,
  require nonblank normalized values, and count Unicode code points after normalization.
  Inclusive maxima: title 200, description 5,000, reason 2,000. Reject rather than truncate.
- RequestChanges accepts explanation only: required, non-null, nonblank after the same
  normalization, inclusive maximum 2,000 code points. Save latest feedback atomically
  with the decision; preserve it through edits, submission, and approval. A later
  successful request-changes decision replaces it. No full feedback history is exposed.
- Submit and Approve accept no content fields. If an optional JSON body is present, it
  must be an empty object; arrays, scalars, null, and nonempty bodies are invalid.
- Validate DTOs after normalization using Jakarta Bean Validation, and independently
  enforce the same invariants in Domain. No partial business updates on invalid input.

Java-time fields use Boot-supported Jackson JavaTimeModule handling. Do not hand-format
timestamps or accidentally serialize numeric arrays/epoch numbers. Use a deterministic
item JSON shape and property order, with no per-caller roles/user labels/current-time
fields. Do not enable response-content transformations/compression without revisiting
strong ETag representation identity.

## Item representation and headers

Response-only fields, in stable order:

| Field | JSON type | Meaning |
| --- | --- | --- |
| id | UUID string | Immutable request ID |
| title / description / reason | string | Normalized proposal content |
| requesterId | UUID string | Immutable owner identity |
| state | string | Exact spec state spelling |
| latestExplanation | string or null | Latest feedback visible with this request |
| createdAt / updatedAt | ISO-8601 UTC string | Stored instants, e.g. 2026-11-16T10:00:00Z |
| revision | integer | Numeric Hibernate-managed request version |

Item GET and successful writes include a strong quoted ETag of the form
`"cr-{id}-v{revision}"`. The curly-brace parts denote the actual UUID and unsigned
decimal revision. Location on creation is `/api/change-requests/{id}`. Responses use
Cache-Control: no-store. No credential or role list appears in an item or error response.

Every changed successful mutation returns its own generated revision and stored snapshot,
not a reread of a later writer's state. Flush before taking that snapshot, but emit
success/ETag only after the transactional proxy has committed. An unchanged no-op PATCH
returns the unchanged revision after an optimistic commit check. No manual @Version
increments, forced-increment assumptions, or unguarded bulk updates.

## If-Match contract

Required for PATCH and all three item action POSTs; not required for creation. On an
action path, If-Match explicitly conditions the **parent change request representation**
obtained from item GET, not a separately stored action resource. The parent ID in the
tag must match the route ID.

The supported contract accepts exactly one strong quoted tag in the documented format.
An outer HTTP whitespace wrapper is permitted. No client body revision substitutes for
the header. Require a fresh item GET after a conflict; never automatically retry writes.

| Header situation | Outcome |
| --- | --- |
| Missing If-Match on a supported mutation | 428 PRECONDITION_REQUIRED |
| Malformed tag syntax, invalid UUID/revision, overflow | 400 INVALID_IF_MATCH |
| Wildcard or multiple validators | 400 UNSUPPORTED_IF_MATCH; syntactically valid HTTP forms but excluded by this observed-version API policy |
| Well-formed weak validator | 412 STALE_VERSION; weak syntax is valid but cannot satisfy strong comparison |
| Strong tag for another item or an older/different revision | 412 STALE_VERSION |
| Tag matched at load but Hibernate optimistic check loses at flush/commit | 412 STALE_VERSION; whole transaction rolls back |
| Tag matches current revision but command is invalid for current state | 409 INVALID_STATE |

After authentication and basic syntax checks, protect item existence/visibility and
action permissions before reporting version/state results. For an authorized visible
item compare expected revision before state validation: a competing old review receives
412, even if the winner changed state. A separately issued command with the **current**
tag but an invalid state receives 409. MVC body parsing/validation can reject invalid
syntax before item lookup; this must behave identically for missing and invisible IDs.

For eligible competing reviews of the same observed submission, one succeeds and every
other decision returns 412, including matching decisions. Save only the winning decision
metadata and, if requesting changes wins, its latest explanation. A losing explanation
cannot overwrite earlier or winning feedback. Neither action has automatic priority.

Retries are new attempts, without stored idempotency keys or replayed success. A repeated
valid creation produces another Draft. A repeated action with an old ETag returns 412;
with a current ETag and invalid lifecycle it returns 409. Permission removal or credential
revocation still applies. Lost confirmation after commit does not mean the business
operation rolled back; callers must inspect current data before retrying.

## Problem responses

Use Spring ProblemDetail with type (stable URN), title, status, safe detail, instance
(request path), and stable top-level code. Optional fieldErrors contains field names and
safe validation messages only, never raw rejected values or credential data. Example
shape, not a recorded result:

```json
{
  "type": "urn:changeflow:problem:stale-version",
  "title": "Precondition failed",
  "status": 412,
  "detail": "Refresh the request before retrying.",
  "instance": "/api/change-requests/00000000-0000-0000-0000-000000000001",
  "code": "STALE_VERSION"
}
```

| HTTP | Stable code(s) | Situation |
| --- | --- | --- |
| 400 | INVALID_JSON, INVALID_CONTENT, FORBIDDEN_FIELD, UNKNOWN_FIELD, INVALID_PARAMETER | Invalid body, validation, controlled fields, or query/path syntax |
| 400 | INVALID_IF_MATCH, UNSUPPORTED_IF_MATCH | Preconditions outside accepted grammar |
| 401 | AUTHENTICATION_FAILED | Missing/invalid/revoked credential; always WWW-Authenticate: Bearer |
| 403 | PERMISSION_DENIED | Authenticated visible action not allowed, including self-review |
| 404 | NOT_FOUND | Authenticated missing/invisible request or unknown route; no existence detail |
| 405 | METHOD_NOT_ALLOWED | Unsupported method; include Allow |
| 409 | INVALID_STATE | Current observed version, invalid lifecycle or Approved mutation |
| 412 | STALE_VERSION | Failed strong comparison or actual optimistic race |
| 415 / 406 | UNSUPPORTED_MEDIA_TYPE / NOT_ACCEPTABLE | Unsupported request media / response negotiation |
| 428 | PRECONDITION_REQUIRED | Missing observed version on a supported mutation |
| 503 | STORAGE_BUSY | Bounded row-lock wait or pool/transaction timeout |
| 503 | STORAGE_UNAVAILABLE | Database/file access unavailable; not a version conflict |
| 500 | INTERNAL_ERROR | Unexpected failure; safe generic detail, no SQL/stack trace |

RestControllerAdvice/ResponseEntityExceptionHandler maps MVC parsing, validation,
application, transaction, and persistence failures, including failures raised on proxy
commit. Handle both body and method validation exception forms. AuthenticationEntryPoint,
AccessDeniedHandler, and custom bearer-filter failures use the same JSON problem writer;
they do not rely on MVC advice being invoked for filter errors.
The filter's credential lookup can itself encounter a storage timeout or outage. Its
shared problem writer must return 503 STORAGE_BUSY or STORAGE_UNAVAILABLE respectively,
not convert an infrastructure failure into 401. Never catch every lookup exception as
invalid authentication or expose the underlying SQL/file failure text.

Rejected validation, permission, state, and version operations have no business effect.
Known rollback failures leave parent, feedback, and decision inserts unchanged. Storage
loss or a lost response can make the caller's observed outcome uncertain; do not promise
that an unacknowledged commit never happened or automatically retry creation. Tests must
distinguish controlled rollback from transport loss and inspect storage after recovery.

## Security and administrative boundaries

SessionCreationPolicy.STATELESS; no form login, Basic, browser-cookie authentication,
or session request cache. CSRF is disabled specifically because no automatically attached
browser credential is accepted and Authorization must be explicitly supplied. Reassess
if authentication changes. Bind loopback by default; this is not a public-production
transport design. Do not log Authorization, raw tokens, hashes, or SQL bind values.

Local provisioning/revocation has no HTTP route. Admin mode starts without a servlet
server and exits after one committed operation. The plain embedded H2 default requires
the HTTP process stopped before a separate admin JVM opens its file. Both modes share
application services and credential/access lock ordering. Exact packaged CLI spellings and
observed restart effects were verified in T076 and are documented in
[quickstart](../quickstart.md#offline-administrative-commands). No new HTTP route is implied.
