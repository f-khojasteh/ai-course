# ChangeFlow

A Java 17 / Spring Boot 3.5.16 backend for owned change proposals and independent review.
Requesters prepare Drafts and submit them; independent Reviewers approve or return work
with required feedback. Returned work uses the same edit/submit routes. Approved content
is immutable, and latest feedback survives revision, resubmission and approval.

The [constitution](.specify/memory/constitution.md) governs the project. The active
[specification](specs/001-change-request-management/spec.md),
[plan](specs/001-change-request-management/plan.md),
[ordered tasks](specs/001-change-request-management/tasks.md),
[data model](specs/001-change-request-management/data-model.md) and
[HTTP contract](specs/001-change-request-management/contracts/http-api.md) define behavior
and design. [Validation](specs/001-change-request-management/validation.md#phase-7-validation-and-final-documentation-evidence)
records the executed evidence; historical planning notes are not passing-test claims.

## Local setup

Use JDK 17, Maven 3.6.3+, a writable private file database and loopback port 8080.
Supply database URL/username/password through private external environment configuration;
there are no default accounts, passwords or API tokens. The default database URL is
`jdbc:h2:file:./data/changeflow`. Use an absolute URL shared by normal and admin mode.

Start with the [verified quickstart](specs/001-change-request-management/quickstart.md).
It gives the exact working directory, machine-specific Maven cache/mirror commands,
full test and packaging commands, executable startup, reproducible graceful shutdown,
restart and actual administrative syntax. Maven lifecycle goals are `test` and `package`.
After packaging, `java -jar target/changeflow.jar --spring.application.admin.enabled=true`
starts the demonstrated local service and enables its local management shutdown MBean.
Flyway creates the schema; Hibernate validates it. No H2 console or manual seed is used.

Provision identities or issue/revoke credentials and remove roles through the packaged
non-web CLI while HTTP is stopped. Each invocation performs one operation and exits.
A successful provision/issue displays its random opaque bearer credential once after
commit. Store it privately; only its digest is persisted. There is no administrative HTTP
endpoint. The plain embedded H2 file cannot be opened by both JVMs at once.

## Architecture

One Maven module keeps these responsibilities separate:

| Package | Responsibility |
| --- | --- |
| `domain` | Framework-independent immutable values, lifecycle and content rules |
| `application` | Use cases, current access/version checks, atomic transaction boundaries and ports |
| `infrastructure` | JPA mappings/adapters, persistent credentials and secure opaque-token codec |
| `api` | Thin MVC transport, presence-aware DTOs, ETags/preconditions and safe problem responses |
| `configuration` | Constructor wiring, stateless bearer security and normal/non-web startup |

Domain imports Java only. Application uses Domain and transactional annotations, with no
HTTP/JPA contracts. Infrastructure implements application ports; API invokes Application;
Configuration composes the packages. No persistence entity is returned to HTTP.

Sensitive writes recheck current credentials and permissions under a defined lock order,
then enforce item-specific If-Match against the managed optimistic version. Parent state,
latest feedback, revision and metadata-only review insert commit atomically. Empty or
normalized-identical PATCH still checks current access/state/version while preserving
representation/timestamp/revision. Read visibility is applied before pagination.

## Verified results and limits

Phase 7 ran **801 tests in all 30 suites: zero failures, errors or skips**. This includes
100 repetitions of each of five real overlapping HTTP race pairs, both revocation orders,
controlled rollback/commit failures, no-op, authentication, visibility, bounded storage
failures and file restart. The packaged JAR also completed the full return/revise/resubmit/
independent-approve loop, graceful separate-JVM restart and actual offline administration.
The [complete evidence matrix](specs/001-change-request-management/validation.md#t081--final-evidence-matrix-and-disposition)
links every FR, success criterion and scenario to actual executed checks.

Phase 8 corrected the two subsequent convergence findings: hidden invalid stored content
now stays indistinguishable from missing data, and successful JSON uses canonical UTF-8
with incompatible charset negotiation rejected before mutation. Its full run passed
**870 tests in 32 suites, zero failures/errors/skips**, including every required race
repetition. [Correction evidence](specs/001-change-request-management/validation.md#phase-8-convergence-corrections-evidence)
includes meaningful red/green results and fresh stored-state/byte-level checks.

This is a local backend demonstration. It has no frontend, delete/cancel/withdraw,
publication/execution, signup, public administration or historical feedback API/storage.
The selected latest-feedback policy and scope remain unchanged. H2 verification does not
establish PostgreSQL portability, public-production readiness or multi-process H2 operation.
The pinned Boot generation's reviewed support caveat remains; reassess before deployment.
Lost confirmation may follow a completed commit: read current data after recovery instead
of automatically retrying writes or assuming rollback. No throughput/latency SLO is claimed.

T001–T083 are implemented/verified as recorded in tasks and validation. Step 13's Converge
identified the two corrected gaps; a follow-up Converge must assess the corrected code
separately. This report is not an external security audit or the project owner's acceptance.
