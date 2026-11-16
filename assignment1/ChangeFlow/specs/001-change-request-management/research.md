# ChangeFlow Design Research

Date: 2026-11-16. Inputs: [reviewed specification](spec.md),
[constitution](../../.specify/memory/constitution.md), and the user's planning instruction.
Original planning research used official documentation and published artifacts before
application implementation. This record now distinguishes those published expectations
from the later actual evidence in [validation.md](validation.md#phase-7-validation-and-final-documentation-evidence).

## R01 - Runtime, dependencies, and packaging

- **Decision:** One Maven module, JDK 17, Spring Boot parent/BOM 3.5.16, explicit
  `java.version=17` and `maven.compiler.release=17`. Use Spring MVC, Security, Data JPA,
  Validation, Jackson Java Time, Flyway core, H2, and JUnit Jupiter. Declare the managed
  Boot Maven plugin with its inherited `repackage` execution, main class
  `com.changeflow.ChangeFlowApplication`, and `finalName=changeflow`.
- **Reason:** Honors the requested stack and produces one executable archive with an
  embedded HTTP server. Boot 3.5.16 is published and supports Java 17 and Maven 3.6.3+.
- **Alternatives:** Boot 4, independently versioned Spring libraries, multiple modules,
  and an external servlet container add migration or operational scope without need.
- **Limitations:** The pinned generation has ended OSS support. Retain the user's pin;
  reassess support before deployment beyond this local demonstration. No preview features,
  virtual threads, or post-Java-17 APIs. Actual effective-POM/dependency-tree versions
  were captured in T073; the table below retains the original published management evidence.
  Actual resolved/effective versions are recorded separately in validation.md and match
  those expectations. No new vendor-support certification was performed in this phase.

| Component | Published managed version expected from the pin |
| --- | --- |
| Boot parent, starters, repackage plugin | 3.5.16 |
| Spring Framework / MVC | 6.2.19 |
| Spring Security | 6.5.11 |
| Spring Data BOM / JPA | 2025.0.13 / 3.5.13 |
| Hibernate ORM / Validator | 6.6.53.Final / 8.0.3.Final |
| Jackson BOM, core, databind, jsr310 | 2.21.4 |
| Jackson annotations | 2.21 |
| Flyway / H2 | 11.7.2 / 2.3.232 |
| JUnit Jupiter / Platform | 5.12.2 / 1.12.2 |
| Maven Compiler / Surefire plugins | 3.14.1 / 3.5.6 |

Use managed versions without independent overrides. Any actually needed unmanaged plugin
or dependency must receive an explicit version in the POM; none was needed by this delivery.
Published parent/BOM management also governs packaging lifecycle plugins. Verify the
effective versions rather than copying a floating dependency list into the build.

Sources: [release and support status](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/),
[system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html),
[managed coordinates](https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html),
[published BOM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/3.5.16/spring-boot-dependencies-3.5.16.pom),
[parent POM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/3.5.16/spring-boot-starter-parent-3.5.16.pom),
[Maven packaging](https://docs.spring.io/spring-boot/3.5/maven-plugin/packaging.html).

## R02 - Authentication errors and the reviewed specification

- **Decision:** Apply the newer explicit planning instruction: missing, malformed,
  invalid, or revoked bearer credentials produce JSON 401 with
  `WWW-Authenticate: Bearer` before protected-resource lookup. A valid authenticated
  caller receives identical 404 outcomes for missing and invisible requests; forbidden
  actions on visible requests produce 403.
- **Reason:** Authentication failure handlers and resource visibility have different
  responsibilities. Uniform 401 reveals no request existence.
- **Alternatives:** Returning 404 for unauthenticated item reads would literally match
  US3-AS11 but contradict the newer mandatory 401 instruction.
- **Explicit refinement and resolution:** At planning, US3-AS11 and FR-015 still included
  unauthenticated callers in their not-found wording. T001 synchronized the affected text
  before implementation under the later user directive, preserving actual answer history.
  Current spec, API and authentication tests agree on 401 before lookup and authenticated
  missing/invisible 404. No other OPEN-001 visibility decision changed.

Sources: [Security architecture](https://docs.spring.io/spring-security/reference/6.5/servlet/architecture.html),
[Bearer authentication](https://www.rfc-editor.org/rfc/rfc6750.html#section-3).

## R03 - Local storage and administrative operating mode

- **Decision:** File-backed embedded H2, default URL `jdbc:h2:file:./data/changeflow`.
  The separately invoked non-web administrative mode is **offline-only**: stop the HTTP
  process and close its pool before opening that file from the admin process. Use the
  same configured absolute database location in both modes. Default HTTP bind is
  `127.0.0.1:8080`; datasource path, bind, and port are overridable.
- **Reason:** Keeps the exact requested local default and avoids a second database
  listener. Provisioning and revocation are available through a local administrative
  application service and an explicitly invoked non-web command mode.
- **Alternatives:** SQLite is local but has a single-writer constraint and adds dialect
  integration work. PostgreSQL is appropriate for a later server deployment but adds
  an administered service. H2 automatic mixed mode could support live separate-process
  administration; it adds listener, connection-security, and lifecycle responsibilities.
- **Limitations:** Embedded H2 permits one owning JVM/classloader. Do not advertise live
  separate-process administration with the default URL. Concurrent revocation/write
  guarantees are still tested by invoking the same transactional administrative service
  inside the running test JVM. Online CLI administration is deferred, not silently
  enabled through `AUTO_SERVER`. H2 tests prove neither PostgreSQL locking nor its SQL
  migration behavior. File access and backups remain local operational responsibilities.

Sources: [H2 connection modes](https://h2database.com/html/features.html#connection_modes),
[SQLite suitability](https://www.sqlite.org/whentouse.html),
[PostgreSQL concurrency](https://www.postgresql.org/docs/current/mvcc-intro.html).

## R04 - Architecture and dependency direction

- **Decision:** Packages `domain`, `application`, `infrastructure`, `api`, and
  `configuration` under `com.changeflow`; one main class at that root. Domain owns pure
  rules and has no Spring/JPA annotations. Application owns use cases, authorization
  decisions, and narrow storage/access contracts. Infrastructure maps Domain objects to
  JPA entities and implements those contracts. API maps DTOs and HTTP semantics;
  configuration wires constructors and security.
- **Reason:** Enforces current responsibility boundaries without separate deployables.
  Public Application services may use Spring transaction annotations; they expose no
  HTTP or JPA types and can be constructed directly in unit tests.
- **Alternatives:** A transaction decorator could remove Spring annotations from
  Application, but adds indirection not requested here. Generic repositories and mapping
  frameworks add abstraction without a concrete use case.
- **Limitations:** Package separation alone is not enforcement; review imports and
  dependency direction during implementation. No circular imports. No JPA entity or
  Domain object is an HTTP body. Application contracts serve current operations only.

## R05 - DTOs, validation, and time

- **Decision:** Separate create, presence-aware partial-edit, request-changes, and response
  DTOs. Jakarta Bean Validation validates supplied fields after normalization; Domain
  independently enforces all business invariants. Reject server-controlled write fields
  and unknown JSON properties. Use Java 17 `String.strip()` for outer whitespace and
  Unicode code-point counts for character limits, preserving internal text. Use `Instant`
  and Boot-managed Jackson Java-time support with textual ISO-8601 UTC output.
- **Reason:** Distinguishing omission from explicit null implements FR-022 without a new
  nullable-field framework. Raw `@Size` before trimming would reject valid padded text.
- **Alternatives:** Exposing entities, raw maps, or nullable strings without presence
  tracking loses boundaries or null/omission information. Manual date serializers and
  additional mapping libraries are unnecessary.
- **Limitations:** Code points are not grapheme clusters; document and test supplementary
  characters. `strip()` follows Java whitespace semantics. A small custom validation
  constraint can validate normalized text; do not assume raw annotation limits or SQL
  column lengths replace Domain checks. The new typed DTO contracts belong in this
  plan, not retroactively in business requirements.

Sources: [MVC validation](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-validation.html),
[Java 17 String](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html),
[Jackson Java-time module](https://github.com/FasterXML/jackson-modules-java8).

## R06 - Flyway owns schema creation

- **Decision:** Boot-managed `flyway-core` plus H2; versioned SQL under
  `src/main/resources/db/migration`. Explicit `ddl-auto=validate`,
  `spring.jpa.defer-datasource-initialization=false`, and `spring.sql.init.mode=never`.
  Default Flyway integration initializes the datasource before JPA schema validation.
- **Reason:** One schema-creation path and fail-fast mismatch detection.
- **Alternatives:** Hibernate create/update, schema.sql/data.sql, mixed initialization,
  and manual DDL are excluded. No Liquibase or Flyway build-time plugin is needed.
- **Limitations:** Verify ordering with an empty-file startup and negative schema test;
  documentation is not execution evidence. Do not rewrite applied migrations, seed
  credentials in migrations, or enable automatic destructive repair/clean on startup.

Sources: [Boot database initialization](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html),
[Flyway H2 support](https://documentation.red-gate.com/flyway/reference/database-driver-reference/h2).

## R07 - Opaque credentials and local administration

- **Decision:** Generate each credential with `SecureRandom` using a 256-bit random secret.
  An opaque value contains a random credential identifier and secret; store only a
  SHA-256 digest of the secret, associated user ID, creation time, and revocation time.
  Verify using constant-time digest comparison. Provision through the non-web local
  administrative mode, disclose the raw value once after successful commit, and never
  log/store it. Roles live in UserAccess, never in a caller-supplied role header.
- **Reason:** High-entropy random secrets permit efficient hash verification and immediate
  server-side revocation without token signing or a remote identity dependency.
- **Alternatives:** JWT requires signature/key and freshness policy; external identity is
  a future integration. Public signup, fixed tokens, OAuth issuance, and introspection
  services are out of scope. Password hashing of human-chosen values is a different use
  case; this design accepts only generated high-entropy credentials.
- **Limitations:** The credential is a bearer secret. Loopback HTTP is for a local demo;
  require protected transport before exposing it remotely. Losing the once-displayed
  value requires provisioning another credential and revoking the lost one. Administration
  is privileged local filesystem/process access, not an additional public HTTP role.

Sources: [Java SecureRandom](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/security/SecureRandom.html),
[constant-time digest comparison API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/security/MessageDigest.html#isEqual(byte%5B%5D,byte%5B%5D)),
[bearer credential handling](https://www.rfc-editor.org/rfc/rfc6750.html).

## R08 - Stateless Security and matching errors

- **Decision:** Custom bearer authentication from `Authorization` only; stateless session
  policy, no session request cache, form login, HTTP Basic, cookie authentication,
  query/form credentials, public signup, or public token endpoint. Disable CSRF only
  because authentication requires an explicitly supplied bearer header and no
  automatically attached browser credential is accepted. Disable the H2 web console.
- **Reason:** Matches the local opaque-credential model without importing an OAuth service.
- **Alternatives:** Built-in opaque-token introspection assumes an introspection contract;
  browser sessions require a different CSRF design.
- **Limitations:** Statelessness alone does not remove CSRF risk. Revisit this setting if
  cookie or Basic authentication is added. A custom filter must handle its own failures
  with the shared JSON problem writer; MVC advice does not catch arbitrary filter errors.
  Use matching AuthenticationEntryPoint and AccessDeniedHandler implementations.
  Storage outages/timeouts during filter credential lookup must also use that writer:
  503 STORAGE_UNAVAILABLE/STORAGE_BUSY, never a fictitious invalid-credential 401.

Sources: [session management](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html),
[CSRF explanation](https://docs.spring.io/spring-security/reference/6.5/features/exploits/csrf.html),
[filter architecture](https://docs.spring.io/spring-security/reference/6.5/servlet/architecture.html).

## R09 - Transaction and revocation ordering

- **Decision:** Each public Application write uses a Spring-managed transaction through
  an external proxy call, with rollbackFor=Exception.class so checked application
  failures also roll back. Do not swallow failures and return a normal result inside
  the transaction. Within that transaction, acquire `PESSIMISTIC_WRITE` locks
  in this order: caller Credential, then its UserAccess. Re-read revocation and roles
  under these locks, then load the request freshly, check visibility/ownership, expected
  version, current state, and Domain rules. Keep locks until commit/rollback.
  Credential revocation uses Credential then UserAccess; role removal locks UserAccess
  only and never subsequently locks an existing Credential. Provisioning may lock
  UserAccess then insert a new credential, but must not lock existing credentials later.
- **Reason:** A write winning the locks commits before revocation takes effect. Revocation
  winning and committing first makes the waiting writer observe removed access and reject.
  Revocation takes effect at commit, not command invocation. Different users retain
  independent access locks and can compete optimistically on the same request.
- **Alternatives:** Filter-only checks permit a check/use race. Locking every request
  pessimistically would hide the requested optimistic-update path. Optimistic UserAccess
  checks alone can miss credential races unless every dependent row is guarded.
- **Limitations:** Same-user writes serialize intentionally. Use READ_COMMITTED and fresh
  locked reads; do not carry managed entities or cached permission snapshots from the
  filter. Configure H2 session `LOCK_TIMEOUT=2000` through connection initialization,
  pool acquisition timeout 3 seconds, and transaction timeout 10 seconds. Test actual
  waits; a JPA timeout hint alone is insufficient. No automatic retry of sensitive writes.
  Timeout and unavailable-database failures are 503, not optimistic 412 conflicts.

Select `spring.jpa.open-in-view=false` so persistence contexts do not extend through
HTTP view/serialization processing. Use a transaction-bound context per operation, never share an
EntityManager across threads, manually commit/rollback, or rely on transactional
self-invocation. DTO snapshots cross boundaries; managed entities do not.

Sources: [Spring transaction proxies](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html),
[JpaTransactionManager](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/orm/jpa/JpaTransactionManager.html),
[H2 lock timeout](https://h2database.com/html/commands.html#set_lock_timeout).

## R10 - Request concurrency, ETags, and commit

- **Decision:** Numeric nullable-before-persist `Long @Version` on the Infrastructure
  request entity, never on Domain. Require a single observed strong ETag in If-Match on
  PATCH and every action POST after creation. Compare it after authorization but before
  state rules; Hibernate's version-conditioned UPDATE supplies the final race check.
  Do not assign/increment version manually or bypass it through bulk updates.
- **Reason:** Explicit expected-version checks handle already-stale calls; the guarded
  UPDATE handles two genuinely overlapping transactions that both saw the same version.
- **Alternatives:** Last-write-wins violates FR-013. Pessimistic request locks alone would
  not demonstrate the requested optimistic strategy. Wildcard preconditions would omit
  the observed-version safeguard.
- **Limitations:** Version ETags are strong only for deterministic representations whose
  exposed fields all change under the same parent version. T083 pins successful
  representations to canonical UTF-8 JSON bytes; incompatible Accept charset alternatives
  are excluded before mutation, with the existing 406 when no compatible alternative remains.
  See Phase 8 validation for the byte-level regression evidence. An identical-content PATCH
  returns 200 with unchanged representation/ETag, using an explicit OPTIMISTIC request
  lock to check its version at commit; it does not force-increment the version or update
  timestamps artificially. Changed writes update business fields and `updatedAt`, flush,
  then copy an immutable result with the generated version. Flush is not commit: the
  controller sends that snapshot only after the transactional proxy commits successfully.
  Do not reread after commit and accidentally return a later writer's revision.

The contract maps missing If-Match to 428; malformed/unsupported forms to 400; weak or
nonmatching validators and optimistic failures to 412. A current version with an invalid
lifecycle yields 409. Lock timeout and database unavailable remain distinct 503 codes.

Sources: [Hibernate locking](https://docs.hibernate.org/orm/6.6/userguide/html_single/#locking),
[If-Match strong comparison](https://www.rfc-editor.org/rfc/rfc9110.html#section-13.1.1),
[428 preconditions](https://www.rfc-editor.org/rfc/rfc6585.html#section-3).

## R11 - Decision inserts without retaining full feedback history

- **Decision:** Insert ReviewDecision metadata for each successful approval/request-changes
  in the same transaction as the parent mutation. Metadata contains request, reviewer,
  action, timestamp, and resulting request version, but **no explanation text**. The
  parent alone stores latest explanation, replaced only by successful request-changes.
- **Reason:** Satisfies the explicit atomic decision-insert instruction while honoring
  FR-024's prohibition on full feedback history. The record supports storage invariants
  and rollback verification; no public audit/history feature is introduced.
- **Alternatives:** Storing historical explanation text violates the clarified scope.
  Replacing a single decision row requires delete/insert ordering without meeting a
  stronger current requirement.
- **Limitations:** Decision metadata is retained but previous explanations cannot be
  reconstructed. Do not expose a history route or add an event-sourcing framework.

## R12 - Problem responses and HTTP contract

- **Decision:** JSON DTO responses; Spring ProblemDetail errors with stable `code`.
  Centralize MVC exceptions through RestControllerAdvice/ResponseEntityExceptionHandler;
  security handlers use the same shape. Define routes, precondition grammar, validation,
  and error precedence in [the HTTP contract](contracts/http-api.md).
- **Reason:** Callers can distinguish state, version, authentication, and temporary
  storage failures without parsing free-form text.
- **Alternatives:** Per-controller error bodies drift; default HTML security responses
  do not match the API; exposing SQL/stack traces leaks implementation data.
- **Limitations:** Error details never include credentials, hashes, SQL, or hidden resource
  information. Application exceptions remain HTTP-independent; advice performs mapping.

Sources: [MVC error responses](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-ann-rest-exceptions.html),
[ResponseEntity](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-methods/responseentity.html).

## R13 - Verification and provisional commands

- **Decision:** JUnit 5 unit and integration classes all end in `Test`. Real HTTP tests use
  `@SpringBootTest(webEnvironment=RANDOM_PORT)` with Java 17 HttpClient and isolated
  temporary file-backed H2. The requested eventual `mvn test` lifecycle must discover
  both kinds through Surefire. Restart tests close/reopen the same file database.
- **Reason:** Responses plus independently inspected committed state verify sensitive
  operations, rather than mock or sequential-staleness claims alone.
- **Alternatives:** MockMvc-only tests, memory-only databases, shared EntityManagers,
  `*IT` names without Failsafe configuration, and test-thread rollback do not meet this
  verification design.
- **Verification:** T074 executed all 30 Test classes / 801 cases, with zero failures,
  errors or skips. Two distinct
  Reviewers must overlap separate transactions/contexts after loading the same version;
  same-user access locks otherwise serialize the calls. Inspect parent, version, latest
  feedback, and decision rows through a fresh verification transaction. Admin services
  can race with HTTP writes in the same test JVM, but offline CLI use cannot demonstrate
  live multi-process revocation. T075 packaged before JAR-dependent checks, then T075/T076
  verified separate-JVM migration/startup/graceful restart and actual offline CLI. Exact
  commands and observed outcomes are now in quickstart.md and validation.md. Injected
  unavailable-storage cases are labeled separately from real lock/pool/deadline failures;
  controlled rollback is distinguished from lost acknowledgement after commit.

Sources: [Boot HTTP integration testing](https://docs.spring.io/spring-boot/3.5/reference/testing/spring-boot-applications.html),
[Surefire discovery](https://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html),
[Java 17 HttpClient](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.html).

## Research disposition

All technical choices needed for the selected design were resolved before implementation.
T001 resolved R02's wording synchronization; T073-T081 now record effective dependencies,
all required test discovery/repetitions, executable and documentation verification.
The original decision rationales/alternatives and published version table remain intact.
R03's offline administrative constraint and R01's reviewed support caveat still apply.
No external security audit, PostgreSQL portability, production SLO or Converge execution
is inferred from the local demonstration. See the final matrix for evidence limits.
