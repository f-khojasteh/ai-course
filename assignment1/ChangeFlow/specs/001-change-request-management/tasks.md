# Tasks: Change-Request Management

**Input**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [HTTP contract](contracts/http-api.md),
[quickstart.md](quickstart.md), and constitution v1.0.0.

**Prerequisites**: Reviewed behavior and design; OPEN-001 through OPEN-010 have actual
user answers. T001 synchronizes the later explicit 401 instruction before code begins.

**Tests**: Required by the assignment and constitution. Author the listed tests before
their associated behavior, demonstrate meaningful red then green, and record blocked
execution honestly. A missing tool/class is not evidence that a business assertion failed.
Test-authoring tasks can finish with documented red; phase checkpoints require green.
The original generation step executed no task; subsequent completion markers and phase
dispositions record the explicitly authorized implementation batches.

**Organization**: The user's requested seven groups are used. US1, US2, and US3 retain
P1 priorities; US4 remains P2. US3's final stress/failure verification follows revision
as requested, but its authentication, authorization, transaction, version, and immutability
controls are mandatory in the foundation and every earlier story, not postponed.

## Format: `[ID] [P?] [Story] Description`

- Each task has an unchecked box, stable sequential ID, exact target paths, requirement
  or design references, and explicit prerequisites in `Deps:`. Do not renumber on execution.
- `[US1]` through `[US4]` are the existing spec story IDs. Setup/foundation/final tasks
  have no story label; story phases require one.
- `[P]` applies only within the named cohorts below, after every prerequisite is complete.
  Those tasks write different files and read shared helpers only. It does not authorize
  parallel edits to a shared service, controller, entity, configuration, or evidence file.
- Phases are barriers. Unmarked tasks run in numeric order; the next unmarked task waits
  for all members of the preceding parallel cohort. Explicit Deps add precise gates.
- All sensitive HTTP tests inspect committed request fields, version, latest feedback,
  and decision rows in a fresh transaction. Never infer rollback from response status alone.

## Path Conventions

All task paths are repository-relative to
`F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow`. They name actual intended files, not path
templates. Use one Maven module, `src/main/java/com/changeflow/` packages and
`src/test/java/com/changeflow/` tests. Every JUnit test class ends in `Test`; helper and
configuration classes are not test classes. No application files exist at generation.

## Phase 1: Setup

**Purpose**: Align authoritative documents and initialize only the reviewed Java 17 build.

- [X] T001 Synchronize only the later authentication refinement in `specs/001-change-request-management/spec.md` (FR-015, US3-AS11, affected US3-AS14/SC-007 and clarification context): missing/invalid/revoked credentials receive uniform 401 plus WWW-Authenticate: Bearer before lookup; authenticated missing/invisible requests remain identical 404. Refresh stale explanatory notes in `specs/001-change-request-management/checklists/requirements.md` against the actual resolved decisions without inventing acceptance evidence. Preserve all existing FR/scenario/OPEN IDs and every other user decision. Refs: research R02, HTTP contract. Deps: none.
- [X] T002 Create `pom.xml` with Boot parent 3.5.16, java.version 17 and compiler release 17, requested web/security/data-jpa/validation/Jackson-Java-Time/Flyway-core/H2/test dependencies, inherited managed plugin versions, and explicit versions for any genuinely unmanaged item; configure Boot repackage, finalName changeflow and mainClass com.changeflow.ChangeFlowApplication. Keep all unit/integration *Test classes in normal Surefire test discovery, with no Failsafe-only suite or default exclusions. Refs: plan Technical Context, research R01/R13. Deps: T001.
- [X] T003 Create/update `.gitignore` to exclude target/, data/ database/trace files, private local configuration, token captures and validation secrets while retaining source-controlled migrations and documentation; never add fixed credentials or commit actual database files. Refs: constitution VII, research R03/R07. Deps: T002.
- [X] T004 Create `src/main/java/com/changeflow/ChangeFlowApplication.java` with the reviewed root package and normal Boot startup; establish only domain/application/infrastructure/api/configuration package boundaries and constructor wiring conventions. Do not add frontend, execution/publication, generic repositories or extra frameworks. Refs: constitution II/III/VIII, research R04. Deps: T003.
- [X] T005 Create `src/main/resources/application.yml` and `src/main/java/com/changeflow/configuration/ApplicationConfiguration.java` with Java Clock wiring, loopback 127.0.0.1:8080, overridable jdbc:h2:file:./data/changeflow and external database credentials, Flyway db/migration, spring.jpa.hibernate.ddl-auto=validate, spring.jpa.defer-datasource-initialization=false, spring.sql.init.mode=never, spring.jpa.open-in-view=false, H2 connection-init SQL SET LOCK_TIMEOUT 2000 and pool acquisition 3000 ms and disabled H2 console/secret-bearing SQL logs. No AUTO_SERVER or fixed token seed. Refs: research R03/R06/R09, data model sections 7-8. Deps: T004.
- [X] T006 Create `specs/001-change-request-management/validation.md` with actual implementation-start environment/build evidence after POM and source exist: check JDK 17/Maven, record effective dependency/plugin versions against research R01, and identify the revision or source snapshot. Distinguish published expected versions from actually resolved versions; do not claim any unrun tests. Refs: constitution I/VII, research R01/R13. Deps: T002, T004, T005.

**Checkpoint**: Documentation conflict reconciled, pinned build/source bootstrap exists,
and local secret handling is established. No feature is complete yet.

## Phase 2: Shared Foundation (Blocking Prerequisites)

**Purpose**: Schema, independent rules/types, persistence, local administration, security,
and test support required by every story. Database-dependent integration work begins
only after T007 supplies migrations. No user-story phase starts before T030.

- [X] T007 Create `src/main/resources/db/migration/V1__create_changeflow_schema.sql` for UserAccess, Credential, ChangeRequest and ReviewDecision metadata with the exact fields/constraints in data-model.md sections 2-5 and T012-T015; include FKs, state/action checks, unique secret hash and unique (requestId, resultingRequestVersion), and TIMESTAMP(9) WITH TIME ZONE. Flyway is the only schema creator; no schema.sql/data.sql/import.sql, fixed users, raw tokens or feedback-history column in decision metadata. Refs: FR-002/012/019/024, research R06/R11. Deps: T005, T006.
- [X] T008 Author `src/test/java/com/changeflow/integration/MigrationStartupTest.java` using an isolated temporary file H2: assert fresh-file Flyway-before-JPA-validate ordering, expected tables/constraints, repeat-start nonreset behavior, and failed startup on broken migration or schema mismatch. Keep its initial bootstrap fixture self-contained; do not assume H2 DDL rollback. Refs: research R06, quickstart startup validation. Deps: T007.
- [X] T009 Author `src/test/java/com/changeflow/domain/ContentRulesTest.java` for normalized nonblank proposal/feedback, inclusive limits, strip semantics, internal whitespace, null and supplementary Unicode code-point counts. Exercise limits after normalization, not raw @Size. Refs: FR-022/024, US1-AS09-14, US2-AS12-13, SC-009. Deps: T008.
- [X] T010 Implement `src/main/java/com/changeflow/domain/ContentRules.java`, `src/main/java/com/changeflow/domain/RequestState.java`, `src/main/java/com/changeflow/domain/UserRole.java`, and `src/main/java/com/changeflow/domain/ReviewAction.java` without Spring/JPA: title "Nonblank, at most 200 normalized code points", description "Nonblank, at most 5,000 normalized code points", reason "Nonblank, at most 2,000 normalized code points"; states "Draft, PendingReview, ChangesRequested, Approved only"; review action "Approve or RequestChanges"; feedback nonblank with inclusive 2,000-code-point limit. Refs: FR-001/002/022/024, T009. Deps: T009.
- [X] T011 Establish the shared aggregate/value fields and rehydration invariants in `src/main/java/com/changeflow/domain/ChangeRequest.java`: id "Server-created immutable request identity"; requesterId "Authenticated creator; never reassigned"; latestExplanation "Latest successful request-changes explanation, at most 2,000 normalized code points" and nullable before first review; createdAt "Server Clock at creation" and immutable; updatedAt "Last actual successful content/state mutation". Reuse T010 content/state rules, preserve ownership/feedback when rehydrating and keep revision/HTTP outside Domain; add lifecycle methods in their story tasks. Refs: FR-002/009/024, data model section 4. Deps: T010.
- [X] T012 [P] Implement `src/main/java/com/changeflow/infrastructure/persistence/UserAccessEntity.java` and `src/main/java/com/changeflow/infrastructure/persistence/UserAccessJpaRepository.java`: userId "UUID primary key, immutable", requester/reviewer "non-null boolean", updatedAt "timezone-aware timestamp, non-null"; both flags may be false or true. Store other-table associations as scalar UUIDs backed by migration FKs so this task does not depend on another parallel entity. No caller-supplied authority or identity reassignment. Refs: FR-001/010/019, data model section 2. Deps: T007, T011.
- [X] T013 [P] Implement `src/main/java/com/changeflow/infrastructure/persistence/CredentialEntity.java` and `src/main/java/com/changeflow/infrastructure/persistence/CredentialJpaRepository.java`: credentialId "random UUID primary key", userId "non-null foreign key to UserAccess", secretHash "non-null fixed binary SHA-256 digest, unique", createdAt "timestamp, non-null", revokedAt "nullable timestamp" with "Null means active; revocation is irreversible". Association/hash are immutable; use UUID fields, not another parallel entity type. Refs: FR-010/019, data model section 3. Deps: T007, T011.
- [X] T014 [P] Implement `src/main/java/com/changeflow/infrastructure/persistence/ChangeRequestEntity.java` and `src/main/java/com/changeflow/infrastructure/persistence/ChangeRequestJpaRepository.java`: id "UUID primary key"; title/description/reason "non-null Unicode text" with T010 limits; requesterId "non-null foreign key to UserAccess"; state "non-null string enum"; latestExplanation "nullable Unicode text"; createdAt "timestamp, non-null, immutable"; updatedAt "timestamp, non-null"; version "non-null BIGINT after insert; JPA Long @Version" and "Hibernate-managed optimistic revision, never assigned by application". Preserve nanoseconds, allow sufficient physical Unicode capacity, and use scalar UUID associations. Refs: FR-002/009/013/024, data model section 4. Deps: T007, T011.
- [X] T015 [P] Implement `src/main/java/com/changeflow/infrastructure/persistence/ReviewDecisionEntity.java` and `src/main/java/com/changeflow/infrastructure/persistence/ReviewDecisionJpaRepository.java`: id "UUID primary key"; requestId "foreign key, non-null"; reviewerId "foreign key to UserAccess, non-null" with "Authenticated reviewer, never the owner" enforced by Application; action "non-null string enum" and "Approve or RequestChanges"; decidedAt "timestamp, non-null"; resultingRequestVersion "BIGINT, non-null". Enforce the migration uniqueness constraint; retain no explanation text and use scalar UUID associations. Refs: FR-006/007/012/024, data model section 5. Deps: T007, T011.
- [X] T016 Implement `src/main/java/com/changeflow/infrastructure/persistence/ChangeRequestMapper.java` between independent Domain values and managed persistence entities; map every proposal/feedback/time field without exposing lazy proxies, accepting caller ownership, or assigning an existing @Version. Rehydration is distinct from validated creation; no entities become HTTP bodies. Refs: FR-002/009/024, research R04/R05. Deps: T012, T013, T014, T015.
- [X] T017 Define narrow current-use contracts in `src/main/java/com/changeflow/application/port/ChangeRequestStore.java`, `src/main/java/com/changeflow/application/port/CurrentAccessGuard.java`, `src/main/java/com/changeflow/application/port/CredentialStore.java`, `src/main/java/com/changeflow/application/port/UserAccessStore.java`, `src/main/java/com/changeflow/application/port/CredentialCodec.java`, and `src/main/java/com/changeflow/application/port/ReviewDecisionLog.java`; define `src/main/java/com/changeflow/application/AuthenticatedCaller.java`, `src/main/java/com/changeflow/application/RequestSnapshot.java`, and `src/main/java/com/changeflow/application/ApplicationFailure.java`. Use immutable IDs/results and expected numeric revision, never HTTP/JPA types or generic CRUD abstractions; expose lock/flush outcomes needed by the reviewed transaction protocol. Refs: FR-010/012/013/019/024, research R04/R09/R10. Deps: T016.
- [X] T018 Implement `src/main/java/com/changeflow/infrastructure/persistence/JpaChangeRequestStore.java` and `src/main/java/com/changeflow/infrastructure/persistence/JpaReviewDecisionLog.java`, completing required query declarations in `src/main/java/com/changeflow/infrastructure/persistence/ChangeRequestJpaRepository.java`: fresh loads, access predicate before pagination, managed @Version updates and explicit OPTIMISTIC no-op checks, flush/snapshot support, then metadata insert/flush with resulting revision in the caller's transaction. No bulk request updates, manual commit/rollback or historical feedback. Refs: FR-012/013/015/016/024, research R09-R11. Deps: T017.
- [X] T019 Author `src/test/java/com/changeflow/infrastructure/security/OpaqueCredentialCodecTest.java` for random 256-bit secrets, opaque format parsing, SHA-256-only storage material, constant-time comparison use, invalid inputs and distinct issued values without printing tokens. Refs: research R07, constitution VII. Deps: T018.
- [X] T020 Implement `src/main/java/com/changeflow/infrastructure/security/OpaqueCredentialCodec.java` against the Application port with SecureRandom, random credential IDs, digest-only persistence input and constant-time verification. Never persist/log a raw secret or derive authority from token/header roles. Refs: FR-010/019, research R07, T019. Deps: T019.
- [X] T021 Implement `src/main/java/com/changeflow/infrastructure/persistence/JpaCredentialStore.java`, `src/main/java/com/changeflow/infrastructure/persistence/JpaUserAccessStore.java`, and `src/main/java/com/changeflow/infrastructure/persistence/JpaCurrentAccessGuard.java`: fresh Credential PESSIMISTIC_WRITE then UserAccess PESSIMISTIC_WRITE for sensitive writes/revocation; role removal locks only UserAccess and never an existing Credential afterward; provisioning may insert a new credential after an access lock. Re-read current revocation/roles; no managed entity or cached roles from the filter. Refs: FR-010/019, data model section 7. Deps: T020.
- [X] T022 Author `src/test/java/com/changeflow/integration/LocalAdministrationTest.java` against migrated temporary file H2 for provisioning, hash-only persisted credentials, both role flags, irreversible revocation, one-time display after commit, rollback/error outcomes and no public HTTP issuance route. Use generated secrets privately and fresh storage inspection. Refs: FR-001/010/019, research R03/R07/R09. Deps: T021.
- [X] T023 Implement `src/main/java/com/changeflow/application/LocalAdministrationService.java` with public Spring-proxied transactions, READ_COMMITTED, timeout 10 seconds and rollbackFor=Exception.class; provision identities/roles/random credentials, revoke credentials and remove roles with T021 ordering. Return one-time secret material only after a successful proxy commit; never swallow failures or change ownership. Refs: FR-010/012/019, T022. Deps: T022.
- [X] T024 Implement `src/main/java/com/changeflow/configuration/AdministrativeModeConfiguration.java` and `src/main/java/com/changeflow/configuration/LocalAdministrationRunner.java`, updating `src/main/java/com/changeflow/ChangeFlowApplication.java` to select explicit non-web administrative mode before HTTP creation. Define/validate actual command actions/arguments, perform one service operation then close/exit; bad admin arguments must not start HTTP. Preserve offline-only H2 operation, common resolved DB path and once-only post-commit token display; do not document unverified command spellings as working. Refs: research R03/R07, quickstart administration. Deps: T023.
- [X] T025 Author `src/test/java/com/changeflow/integration/ProblemResponseTest.java` for stable ProblemDetail code/shape, JSON media negotiation, no SQL/stack traces/rejected secrets, 400 validation/controlled fields, and distinct 409/412/428/503 mappings using isolated test-only probes. Refs: HTTP contract Problem responses, FR-014/023. Deps: T024.
- [X] T026 Implement `src/main/java/com/changeflow/api/ProblemFactory.java` and `src/main/java/com/changeflow/api/ApiExceptionHandler.java` with RestControllerAdvice/ResponseEntityExceptionHandler, parsing/body/method validation handling, safe field messages, Application-failure mapping, optimistic flush/commit 412, lock/pool timeout STORAGE_BUSY and unavailable-storage STORAGE_UNAVAILABLE 503. Reuse the factory for security errors; never treat all persistence failures as version conflicts or claim rollback after uncertain commit acknowledgement. Refs: FR-012-015/023, research R10/R12, T025. Deps: T025.
- [X] T027 Implement `src/main/java/com/changeflow/configuration/JsonConfiguration.java`, `src/main/java/com/changeflow/api/validation/NormalizedText.java`, and `src/main/java/com/changeflow/api/validation/NormalizedTextValidator.java`: supported JavaTimeModule ISO-8601 UTC Instant handling, deterministic DTO property order, reject unknown/system-controlled fields with correct codes, normalize before Jakarta validation and count code points. No entity serialization, raw-value leakage, extra nullable-field framework or custom date formatter. Refs: FR-022-024, research R05. Deps: T026.
- [X] T028 Implement `src/main/java/com/changeflow/application/CredentialAuthenticationService.java`, `src/main/java/com/changeflow/infrastructure/security/BearerAuthenticationFilter.java`, `src/main/java/com/changeflow/api/SecurityProblemHandlers.java`, and `src/main/java/com/changeflow/configuration/SecurityConfiguration.java`: Authorization-only opaque verification, immutable caller IDs, STATELESS/no session request cache, no form/Basic/cookie/query credentials, matching JSON 401/403 and WWW-Authenticate: Bearer on authentication failures. Disable CSRF only for this explicit-header transport; map filter lookup outages/timeouts to matching 503 rather than 401 and register the filter only once. Refs: FR-010/015/016/019, T001, research R07/R08. Deps: T027.
- [X] T029 Create `src/test/java/com/changeflow/integration/support/HttpTestSupport.java` and `src/test/java/com/changeflow/integration/support/TestDatabaseConfiguration.java` for RANDOM_PORT Boot tests, Java 17 HttpClient, per-class/context isolated temporary file H2, generated credentials/users via the real admin service, deterministic seed requests, fresh committed-state snapshots and orderly pool/context cleanup. Helpers must support seeded states so stories can be tested independently; no shared EntityManager or test-thread rollback assumption for HTTP writes. Refs: constitution VI, research R13. Deps: T028.
- [X] T030 Execute foundation tests and bootstrap checks, extending `src/test/java/com/changeflow/integration/LocalAdministrationTest.java` and `src/test/java/com/changeflow/integration/ProblemResponseTest.java` for the completed command/security wiring as needed; verify no admin HTTP port, matching auth/storage problems, migrations before validation and actual timeout settings. Record real outcomes in `specs/001-change-request-management/validation.md`; fix failures in the responsible foundation files before proceeding. Refs: constitution VI/VII, T008/T009/T019/T022/T025. Deps: T029.

**Checkpoint**: Shared foundation is green. All four tables/mappings and safe storage,
credentials, revocation, security/error handling, and isolated test helpers exist.
The parallel foundation cohort is T012-T015 only, after T007/T011 are complete.

## Phase 3: Owner Lifecycle - User Story 1 (Priority: P1, MVP)

**Goal**: An authorized Requester creates, reads, edits and submits their own Draft.
**Independent Test**: Use real HTTP to create/edit/submit, compare ETags/content/ownership,
and inspect unchanged committed data after denial. Seed other identities/states privately.
Returned-request variants of shared scenarios are completed in US4, not silently omitted.

### Required tests before story implementation

- [X] T031 [P] [US1] Author `src/test/java/com/changeflow/integration/OwnerHttpContractTest.java` for POST/GET collection/GET item/PATCH/submit routes: 201 Location/ETag, 200 DTOs, ISO timestamps, required If-Match grammar, 400/403/404/409/412/428 and media errors. Use only completed helpers; write no shared test/support or production files. Refs: FR-003-005/010/013-016, HTTP contract, US1-AS01-05/08. Deps: T030.
- [X] T032 [P] [US1] Author `src/test/java/com/changeflow/integration/OwnerLifecycleHttpTest.java` for Draft creation/edit/submission, stable owner/content, unauthorized/non-owner failures and valid repeated creation producing distinct Drafts; inspect every stored field and decision count. Cover the Draft slice of US1-AS01-08 and US3-AS05-07. Refs: FR-003-005/012/014/015/021. Deps: T030.
- [X] T033 [P] [US1] Author `src/test/java/com/changeflow/integration/ContentValidationHttpTest.java` for Draft null/omission/blank/whitespace, partial edits, normalized exact/max+1 lengths, supplementary Unicode, forbidden server fields even matching, unknown fields and all-or-nothing mixed valid/invalid edits. Refs: FR-022/023, US1-AS06/09-16, SC-009/010. Deps: T030.
- [X] T034 [P] [US1] Author `src/test/java/com/changeflow/integration/NoOpPatchHttpTest.java` for empty-object/identical-content PATCH: normal permission and If-Match checks, 200 with unchanged timestamp/ETag, no manual version increment; compare write representation to later GET including nanoseconds. Refs: FR-013, research R10, HTTP no-op contract. Deps: T030.
- [X] T035 [P] [US1] Author `src/test/java/com/changeflow/integration/VisibilityHttpTest.java` for Requester/Reviewer/dual-role/neither across all four seeded states, identical authenticated missing/invisible 404, predicate-before-pagination (offset 0, limit 50, maximum 100), and uniform unauthenticated 401 under T001. Check reads never change storage and return no roles/credentials/entities. Refs: FR-001/014-016, US3-AS08-11, SC-007. Deps: T030.
- [X] T036 [US1] Author `src/test/java/com/changeflow/domain/OwnerLifecycleTest.java` for pure create/edit/submit state rules, immutable creator identity, normalized content and invalid Draft transitions without Spring or a database. Refs: FR-002-005/009/011/022, US1-AS01-03. Deps: T031, T032, T033, T034, T035.

### Implementation and integration

- [X] T037 [US1] Implement Draft creation/edit/submission in `src/main/java/com/changeflow/domain/ChangeRequest.java`, using T010 normalization and caller-independent Domain invariants; keep owned-request authorization in Application, preserve identity and submission content, reject disallowed states and all Approved mutations. Refs: FR-002-005/009/011/022, T036. Deps: T036.
- [X] T038 [US1] Implement `src/main/java/com/changeflow/api/dto/CreateRequest.java`, `src/main/java/com/changeflow/api/dto/EditRequest.java`, `src/main/java/com/changeflow/api/dto/ChangeRequestResponse.java`, `src/main/java/com/changeflow/api/dto/ChangeRequestListResponse.java`, and `src/main/java/com/changeflow/api/ChangeRequestDtoMapper.java` with presence-aware partial fields, normalized validated content and the exact response-only fields/order from the contract. No Domain/JPA objects as bodies; controlled fields must reject even when correct. Refs: FR-002-005/022/023, T031/T033. Deps: T037.
- [X] T039 [US1] Implement `src/main/java/com/changeflow/api/RequestEtags.java` for strong quoted "cr-{id}-v{revision}" tags and supported single-tag If-Match parsing: missing 428, malformed/unsupported wildcard/list 400, weak/nonmatching/cross-item 412. Pass a numeric expected revision to Application; action routes condition the parent request. Refs: FR-013, research R10, T031/T034. Deps: T038.
- [X] T040 [US1] Implement create/list/read/Draft edit/submit in `src/main/java/com/changeflow/application/ChangeRequestService.java`: public Spring transactions READ_COMMITTED/10-second timeout/rollbackFor=Exception.class; lock/recheck Credential then UserAccess, enforce visibility/owner/current Requester, compare expected revision before state, mutate/flush/copy immutable snapshot, and allow proxy commit before API success. Include read visibility, repeated-command policy and explicit optimistic no-op check; no self-invocation/manual transactions. Refs: FR-003-005/010-016/019/021-023, T032/T034/T035. Deps: T039.
- [X] T041 [US1] Implement POST/GET /api/change-requests, GET/PATCH /api/change-requests/{id}, and POST /api/change-requests/{id}/submit in `src/main/java/com/changeflow/api/ChangeRequestController.java` as thin RestController mappings to validated DTOs, trusted caller IDs, Application service and ETag/Location responses; require JSON only for nonempty bodies, protect item visibility before version/state disclosure and emit success only after the service proxy returns. Refs: FR-003-005/010/013-016/022/023, HTTP contract, T031. Deps: T040.
- [X] T042 [US1] Run the owner unit/HTTP/visibility/no-op suites, resolve failures in their owning files, and record actual red/green, stored-state and discovered-Test evidence in `specs/001-change-request-management/validation.md`. Verify the five owner/read routes and the Draft slice of US1-AS01-16; explicitly defer ChangesRequested variants to T053-T059. Refs: SC-001/003/006/007/009/010. Deps: T041.

**Checkpoint / MVP**: Safe authenticated Draft management and submission work without
review implementation. Do not call the full assignment accepted or skip later US3 work.
**Phase 3 batch disposition (2026-11-16)**: T031-T042 are complete; actual red/green,
commands, discovered tests and fresh stored-state evidence are recorded in
[validation.md](validation.md#phase-3-owner-lifecycle-validation-evidence).
Owner mutations currently support Draft only. ChangesRequested variants of shared
US1 scenarios (including the returned origin of AS08, partial/invalid/boundary edits
in AS10/12/13, and invalid-content submission in AS14), returned-state no-ops,
revision/resubmission and feedback retention remain deferred to T053-T059 as planned.
Read visibility across all four seeded states is covered here; that does not implement
returned-state mutations. T043 and every later task remain unstarted and unchecked.

## Phase 4: Independent Review - User Story 2 (Priority: P1)

**Goal**: An independent Reviewer approves or returns PendingReview with required feedback.
**Independent Test**: Seed PendingReview requests without calling the owner routes; verify
both actions, both self-review denials and atomic parent/feedback/metadata persistence.

### Required tests before story implementation

- [X] T043 [P] [US2] Author `src/test/java/com/changeflow/integration/ReviewHttpContractTest.java` for approve/request-changes DTOs, current If-Match, required explanation, success snapshots, 403 self-review, 409 fresh invalid state, 412 stale review and 428 missing precondition. Refs: FR-006-008/011/013/023/024, US2-AS01-07/11, HTTP contract. Deps: T042.
- [X] T044 [P] [US2] Author `src/test/java/com/changeflow/integration/IndependentReviewHttpTest.java` for independent and dual-role non-owner success, owner/dual-role self-approval and self-request-changes rejection, absent permissions, wrong state, retries and Approved immutability; inspect all parent fields, revision and metadata count after each rejection. Refs: FR-001/006-014/020/021, US2-AS01-07/11, US3-AS01/02/07. Deps: T042.
- [X] T045 [P] [US2] Author `src/test/java/com/changeflow/integration/ReviewFeedbackHttpTest.java` for required/non-null/nonblank trimmed feedback, inclusive 2,000/2,001 code-point boundary, authorized visibility and no historical explanation text in metadata; reject server fields without storing partial state/feedback. Refs: FR-007/014-016/023/024, US2-AS02, US2-AS12, US2-AS13, US2-AS15. Deps: T042.
- [X] T046 [US2] Author `src/test/java/com/changeflow/domain/ReviewRulesTest.java` for PendingReview-only decisions, owner self-review prohibition inputs, Approved immutability, required normalized feedback, preservation on approval and no invalid partial change. Refs: FR-006-009/011/024. Deps: T043, T044, T045.

### Implementation and integration

- [X] T047 [US2] Implement approve/request-changes Domain operations in `src/main/java/com/changeflow/domain/ChangeRequest.java`, using required explanation validation and preserving proposal/identity; approval preserves prior feedback and only successful request-changes replaces it. Keep permission/credential checks in Application and never allow either form of self-review. Refs: FR-006-009/011/024, T046. Deps: T046.
- [X] T048 [US2] Implement `src/main/java/com/changeflow/api/dto/RequestChangesRequest.java` with required non-null/nonblank normalized explanation, inclusive 2,000 code points and preserved internal whitespace; reject unknown/controlled fields, and keep Approve's optional body limited to an empty object. Refs: FR-023/024, T043/T045. Deps: T047.
- [X] T049 [US2] Add public transactional approve/requestChanges methods to `src/main/java/com/changeflow/application/ChangeRequestService.java`: repeat current credential/access checks, deny owner review regardless of roles, compare expected revision before state, mutate parent, flush for generated version, insert/flush metadata with no explanation text in the same transaction and return only through successful proxy commit. Preserve existing feedback when approval wins. Refs: FR-006-014/019/020/024, data model sections 5/7. Deps: T048.
- [X] T050 [US2] Add POST /api/change-requests/{id}/approve and POST /api/change-requests/{id}/request-changes to `src/main/java/com/changeflow/api/ChangeRequestController.java`, reusing T039 parent ETags and the shared DTO/problem mapping; no business rules in the controller and no history/publication/admin route. Refs: FR-006-008/013/024, T043. Deps: T049.
- [X] T051 [US2] Implement `src/test/java/com/changeflow/integration/ReviewAtomicityHttpTest.java` with file-local test-only failure injection after parent mutation/flush and after metadata insert, including checked failures and controlled commit failure: assert non-success plus unchanged committed state/feedback/version/decision count. Distinguish known rollback from uncertain commit acknowledgement; do not add a production fault endpoint. Refs: FR-012/014/024, US2-AS14, SC-005/011. Deps: T050.
- [X] T052 [US2] Run all independent-review/feedback/rollback suites, correct failures in their owning implementation files, and record HTTP plus fresh stored-state evidence in `specs/001-change-request-management/validation.md`. Cover US2-AS01-07 and US2-AS11-15; schedule true overlapping US2-AS08-10 in T061 and replacement-after-resubmission US2-AS16 in US4. Refs: SC-001/002/003/005/011. Deps: T051.

**Checkpoint**: Review is independently verifiable with seeded PendingReview. Every
successful review has one atomic metadata insert; rejected review has no stored effect.
**Phase 4 batch disposition (2026-11-16)**: T043-T052 are complete. The final full
regression has 128 passing tests with no failures/errors/skips; actual red/green,
rollback, fresh stored-state and source-snapshot evidence is in
[validation.md](validation.md#phase-4-independent-review-validation-evidence).
US2-AS08/09/10 genuinely overlapping review pairs and their required repetitions remain
T061. US2-AS16 replacement after actual revision/resubmission and the US4-AS05 feedback
retention journey remain US4 (T055/T059). Seeded prior-feedback review checks here do
not complete those journeys. T053 and all later tasks remain unstarted and unchecked.

## Phase 5: Revision/Resubmission - User Story 4 (Priority: P2)

**Goal**: Owner revises ChangesRequested and resubmits without changing identity/feedback.
**Independent Test**: Seed an owned ChangesRequested request and its latest explanation;
exercise existing PATCH/submit routes and independently inspect retention and rejection.
No new routes or controller-side state rules are required.

### Required tests before story implementation

- [X] T053 [P] [US4] Author `src/test/java/com/changeflow/integration/RevisionHttpContractTest.java` for the existing PATCH/submit routes against seeded ChangesRequested, exact DTO/ETag/error contract, null/omission and invalid content atomic rejection; include returned-state variants of US1-AS10/12/14. Refs: FR-004/005/010-014/022, US4-AS01-04. Deps: T052.
- [X] T054 [P] [US4] Author `src/test/java/com/changeflow/integration/RevisionResubmissionHttpTest.java` for owned revise/resubmit success, non-owner/dual-role rejection, invalid-state PendingReview edits and current permissions, preserving original request identity and requester. Refs: FR-004/005/010/011/014/019, US4-AS01-04. Deps: T052.
- [X] T055 [P] [US4] Author `src/test/java/com/changeflow/integration/FeedbackRetentionHttpTest.java` for feedback unchanged through edit/resubmit/approval and replaced only by the next successful request-changes decision, with metadata containing no prior explanation text and invalid operations preserving every field. Refs: FR-006/007/012/014/024, US2-AS16, US4-AS05, SC-011. Deps: T052.
- [X] T056 [US4] Author `src/test/java/com/changeflow/domain/RevisionRulesTest.java` for ChangesRequested content edits and return to PendingReview, unchanged explanation/owner and rejection in PendingReview/Approved. Refs: FR-004/005/009/011/024. Deps: T053, T054, T055.

### Implementation and integration

- [X] T057 [US4] Extend the existing edit/submit Domain methods in `src/main/java/com/changeflow/domain/ChangeRequest.java` to support ChangesRequested while preserving identity, reason/content validation, state on edit and latest feedback through resubmission; do not add a new lifecycle state. Refs: FR-004/005/009/011/022/024, T056. Deps: T056.
- [X] T058 [US4] Extend owned edit/submit paths in `src/main/java/com/changeflow/application/ChangeRequestService.java` to permit ChangesRequested under current Requester/owner/version checks, reusing public transaction/flush/snapshot boundaries; retain the same thin controller/routes and no feedback clearing by owner edits. Refs: FR-004/005/010-014/019/024, T053-T055. Deps: T057.
- [X] T059 [US4] Execute the revision/feedback-retention unit and HTTP suites plus the complete create-submit-return-revise-resubmit-approve journey; fix failures and record committed content/feedback/revision/decision evidence in `specs/001-change-request-management/validation.md`. Verify US4-AS01-05, US2-AS16 and all deferred ChangesRequested content cases. Refs: SC-001/003/009/011. Deps: T058.

**Checkpoint**: The full functional lifecycle and feedback loop are green. Shared
ChangeRequest.java, ChangeRequestService.java and controller edits have remained serial.
**Phase 5 batch disposition (2026-11-16)**: T053-T059 are complete. The Phase 4
checkpoint was reverified (128 passing tests); the final full regression has 161
passing tests with no failures/errors/skips. Actual red/green, fresh committed-state,
full lifecycle and source-snapshot evidence is in
[validation.md](validation.md#phase-5-revision-and-resubmission-validation-evidence).
Returned-content variants (US1-AS08/10/12/13/14), returned no-ops and the actual
feedback-retention/replacement journeys (US4-AS01-05 and US2-AS16) are now verified.
The earlier batch deferral notes are historical. Genuine overlapping review cases
remain T061. Stop before T060; all US3 hardening and later tasks remain unchecked.

## Phase 6: Concurrency/Security Hardening - User Story 3 (Priority: P1)

**Goal**: Prove already-required security, atomicity and concurrency guarantees under real
failure/race conditions. All work is required before assignment acceptance.
**Independent Test**: Seed requests/users and exercise real HTTP plus separate transactional
administration/storage workers; check all committed data after both workers finish.

- [X] T060 [US3] Create `src/test/java/com/changeflow/integration/support/ConcurrencyTestConfiguration.java` to coordinate test-only barriers around completed request loads/expected-version checks and credential/access locks using existing ports, with separate threads/contexts and bounded latch waits. Keep hooks out of public routes and production behavior; test classes consume this completed helper without editing it in parallel. Refs: FR-012/013/019/020, research R09/R10/R13. Deps: T059.
- [X] T061 [P] [US3] Implement `src/test/java/com/changeflow/integration/ReviewConcurrencyHttpTest.java`: two distinct Reviewers with distinct Credential/UserAccess rows, genuinely overlapping transactions/EntityManagers after both load the same version, approve/approve, changes/changes and mixed pairs repeated at least 100 times each. Require one 2xx and one 412, one metadata insert, winning state/feedback/version, and no losing explanation. Refs: FR-012-014/020/024, US2-AS08-10, SC-004/011. Deps: T060.
- [X] T062 [P] [US3] Implement `src/test/java/com/changeflow/integration/OwnerConcurrencyHttpTest.java` for edit/edit and edit/submit repeated at least 100 times each with separate HTTP workers and contexts; include multiple credentials for the owner, deliberate UserAccess serialization and stale preconditions. Assert no silent loss, incompatible successes, post-submission edit or rollback of a successful competitor. Refs: FR-011-014, US3-AS03-04, SC-004. Deps: T060.
- [X] T063 [P] [US3] Implement `src/test/java/com/changeflow/integration/RevocationConcurrencyHttpTest.java` racing the real Spring-proxied admin service with HTTP create/edit/submit/review for credential revocation and role removal in both lock-winning orders; test remaining-role access and no ownership exception. Inspect request/metadata/access/credential state; already completed writes survive and revoked-before-completion writes reject atomically. Do not pretend the offline CLI supports a second live H2 JVM. Refs: FR-010/012/014/019/021, US3-AS14-18, SC-008. Deps: T060.
- [X] T064 [P] [US3] Implement `src/test/java/com/changeflow/integration/StorageFailureHttpTest.java` with per-test lock holds and pool/storage faults: measure H2 2000-ms lock bound, 3000-ms acquisition and 10-second transaction policy; assert distinct 503 STORAGE_BUSY/STORAGE_UNAVAILABLE, never 412 or invalid-credential 401 for outages, in both MVC and filter paths. Inspect storage after recovery; distinguish controlled rollback from unknown commit acknowledgement. Refs: FR-012-014, HTTP contract Problem responses. Deps: T060.
- [X] T065 [P] [US3] Implement `src/test/java/com/changeflow/integration/AuthenticationSecurityHttpTest.java` for missing/malformed/invalid/revoked credentials, JSON 401 plus Bearer challenge, visible 403, absent Basic/form/cookie/query fallback, no session auth, role-header spoofing and bearer-specific CSRF behavior; confirm no token/hash/SQL leakage and no public signup/admin/issuer/introspection route. Refs: FR-010/014-016/019/023, US3-AS02/07/19, T001, research R07/R08. Deps: T060.
- [X] T066 [P] [US3] Implement `src/test/java/com/changeflow/integration/OptimisticPersistenceTest.java` to prove provider-level guarded updates and explicit OPTIMISTIC no-op checks using distinct managed persistence contexts, including conflict at flush/commit and immutable response snapshot versus a later writer. Any direct test-owned storage race must use managed @Version entities, never manual version assignment; supplement rather than replace real HTTP review races. Refs: FR-012/013, research R10. Deps: T060.
- [X] T067 [P] [US3] Implement `src/test/java/com/changeflow/integration/LifecycleRollbackHttpTest.java` for controlled creation/edit/submission failures after work begins, runtime and checked error paths, and no false success before proxy commit. Use file-local test configuration; inspect fresh request/version/feedback/decision data and never infer rollback solely from a lost response. Refs: FR-012/014, US3-AS05, SC-005. Deps: T060.
- [X] T068 [P] [US3] Implement `src/test/java/com/changeflow/integration/ForbiddenOperationHttpTest.java` across all states/role combinations for Approved mutation, deletion, cancellation/withdrawal, execution/publication, current-version invalid actions and old-version retries; require documented 403/404/405/409/412 outcomes as applicable with no stored effects. Refs: FR-008/009/011/014/017/018/021, US3-AS01/12/13, SC-002/003/006. Deps: T060.
- [X] T069 [P] [US3] Implement `src/test/java/com/changeflow/integration/VisibilitySecurityHttpTest.java` for current server-side read permission after role removal, dual-role union, missing/invisible equivalence, latest-feedback privacy, pagination-before-filter correctness and unauthenticated 401 after T001; inspect unchanged data on reads/denials. Refs: FR-014-016/019/024, US3-AS06/08-11/14/16, SC-007/008/011. Deps: T060.
- [X] T070 [US3] Audit and correct concurrency/security guarantees in `src/main/java/com/changeflow/application/ChangeRequestService.java`, `src/main/java/com/changeflow/infrastructure/persistence/JpaCurrentAccessGuard.java`, `src/main/java/com/changeflow/infrastructure/persistence/JpaChangeRequestStore.java`, `src/main/java/com/changeflow/infrastructure/security/BearerAuthenticationFilter.java`, and `src/main/java/com/changeflow/api/ApiExceptionHandler.java` against the T061-T069 failures and reviewed protocol. Prove current access rechecks/lock order, rollbackFor, no shared contexts/self-invocation/manual commits/bulk updates, flush-versus-commit response ordering, no-op locking and exact conflict/storage classification; make only required fixes and record evidence in `specs/001-change-request-management/validation.md`. Refs: FR-010-014/019/020. Deps: T061, T062, T063, T064, T065, T066, T067, T068, T069.
- [X] T071 [US3] Implement `src/test/java/com/changeflow/integration/RestartPersistenceTest.java`: close the first app/pool, reopen the exact same temporary file in a second context, and verify all request fields/revisions/feedback, metadata, credential hashes/revocation, roles and Flyway history survive without reset or seeded secrets. Verify nanosecond representation consistency and separate H2-only evidence from any PostgreSQL claim. Refs: FR-002/012/019/024, research R03/R06/R13. Deps: T070.
- [X] T072 [US3] Run all US3 suites including every required repetition and restart, fix failures and append real commands, statuses, measured waits, transaction outcomes and persisted-state comparisons to `specs/001-change-request-management/validation.md`. Account for US3-AS01-19 and US2-AS08-10; no mocked/sequential-only substitute for the true HTTP optimistic race. Refs: SC-002-008/010/011. Deps: T071.

**Checkpoint**: Critical rules, negative paths, concurrency, revocation and restart are
verified through actual HTTP/transactions and persisted evidence. No claim of PostgreSQL
compatibility, production readiness or completed acceptance is inferred from H2 alone.
**Phase 6 batch disposition (2026-11-16)**: T060-T072 are complete. The final full
Maven run has 801 passing tests across 30 suites, with no failures/errors/skips.
It includes all 500 required pair repetitions (100 each), distinct-context optimistic
review races, owner serialization, both revocation orders, measured storage waits,
rollback/no-op/authentication/visibility checks and same-file restart persistence.
Actual commands, failure/correction history, per-pair completion counters, measured
waits and committed-state/source evidence are recorded in
[validation.md](validation.md#phase-6-concurrency-and-security-hardening-evidence).
The observed provider transaction-deadline mapping was corrected to STORAGE_BUSY;
no required verification in this batch remains blocked or reduced. Stop before T073.
All nine Phase 7 tasks remain unstarted and unchecked; final acceptance is not claimed.

## Phase 7: Validation and Final Documentation

**Purpose**: Consolidate required evidence, verify the executable distribution and publish
only real local instructions. These tasks contain no new product functionality.

- [X] T073 Audit `pom.xml` and `src/main/java/com/changeflow/` for Java 17 release/API use, pinned Boot/BOM/unmanaged versions, permitted package imports/constructor boundaries, no Domain Spring/JPA, no HTTP/JPA Application contracts, no extra frameworks and no secrets; record actual effective versions and findings in `specs/001-change-request-management/validation.md` rather than overwriting published expected versions as if already resolved. Refs: constitution II/III/VII/VIII, research R01/R04. Deps: T072.
- [X] T074 Execute the complete required Maven test lifecycle now that POM/source exist, inspect Surefire discovery/results for every unit and real-HTTP *Test class, and record actual counts/failures/skips/commands/revision in `specs/001-change-request-management/validation.md`. Do not exclude stress/integration tests or mark unexecuted cases passed; resolve failures before advancing. Refs: constitution VI/VII, research R13, SC-001-011. Deps: T073.
- [X] T075 Determine and execute the actual packaging/start commands from `pom.xml`, verify `target/changeflow.jar` repackage/main class and normal loopback startup/migration/validation, then graceful stop/restart of its file database; record observed outcomes and command prerequisites in `specs/001-change-request-management/validation.md`. This is post-package executable validation, not a Surefire test that assumes a JAR exists before mvn test. Refs: research R01/R03/R06, quickstart packaging. Deps: T074.
- [X] T076 Verify actual non-web provisioning/revocation command syntax against `src/main/java/com/changeflow/configuration/LocalAdministrationRunner.java` and the packaged `target/changeflow.jar` while HTTP is stopped; demonstrate one operation/exit, no HTTP bind, correct DB path, once-only post-commit credential display and effect after HTTP restart. Keep secrets out of recorded output and document embedded-file/offline limitations in `specs/001-change-request-management/validation.md`. Refs: FR-010/019, research R03/R07. Deps: T075.
- [X] T077 Replace provisional entries in `specs/001-change-request-management/quickstart.md` with exact verified setup/build/test/start/admin/restart commands, actual working directory, external configuration prerequisites, recorded outcomes and remaining limitations. Preserve HTTP contract/data-model links; do not copy implementation bodies or publish credentials. Refs: constitution VII, quickstart Evidence section. Deps: T076.
- [X] T078 Create/update `README.md` with the implemented backend purpose, source-of-truth links, package responsibilities, local setup and verified validation/admin instructions, approved lifecycle boundaries and offline H2/Boot-support limitations; distinguish demonstration behavior from future scope and no PostgreSQL portability claim. Refs: constitution I/VIII, research R01/R03. Deps: T077.
- [X] T079 Update root `AGENTS.md` after actual verification: replace the initial absent-POM/source note with confirmed Maven build/test commands, working directory and prerequisites; retain concise source-of-truth/scope/dependency/secret/evidence guidance and avoid duplicating constitution/spec. Refs: root guidance, constitution VII. Deps: T078.
- [X] T080 Reconcile delivered design/evidence in `specs/001-change-request-management/spec.md`, `specs/001-change-request-management/plan.md`, `specs/001-change-request-management/research.md`, `specs/001-change-request-management/data-model.md`, and `specs/001-change-request-management/contracts/http-api.md`; preserve stable IDs/actual answers, record justified implementation details and real versions, and distinguish historical planning notes from current state. Do not silently change product behavior or amend the constitution. Refs: constitution I/IX. Deps: T079.
- [X] T081 Complete the FR-001-FR-024 / SC-001-SC-011 / acceptance-scenario evidence matrix and final secret-free review disposition in `specs/001-change-request-management/validation.md`; verify document links, actual test results, immutable/authorization/atomicity guarantees and unresolved failures. Mark tasks in `specs/001-change-request-management/tasks.md` complete only with real completion evidence; list skipped/blocked work and do not claim acceptance if required checks remain. Refs: constitution I/VI/VII/IX. Deps: T080.

**Checkpoint**: Required tests, packaged startup/admin/restart and actual user instructions
are verified; traceability covers delivered behavior and remaining failures honestly.

### Phase 7 execution disposition

T073-T081 completed sequentially without additional agents. Entry Phase 6 hashes/results
matched; fresh full Maven run passed 801 tests across all 30 suites, no failures/errors/
skips, including 100 completed repetitions per required race pair. Package then separate
JVM startup/migration/graceful restart, actual offline administration and full lifecycle
passed with fresh persisted data inspection. Final matrix covers 24 FR + 11 SC + 56
scenarios and verified local links. Only documentation/guidance/evidence changed in this
phase; source/POM/constitution and approved behavior are unchanged. No required Phase 7
verification is blocked or reduced. See validation.md for exact commands, counts, resolved
packaging/helper issues and remaining demonstration limits. Stop after this phase.
Converge has not run; Step 13 must invoke it separately on current code.

## Dependencies and Execution Order

### Phase and story graph

```text
Setup T001-T006
  -> Foundation T007-T030 (schema T007 precedes every database integration task)
  -> US1 owner lifecycle T031-T042
  -> US2 independent review T043-T052
  -> US4 revision/resubmission T053-T059
  -> US3 concurrency/security hardening T060-T072
  -> Validation/docs T073-T081
```

US2 accepts seeded PendingReview and US4 seeded ChangesRequested for independent tests;
implementation is still serialized after earlier stories because they extend the same
Domain aggregate, service and controller. US3 can seed every state for isolated failure
tests. Story independence describes verification, not permission to edit shared files
concurrently or deliver a lifecycle without its required security baseline.

### Within each story

Tests are authored first; Domain changes precede service orchestration, which precedes
thin endpoint integration. DTO/ETag prerequisites precede controllers. Foundation schema,
entities, ports, security and helpers must be complete before story HTTP tests execute.
All writes retain public proxy transactions and current-access/version checks from day one.
Shared Java/config/evidence files are edited serially. No dependency points to a later ID.

### Parallel opportunities

These are the only `[P]` cohorts. Finish each cohort before the next unmarked task.
Task authors may read already-completed helpers/contracts; they must not change them.
If a shared helper needs correction, stop the cohort and make that correction serially.
Parallel test authoring does not require enabling parallel Surefire execution.

| Cohort | Ready after | Disjoint writes |
| --- | --- | --- |
| T012-T015 | T007 and T011 | Four separate entity/repository file pairs, scalar UUID associations |
| T031-T035, US1 | T030 | Five separate HTTP Test files |
| T043-T045, US2 | T042 | Three separate review Test files |
| T053-T055, US4 | T052 | Three separate revision/retention Test files |
| T061-T069, US3 | T060 | Nine separate hardening Test files; shared helpers read-only |

## Parallel Examples per User Story

- US1: after T030, T031 OwnerHttpContractTest and T032 OwnerLifecycleHttpTest can be
  authored together; each writes its own file and reads the completed T029 helpers.
- US2: after T042, T043 ReviewHttpContractTest and T045 ReviewFeedbackHttpTest can run
  as independent authoring tasks. T049/T050 shared service/controller edits stay serial.
- US4: after T052, T053 RevisionHttpContractTest and T055 FeedbackRetentionHttpTest can
  be authored together. T057/T058 touch shared production files and are not parallel.
- US3: after T060, T061 ReviewConcurrencyHttpTest and T063 RevocationConcurrencyHttpTest
  can be authored independently with isolated contexts; neither edits the completed
  barrier helper or the same source/evidence file.

## Requirement and Contract Coverage

| Requirement / contract area | Main work and proof |
| --- | --- |
| FR-001-FR-003, identity/roles/creation | T010-T024, T031-T032, T035-T041 |
| FR-004-FR-005, edit/submit/resubmit | T033-T042, T053-T059 |
| FR-006-FR-008, independent review and self-review rejection | T043-T052, T055, T061 |
| FR-009, Approved immutability | T037, T044, T047, T068 |
| FR-010-FR-011, current authorization/state | T021-T028, T040, T049, T058, T063/T065/T070 |
| FR-012-FR-014, atomicity/concurrency/rejection | T018/T026, T040, T049/T051, T060-T072 |
| FR-015-FR-016, missing/visibility | T001, T018/T028, T035/T040/T041, T045, T069 |
| FR-017-FR-018, no delete/cancel/withdraw | T041/T050 route boundaries, T068 |
| FR-019, provisioning/revocation/current roles | T012-T024/T028, T063/T065/T069-T072, T076 |
| FR-020, competing review | T014/T018/T039/T049, T060-T061/T066/T070 |
| FR-021, duplicate-command policy | T032/T040, T043-T044/T049, T063/T068 |
| FR-022, content/partial validation | T009-T011/T027, T033/T037/T038, T053/T057/T058 |
| FR-023, controlled fields | T026-T028/T033/T038/T041, T043/T048/T050/T065 |
| FR-024, latest feedback and metadata-only decision insert | T007/T011/T014-T018, T045-T052, T055-T059/T061/T071 |
| All seven HTTP routes / errors / headers | T025-T028, T031/T039-T041, T043/T048-T050, T053/T058, T064-T069 |
| Flyway, persistent restart, executable/admin mode | T005/T007-T008/T012-T024, T030/T071, T075-T077 |
| Actual test discovery, build evidence, final docs | T006/T030/T042/T052/T059/T072, T073-T081 |

Each task's detailed references and the spec's Acceptance Traceability retain the exact
scenario IDs. T081 must link every scenario to real evidence, not merely a test filename.

## Implementation Strategy

### MVP first

Complete Setup and Foundation, then US1 through T042. Demonstrate safe Draft preparation
and submission with actual tests/evidence. The MVP does not waive authorization, current
access locks, @Version/If-Match, validation, or secret handling. It is not full acceptance.

### Incremental delivery

Add US2 and verify independent review, add US4 and verify the complete feedback loop,
then finish all US3 hardening/race/restart verification and final distribution/docs.
Use focused test runs during implementation and the complete regression at T074.
Fix observed failures before checking off phase gates; do not broaden scope to hide gaps.

### Completion reporting

At generation there were 81 unchecked tasks: Setup 6, Foundation 24, US1 12, US2 10,
US4 7, US3 13, and Validation/docs 9, with 24 `[P]` tasks in five cohorts. Later phase
dispositions above are chronological checkpoint records, not assertions that subsequent
completed phases are still unstarted. Current final disposition follows T081 below;
the original task IDs, prerequisites and requirements remain unchanged.









## Phase 8: Convergence

Assessment: 2026-11-16, `$speckit-converge`, current implementation against all 24 FRs,
11 SCs, 56 acceptance scenarios, 81 existing tasks, 13 technical decision areas and nine
constitution principles. Fresh full Maven `test` run: 801 cases / 30 suites, zero
failures/errors/skips (including all five 100-repetition cohorts), BUILD SUCCESS at
22:40:01 +03:30. Additional in-memory JShell probes ran the actual Spring application,
real HTTP and isolated Flyway-migrated file H2; generated credentials stayed private.
The green suite did not cover the two reproduced gaps below. Existing tasks and source
are preserved; these tasks are unimplemented. No constitution MUST violation, missing
feature or unrequested runtime addition was identified in this assessment.

- [X] T082 HIGH — Close F1's hidden-record existence oracle before stored-content validation: author real-HTTP regression cases using the already in-scope invalid-stored-content fixtures for inaccessible Draft/non-Draft rows, current Requester/Reviewer/removed-role visibility and valid mutation bodies/If-Match; require the same 404 NOT_FOUND as missing items for read/edit/submit/review, no feedback/existence disclosure and no changes to any parent field/version/decision. Then correct access-aware lookup/rehydration ordering in `src/main/java/com/changeflow/application/ChangeRequestService.java`, `src/main/java/com/changeflow/application/port/ChangeRequestStore.java` and `src/main/java/com/changeflow/infrastructure/persistence/JpaChangeRequestStore.java`/`ChangeRequestMapper.java` as needed, retaining independent Domain invariants, current-access checks, optimistic transactions and authorized invalid-content submission rejection without repair. Evidence: `visible()` calls `requests.find()` before its visibility decision; `find()` constructs a Domain value and the mapper raises INVALID_CONTENT first. A valid other-owner Draft and a missing ID returned 404; after setting that Draft's stored title to blank, hidden GET/PATCH/submit/approve/request-changes returned 400 INVALID_CONTENT. The authorized owner's invalid submission remained 400; all parent columns and zero decision rows stayed unchanged. Re-run owner/revision/visibility/security suites and update actual evidence in `specs/001-change-request-management/validation.md`. Refs: FR-015/016, SC-007, US3-AS06/11, US2-AS15, US1-AS14, plan: access-before-Domain-validation protocol, T035/T069/T081 (partial). Deps: T081.
- [X] T083 MEDIUM — Close F2's strong-validator encoding mismatch: author real-HTTP byte-level negotiation regressions for item GET and successful write responses using default JSON and UTF-16LE/UTF-16BE Accept charset variants, then constrain JSON response encoding/negotiation in `src/main/java/com/changeflow/configuration/JsonConfiguration.java` and the relevant API composition so different representation octets never share the same strong ETag. Preserve the reviewed item-specific `"cr-{id}-v{revision}"` and parent If-Match contract, deterministic DTOs, no-op representation identity, JSON problem responses and existing supported routes; prefer one canonical JSON encoding or the existing 406 outcome for incompatible negotiation rather than inventing per-encoding revision/tag formats. Evidence: one unchanged item returned 200 application/json with 307 bytes and 200 application/json;charset=UTF-16LE / UTF-16BE with 614 bytes each, all carrying exactly the same strong ETag; parent revision stayed unchanged. Verify response bytes/content types and If-Match/no-op behavior, run the full required Maven suite including every race repetition, and update actual evidence in `specs/001-change-request-management/validation.md`. Refs: plan: deterministic representation/strong ETag transport decision, HTTP contract DTO/normalization and item headers, FR-013, T031/T034/T039/T074/T081 (partial). Deps: T082.

### Phase 8 implementation disposition

T082-T083 completed sequentially, tests before behavior, without additional agents. T082:
51 new cases initially produced 45 expected 404-vs-400 failures, then 141 focused cases
passed. T083: after correcting a test-helper compilation issue, 18 cases produced 13
real encoding/negotiation failures, then all 68 focused cases passed. Full current run:
870 cases / 32 suites, zero failures/errors/skips, all five required 100-repetition
cohorts and restart/rollback/revocation suites included. Repackaged the current JAR only
after full tests and passed a targeted real-JAR provisioning/UTF-8/fallback/406/no-op smoke
with graceful closure. See Phase 8 validation.md for exact commands, times, hashes,
stored-state evidence and resolved authoring issues. POM/schema/Domain rules/constitution
and stable FR/SC/scenario blocks are unchanged. Both convergence findings are corrected;
a follow-up Converge remains a separate assessment, not an implicit implementation result.
