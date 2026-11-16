# ChangeFlow Data Model and Transaction Protocol

This is the selected model/protocol, reconciled with implemented V1 SQL and JPA adapters
at T080. Actual schema, atomicity, concurrency and restart proof is in
[validation.md](validation.md#phase-7-validation-and-final-documentation-evidence). Business authority is
[spec.md](spec.md); technical decisions and alternatives are in [research.md](research.md).
All timestamps are Java Instant values stored in H2 TIMESTAMP(9) WITH TIME ZONE columns
and serialized as ISO-8601 UTC text. Preserve nanoseconds so a response snapshot and a
later GET of the same revision cannot differ through storage rounding. UUIDs are
generated server-side.

## 1. Domain and persistence separation

Domain ChangeRequest owns identity, proposal content, requester identity, the four states,
and latest feedback. It contains no Spring, JPA, HTTP, or transaction annotations.
Application coordinates authorization, expected revision, storage, and review recording.
Infrastructure maps Domain values to separately named JPA entities and back. Entity
references, lazy proxies, EntityManager, and Spring Data interfaces do not cross into
Application contracts or HTTP DTOs.

Application returns an immutable request snapshot plus numeric revision; API maps this
to a response DTO and ETag. The Domain aggregate does not parse If-Match or know @Version.
Use constructor injection at every boundary, with Java Clock for testable timestamps.

## 2. UserAccess

| Field | Persistence mapping | Meaning |
| --- | --- | --- |
| userId | UUID primary key, immutable | Stable authenticated identity and ownership reference |
| requester | non-null boolean | Current Requester permission set |
| reviewer | non-null boolean | Current Reviewer permission set |
| updatedAt | timezone-aware timestamp, non-null | Last local access administration |

Both flags may be true or false. Requester permits create plus owned edit/submit and
owned reads. Reviewer permits reads of all non-Drafts and review of others' PendingReview
requests. Neither flag alone changes an ownership or state restriction. No third public
role is introduced for the local operator. Removing a flag does not change request
ownership and preserves access supplied by the other flag only (FR-001/010/016/019).

Keep this row permanently while requests/credentials/decisions reference its identity;
user deletion and identity reassignment are not offered. Permission modifications acquire
the UserAccess write lock described below; an @Version on this row is unnecessary for
the selected serialized administrative access protocol.

## 3. Credential

| Field | Persistence mapping | Meaning |
| --- | --- | --- |
| credentialId | random UUID primary key | Nonsecret lookup component |
| userId | non-null foreign key to UserAccess | Identity verified by this credential |
| secretHash | non-null fixed binary SHA-256 digest, unique | Hash of a generated 256-bit secret |
| createdAt | timestamp, non-null | Creation instant |
| revokedAt | nullable timestamp | Null means active; revocation is irreversible |

Provision a raw opaque value consisting of the lookup ID and a cryptographically random
secret. The secret is not a user-chosen password, a JWT, or a role-bearing claim. Generate
through SecureRandom, compare digests in constant time, and display the raw value once
only after the provisioning transaction commits. Never persist raw credentials or place
them in logs, fixtures, migrations, HTTP bodies, exception details, or documentation.

Credentials may be multiple per user; the secret/hash association is immutable. Issue a
new ID/secret rather than changing an existing secret. Revocation records revokedAt and
does not delete the identity. No public token issuance, signup, or introspection endpoint.
Local administrative mode uses these same application operations and exits without HTTP.

## 4. ChangeRequest

| Field | Persistence mapping | Invariant / mapping |
| --- | --- | --- |
| id | UUID primary key | Server-created immutable request identity |
| title | non-null Unicode text | Nonblank, at most 200 normalized code points |
| description | non-null Unicode text | Nonblank, at most 5,000 normalized code points |
| reason | non-null Unicode text | Nonblank, at most 2,000 normalized code points |
| requesterId | non-null foreign key to UserAccess | Authenticated creator; never reassigned |
| state | non-null string enum | Draft, PendingReview, ChangesRequested, Approved only |
| latestExplanation | nullable Unicode text | Latest successful request-changes explanation, at most 2,000 normalized code points |
| createdAt | timestamp, non-null, immutable | Server Clock at creation |
| updatedAt | timestamp, non-null | Last actual successful content/state mutation |
| version | non-null BIGINT after insert; JPA Long @Version | Hibernate-managed optimistic revision, never assigned by application |

Use infrastructure entities, not this Domain object, for the JPA mapping. For H2 choose
text column capacity that safely accommodates supplementary Unicode characters; for
example, title capacity 400 UTF-16 units and large-text columns for description/reason/
feedback. Physical capacity is not the business length rule. Domain and normalized DTO
validation enforce code-point limits consistently; do not let a smaller JDBC/SQL length
silently reject accepted supplementary characters or truncate text.

Normalize supplied content with Java 17 strip semantics, preserving internal whitespace.
Creation requires all three proposal fields; explicit null and normalized blank are
invalid. Partial edit tracks field presence separately: omission preserves, null rejects.
Apply valid supplied fields together or none. Submission validates existing content and
does not normalize/rewrite it. DTOs must not accept requesterId, roles, or state fields,
even if they contain matching values (FR-022/023).

latestExplanation is absent before the first successful request-changes decision. That
decision requires nonblank normalized feedback; store it with the state change. Revision,
resubmission, and approval preserve it. Only a later successful request-changes decision
replaces it. It is readable with the parent under existing visibility, not through a
separately authorized feedback endpoint. There is no full feedback history (FR-024).

All exposed item fields, including timestamps and latestExplanation, participate in the
same parent's versioned update. Do not include dynamically resolved user names or current
roles in the item representation. An identical-content PATCH leaves updatedAt and revision
unchanged but requests an explicit optimistic version check at commit. A changed PATCH
or state transition obtains its new version through Hibernate's guarded UPDATE.

## 5. ReviewDecision metadata

| Field | Persistence mapping | Meaning |
| --- | --- | --- |
| id | UUID primary key | Internal decision identity |
| requestId | foreign key, non-null | Parent request |
| reviewerId | foreign key to UserAccess, non-null | Authenticated reviewer, never the owner |
| action | non-null string enum | Approve or RequestChanges |
| decidedAt | timestamp, non-null | Server Clock at valid decision |
| resultingRequestVersion | BIGINT, non-null | Hibernate-generated parent revision for this decision |

Add a unique constraint on (requestId, resultingRequestVersion) and indexes serving
request and reviewer lookups needed for integrity/verification. Insert only on successful
review; do not insert on edit, submit, rejection, failure, or losing review conflict.
This metadata contains **no explanation text**. It satisfies the user's requested atomic
decision insert without reconstructing earlier feedback or exposing a history feature.
The parent remains the source of current state and latest explanation. Do not derive
authorization from metadata or add a generic audit/event framework.

Flush the parent first to obtain the generated revision, then append and flush decision
metadata inside the same transaction. Known flush/transaction failures roll back the
operation; constraint failures must not leave a successful parent transition without
its decision insert. A lost commit acknowledgement can leave the complete outcome
uncertain: never report success, and inspect fresh storage after recovery before claiming
rollback. Atomicity means parent and decision commit together or neither commits; it
does not guarantee the caller always receives the outcome.

## 6. State transitions

| Operation | Actor / current permission | Before | After | Content / feedback effect |
| --- | --- | --- | --- | --- |
| Create | Requester | absent | Draft | Store valid normalized proposal; requester from trusted identity |
| Edit | Requester, owner | Draft | Draft | Partial proposal edits; preserve feedback |
| Submit | Requester, owner | Draft | PendingReview | Preserve proposal/feedback |
| Approve | Reviewer, not owner | PendingReview | Approved | Preserve proposal/feedback; insert decision metadata |
| Request changes | Reviewer, not owner | PendingReview | ChangesRequested | Preserve proposal; replace latest feedback; insert metadata |
| Revise | Requester, owner | ChangesRequested | ChangesRequested | Partial proposal edits; preserve feedback |
| Resubmit | Requester, owner | ChangesRequested | PendingReview | Preserve proposal/feedback |

No deletion, cancellation, withdrawal, execution, publication, or Approved mutation.
Self-approval and self-request-changes are forbidden for dual-role users too. Every
post-creation mutation requires an observed revision under the HTTP contract. New
submission/review retries are fresh commands, not success replay; valid repeated creation
makes another request. Neither review action has priority: first valid completion wins.

## 7. Atomic write algorithm and lock order

1. Enter the public Application transactional proxy; transaction isolation is
   READ_COMMITTED, timeout 10 seconds, and rollbackFor=Exception.class. Propagate failure
   rather than returning normally from a failed transaction. A verified caller contains
   credentialId/userId only.
2. Load and PESSIMISTIC_WRITE-lock caller Credential, then its UserAccess. Do not use
   entity instances loaded in the security filter or cached before lock acquisition.
3. Verify immutable association, hash-verified credential proof, current revokedAt, and
   current roles; reject revoked credentials or removed permission without mutation.
4. For item operations load the request freshly, check visibility/ownership, compare
   supplied expected revision, then validate current state and Domain invariants.
   Do not pessimistically lock the request; request concurrency uses @Version.
5. Apply the Domain operation; map changed values to the managed infrastructure entity.
   Creation also uses these access locks so revocation cannot race initial storage.
6. Flush the parent. For review append and flush metadata with the resulting version.
   Capture an immutable snapshot, including normalized values and generated revision.
7. Allow the Spring proxy to commit. Only its successful return permits the controller
   to serialize 2xx/ETag. An exception during flush or commit is never a success.

Credential revocation locks target Credential then target UserAccess. Role removal locks
UserAccess alone and never locks an existing Credential afterward. Initial provisioning
may lock UserAccess and insert a new Credential, but must not traverse backward to lock
an existing credential. Avoid multi-user/bulk administrative commands in this version.

If a write wins its locks, it commits before revocation takes effect. If revocation
commits first, the writer's fresh locked reads see it and reject. An attempted removal
has no effect until its transaction commits. Configure each H2 connection's LOCK_TIMEOUT
to 2,000 ms; pool acquisition timeout is 3,000 ms and the write transaction timeout 10 s.
Map lock/storage failures distinctly from optimistic version failures; test the actual
exception causes and waits instead of relying on a JPA hint as a guarantee.

Same-user calls serialize on UserAccess, including different credentials for that user.
Different Reviewers lock different rows, so genuine overlapping request UPDATEs remain
possible. Do not share an EntityManager between their threads. No manual commit/rollback,
self-invocation to start a transaction, or automatic retry of sensitive operations.

## 8. Schema lifecycle, reads, and operating limits

Select spring.jpa.open-in-view=false. Managed persistence contexts end at the service
transaction boundary; API serialization operates on immutable snapshots only.

Flyway is the sole schema creator under db/migration. Initial migrations will create the
four application tables, FKs, state/action constraints, and required indexes; no SQL is
written by this planning step. Hibernate ddl-auto=validate must run after successful
migration. No create/update, schema.sql/data.sql, fixed credential seed, or startup reset.

List queries enforce visibility in the database predicate before limit/offset: own rows
for current Requester OR non-Draft rows for current Reviewer. Item lookup returns no
data for missing/invisible requests. Item visibility filtering precedes Domain rehydration,
so invalid hidden stored content still returns NOT_FOUND; Application retains its current
visibility/permission checks. Read transactions recheck current server access;
they do not trust roles supplied in a header. No read mutates request/version/decision.

H2 data path is relative to the process working directory unless explicitly overridden;
normal/admin modes must resolve the same absolute file. Database username/password are
external local configuration, not committed defaults. Keep database, trace, and generated
credential files out of version control. Disable H2 console and SQL bind/credential logging.
Close pools before restart or offline administration. A separate admin JVM cannot open
the default embedded database while the HTTP JVM owns it. H2-only evidence does not
establish portability to PostgreSQL, and no such migration is implemented here.

