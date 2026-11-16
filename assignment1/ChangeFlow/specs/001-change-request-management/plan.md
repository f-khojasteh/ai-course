# Implementation Plan: Change-Request Management

**Branch**: `001-change-request-management` (Spec Kit feature identifier; no Git branch was created)
| **Date**: 2026-11-16 | **Spec**: [spec.md](spec.md)

**Input**: Reviewed feature specification, constitution v1.0.0, and the user's explicit
Java 17 / Spring Boot 3.5.16 planning instruction. This is the selected design, reconciled
with implementation through T083. Actual build/test/migration/CLI evidence is recorded
in [validation.md](validation.md#phase-7-validation-and-final-documentation-evidence);
design assertions alone do not establish passing checks or independent human review.

## Summary

Build one local backend that creates and edits owned Drafts, submits requests, supports
independent approval or required changes feedback, and preserves approved immutability.
Use one Maven module, thin Spring MVC controllers, Application services, independent
Domain rules, and Infrastructure JPA adapters mapped to separate persistence entities.
Use Flyway-managed file H2, server-side permissions, revocable hashed opaque credentials,
and optimistic request versions protected by explicit HTTP preconditions.

All OPEN-001 through OPEN-010 product decisions are answered in the spec. The newer
planning instruction explicitly refines authentication-failure transport: unauthenticated
protected requests return JSON 401, whereas authenticated invisible/missing items return
identical 404. This supersedes the unauthenticated-not-found portion of US3-AS11/FR-015;
recorded in research R02 and the HTTP contract. T001 synchronized that wording before code,
preserving the actual clarification answers and other business decisions.

## Technical Context

**Language/Version**: JDK 17; explicit Maven java.version and compiler release 17.

**Primary Dependencies**: Boot parent/BOM 3.5.16; web/MVC, Security, Data JPA, Validation,
Jackson Java Time, flyway-core, H2, Boot test/JUnit 5. Expected managed versions and
primary sources are in [research R01](research.md#r01---runtime-dependencies-and-packaging).
T073 captured the actual effective POM and resolved tree in validation.md; observed versions
match the published pin, including Boot 3.5.16, Hibernate 6.6.53.Final, Flyway 11.7.2 and
H2 2.3.232. No unmanaged production dependency or additional framework was required.

**Storage**: Embedded file H2, `jdbc:h2:file:./data/changeflow`; same absolute resolved
file for normal and administrative mode. Flyway alone creates schema; Hibernate validates.
Default admin CLI is offline-only because plain embedded H2 has one owning JVM.

**Testing**: JUnit 5; every unit/integration class ends in Test. Real HTTP RANDOM_PORT
Boot tests, Java 17 HttpClient, separate transactions and contexts, temporary file H2,
fresh persisted-state inspection, controlled concurrent races, and close/reopen persistence.
The verified Maven lifecycle goal is test: T074 discovered all 30 Test classes and ran
801 cases, including all five 100-repetition cohorts, with zero failures/errors/skips.
Exact machine-specific options are in quickstart.md and validation.md.

**Target Platform**: Java 17 local workstation, one HTTP process, loopback 127.0.0.1:8080.
The non-web administrative mode runs separately while that HTTP process is stopped.

**Project Type**: Single-module backend web service and a local non-web admin mode in
one executable JAR, main class com.changeflow.ChangeFlowApplication, finalName changeflow.

**Performance Goals**: Correctness targets SC-001 through SC-011, including 100 repetitions
per specified concurrency pair. No production throughput, latency, population, or uptime
SLO was supplied; do not invent one. Technical wait budgets are not performance claims.

**Constraints**: Domain has no framework annotations; no HTTP/JPA types in Application
contracts; constructor injection; current identity/permission checks inside sensitive
write transactions; numeric JPA @Version; strong ETag/If-Match; explicit bounded waits.
No manual transactions, shared EntityManager, transactional self-invocation, or unguarded
bulk request updates. No fixed/demo tokens or secrets in source/evidence.

**Scale/Scope**: Local demo with concurrent callers against one file database. No frontend,
execution/publication, AI, RAG, MCP, vector database, microservices, multi-agent product
features, public signup, OAuth issuer/introspection, or generic repositories. Decision
metadata inserts are internal storage evidence, not a new public audit/history feature.

## Constitution Check

The following table is the historical planning-gate assessment. Its PASS entries describe
the design at that time, not executed implementation checks. T073-T081 provide the later
architecture, dependency, security, lifecycle and distribution evidence in validation.md.

| Principle / gate | Before research | After design / evidence |
| --- | --- | --- |
| I - Behavior and traceability | PASS: reviewed stories, FRs, scenarios, ten explicit answers | PASS: mapping below; later 401 directive recorded openly; spec transport wording must be synchronized before code |
| II - Business rules in Domain/Application | PASS | PASS: controllers map transport; Application decides use cases; Infrastructure implements storage and transaction mechanics |
| III - Directed dependencies | PASS | PASS: package graph below; no Domain Spring/JPA; narrow current-use ports and constructor injection |
| IV - Authoritative authorization | PASS | PASS: opaque credentials, trusted caller IDs, current server roles, transaction locks, and both self-review prohibitions |
| V - Atomicity and immutability | PASS | PASS: parent mutation plus decision insert in one transaction; version-guarded writes; no Approved mutations |
| VI - Critical tests | PASS: required by spec | PASS: planned real-HTTP and persisted-state suites include overlapping writes and revocation; not executed |
| VII - Honest evidence and secrets | PASS | PASS: generated tokens displayed once, hashes only, no credentials in files, explicit provisional command/version status |
| VIII - Small initial scope | PASS | PASS: one module/JAR, embedded H2, no extra frameworks; offline admin limitation accepted as local design |
| IX - Governance record | PASS: v1.0.0 read | PASS: decisions reference OPEN/FR IDs; technical CF-OPEN-002 choices are recorded here/research; no constitution amendment claimed |

There are no unjustified constitutional violations or unanswered product/technical design
choices. Published support limitations and future verification obligations are explicit.
The 401 refinement comes directly from the newer user instruction, not agent approval
of a recommendation. No implementation is authorized by completion of this plan alone.

## Project Structure

### Documentation (this feature)

```text
specs/001-change-request-management/
├── spec.md                    # reviewed behavior; preserved here
├── plan.md                    # this implementation design
├── research.md                # decisions, reasons, alternatives, limitations, sources
├── data-model.md               # entities, mappings, constraints, transitions, transactions
├── contracts/http-api.md       # transport and error/precondition contract
├── quickstart.md               # verified build/start/stop/admin commands and constraints
├── tasks.md                    # ordered implementation and checkpoint status
├── validation.md               # executed results, hashes and final evidence matrix
└── checklists/requirements.md  # existing specification-quality artifact
```

tasks.md was generated after planning and executed in the explicitly requested phase batches.

### Source Code (repository root)

Delivered layout (the original plan proposed these package boundaries):

```text
pom.xml
src/main/java/com/changeflow/
├── ChangeFlowApplication.java
├── domain/             # ChangeRequest, RequestState, content/review invariants
├── application/        # use cases, public transactional services, caller/result types
│   └── port/           # request store, current-access guard, credentials, decision log
├── infrastructure/
│   ├── persistence/    # JPA entities/repositories, mappers, concrete adapters
│   └── security/       # opaque credential hashing, verification, secure generation
├── api/                # thin controllers, presence-aware DTOs, problem mapping
└── configuration/      # constructor wiring, Security chain/filter, admin/server mode
src/main/resources/
├── application.yml
└── db/migration/       # versioned Flyway SQL only
src/test/java/com/changeflow/
├── domain/             # pure rules *Test
├── infrastructure/     # opaque credential codec *Test
└── integration/        # HTTP, migration, concurrency, revocation, restart *Test
```

**Structure Decision**: One module keeps the first implementation small. Domain imports
only Java. Application imports Domain and Spring transaction annotations where required,
but no API/JPA/concrete adapter types. Infrastructure imports Domain/Application and
implements their contracts. API calls Application. Configuration composes all packages;
no lower package imports Configuration. Do not introduce a separate interface for every
class, a generic CRUD repository, event bus, mapping framework, or service deployable.

### Application boundaries and transaction design

Public ChangeRequestService operations are create, list/read, edit, submit, approve, and
requestChanges. LocalAdministrationService provisions users/roles/credentials and revokes
credentials/roles; it is reached by non-web local mode, with no public administrative API.
Construct services with use-case-specific ports and a Java Clock. Domain validates
normalized content/state and immutable ownership; Application checks current access and
coordinates operations. JPA repositories and mapping entities stay in Infrastructure.

Every sensitive write enters a public @Transactional method through a Spring proxy.
Configure rollbackFor=Exception.class and propagate failures so checked errors cannot
commit partial work. Set spring.jpa.open-in-view=false. Use one JpaTransactionManager
and a fresh transaction-bound persistence context. Lock
Credential then UserAccess; administrative revocation uses the compatible order documented
in research R09. Role removal locks UserAccess only and must never later lock an existing
Credential. Load request after access locks; check visibility/ownership, If-Match expected
revision, then state and Domain rules. This is a proposed application protocol to implement
and prove, not a guarantee supplied merely by annotations.

T082 makes item lookup access-aware before Domain rehydration: the port accepts the
current user's server-side visibility flags, and the adapter excludes invisible rows
before validating stored content. Application retains its own visibility check. Invalid
legacy content therefore cannot become an existence oracle; authorized invalid submission
still rejects without repair. List filtering and item filtering use the same predicate.

Keep READ_COMMITTED; H2 session LOCK_TIMEOUT=2000 ms, pool acquisition 3000 ms, public
write transaction timeout 10 s. JPA lock hints are supplementary. Different users' locks
allow real optimistic request races. Same-user writes serialize deliberately. Filter
proof carries immutable verified credential/user IDs, not a managed entity or authoritative
cached role list. Recheck current credentials and permissions in the write transaction.

Each review updates the parent and inserts decision metadata atomically. Explanations
live only on the parent; historical metadata stores no feedback text. On changed writes,
flush then capture an immutable result with Hibernate's generated version. API constructs
the response only after proxy commit succeeds. Identical-content PATCH keeps its ETag
and timestamp but uses an explicit optimistic version check at commit. Never manually
increment @Version or expose managed entities beyond the transaction.

### Transport and operating decisions

[HTTP contract](contracts/http-api.md) defines seven routes, validated DTOs, strong quoted
ETags, If-Match requirements, deterministic representations, and ProblemDetail codes.
Read lists apply access predicates; invisible items are not fetched and filtered after
pagination. No item DTO includes roles, credentials, or JPA internals.
T083 pins successful JSON bytes to UTF-8. Header negotiation excludes incompatible charset
alternatives before controller invocation; it accepts a compatible fallback or returns
the existing 406. This preserves strong version-tag identity without adding another tag
format or allowing a negotiation failure after a business mutation has committed.

Boot startup applies Flyway before Hibernate validate and starts embedded HTTP. Admin mode
must be selected explicitly, uses WebApplicationType.NONE, performs one local operation,
commits/closes, and exits with a meaningful status; it never seeds fixed credentials or
falls back to starting HTTP on bad admin arguments. Actual command spelling is deferred
until command parsing and the executable JAR exist and have been verified.

### Traceability and planned verification

| Requirements | Design location | Verification obligations (executed evidence in validation.md) |
| --- | --- | --- |
| FR-001-003 | Domain identity/state; current roles; create DTO and store | US1 create and forged-input cases, dual-role behavior |
| FR-004-005 | partial edit and submit services; Domain content/state rules | owned/non-owned Draft and ChangesRequested; unchanged state/content on rejection |
| FR-006-008 | independent review policies and decision insert | both decisions; self-approval and self-request-changes for all role combinations |
| FR-009 | immutable Approved Domain state and no delete/cancel routes | all attempted Approved writes inspect stored parent/feedback/decision rows |
| FR-010-011 | access guard, ordering, explicit commands | current permissions, invalid state, filter plus in-transaction checks |
| FR-012-014 | transactions, guarded UPDATE, problems | fault injection, real overlapping contexts, winning state preserved; no partial decision insert |
| FR-015-016 | visibility queries, uniform authenticated 404 | all roles/states; 401 refinement to unauthenticated reads documented in R02 |
| FR-017-018 | deliberately absent delete/cancel/withdraw | unsupported methods/routes fail and stored data remains unchanged |
| FR-019 | credential/access lock protocol | both lock-winning orders; removed roles and revoked credentials; bounded lock waits |
| FR-020 | @Version and first completed valid review | separate Reviewers racing approve/approve, changes/changes, mixed; one success/one 412 |
| FR-021 | no idempotency store/replay | repeated create makes distinct Draft; invalid repeated commands re-evaluate and reject |
| FR-022-023 | normalized/presence-aware DTOs and Domain validation | limits, Unicode, null/omission, forbidden fields even matching; whole-operation rollback |
| FR-024 | latest explanation + metadata-only decision insert | required feedback, retention/replacement/visibility, loser explanation not stored |

All sensitive tests inspect HTTP outcomes and committed storage via a fresh transaction.
Temporary file H2 uses the same Flyway/validate strategy as the demo. Test class suffixes
must let the requested Maven test lifecycle discover both unit and integration classes.
A test-only synchronization barrier allows two different Reviewers to load one version
before competing updates; sequential stale calls alone are insufficient. Test both
write-first and revoke-first ordering through the real local administration service in
one JVM, plus failure injection and restart on the same file after pool/context closure.
A successful H2 suite establishes no PostgreSQL compatibility claim.

## Complexity Tracking

No constitutional exceptions. Credential/access locks address the concrete current-access
race; request optimistic locking addresses lost updates. Decision metadata inserts are
explicitly requested and exclude feedback history. The single package structure, offline
admin command, and DTO presence tracking avoid additional services or frameworks.

### Implementation reconciliation and disposition

The original plan produced design artifacts only. Subsequent authorized batches implemented
T001-T081: 57 production Java files, 30 Test classes, V1 SQL and the executable JAR. Application
use cases and fault paths are verified through the real integration/service-proxy suites;
there is no standalone application Test package. Pure Domain rules remain unit tested.
No product behavior, dependency direction, authentication policy or latest-feedback choice
was changed during final documentation. Constitution v1.0.0 is unchanged.

Two concrete details were proven during implementation: H2 columns preserve Instant
nanoseconds with TIMESTAMP(9), and the actually observed Hibernate TransactionException
message `transaction timeout expired` maps narrowly to STORAGE_BUSY; unrelated provider
failures remain INTERNAL_ERROR. Both fit the reviewed timestamp/timeout intent and have
regression evidence. No broad exception-to-503 mapping or automatic retry was added.

T074 full tests and T075/T076 packaged startup/migration/graceful restart/offline CLI passed.
Quickstart, README and AGENTS now reference actual commands and limitations. The constitution's
initial CF-OPEN-001 behavior choices were resolved by the reviewed spec; CF-OPEN-002 technical
choices were resolved by this reviewed plan/research. Their adoption-time text is historical,
not a reason to amend the constitution during implementation. No amendment was made.
Step 13's Converge subsequently identified F1/F2 and appended T082/T083. Their corrections
and regression evidence are recorded in [Phase 8 validation](validation.md#phase-8-convergence-corrections-evidence).
No follow-up Converge is implied by implementation; it must assess the corrected code separately.
