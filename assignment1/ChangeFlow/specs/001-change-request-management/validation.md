# Phase 1 Setup Validation Evidence

This is the historical Phase 1 record. The Phase 2 implementation record follows it.

Date: 2026-11-16 (Asia/Tehran). Scope: T001-T006 only. No T007 or later task was executed.
This record reports actual setup checks, not database startup or feature acceptance.

## Prerequisites and current analysis

- Read root AGENTS.md, constitution v1.0.0, spec, plan, research, data model, HTTP contract,
  quickstart, tasks, and requirements checklist before implementation.
- Spec Kit check-prerequisites.ps1 -Json -RequireTasks -IncludeTasks succeeded and selected
  specs/001-change-request-management. No extensions.yml or implementation hooks exist.
- requirements.md: 16 total, 16 checked, 0 unchecked, PASS. T001 refreshed only stale notes;
  all original checklist markers stayed unchanged. This is requirements-quality status.
- No saved speckit-analyze report exists to certify. The current Phase 1 artifact review
  found the known authentication wording discrepancy, resolved by T001, and no additional
  unresolved blocking finding for this batch. This is not a fabricated formal analysis run.
- This directory is not a Git repository: git rev-parse --git-dir failed with the expected
  not-a-repository message. No Git repository/branch/commit was created. File SHA-256
  hashes below identify the delivered source snapshot instead of a commit ID.

## Environment actually observed

| Tool / property | Observed value |
| --- | --- |
| Java runtime | Oracle Java 17.0.13+10-LTS-268, 64-bit |
| javac | 17.0.13 |
| Maven | Apache Maven 3.9.9, revision 8e8579a9e76f7d015ee5ec7bfcdc97d260186937 |
| Maven executable | C:/Program Files/JetBrains/IntelliJ IDEA 2025.2.5/plugins/maven/lib/maven3/bin/mvn.cmd |
| Selected process JAVA_HOME | C:/Program Files/Java/jdk-17 |
| OS / architecture reported by Maven | Windows 10, amd64 |
| Platform encoding / project source encoding | Cp1252 / explicit UTF-8 |
| Working directory for Maven | D:/oroumand-course/assignment1/ChangeFlow |
| Local Maven repository used | target/maven-repository (ignored, within workspace) |

Maven was absent from PATH. An existing IntelliJ-bundled installation was located and
verified; no system installation or global environment change was made. JAVA_HOME was
set only in the command processes to select the installed JDK 17.

## Completed task results

| Task | Result | Verification |
| --- | --- | --- |
| T001 | Complete | Reconciled FR-015, US3-AS11/14, SC-007 and context with explicit later 401 instruction; original answer history retained; other FR/scenario blocks unchanged |
| T002 | Complete | POM parsed; Boot 3.5.16 and both Java 17 properties; effective compiler/Surefire/Boot management; one inherited repackage execution; main class/finalName correct |
| T003 | Complete | Ignore patterns cover generated output, H2 data/traces, private config and credential captures without excluding source/migrations/docs; Git check-ignore not claimed without repository metadata |
| T004 | Complete | Main entry point and five package-boundary documents only; no feature code; compilation and main metadata checked |
| T005 | Complete | UTC Clock bean, reviewed defaults/external credential placeholders; 20 actual SnakeYAML assertions and 5 compiled bootstrap/Clock metadata assertions |
| T006 | Complete | Actual tool versions, Maven resolution/build logs, dependency/plugin versions, source hashes and limitations recorded here |

T001 preserves all 24 FR IDs, 56 acceptance scenario IDs and ten actual user answers.
Authentication failure is uniformly JSON 401 with WWW-Authenticate: Bearer before lookup;
authenticated missing/inaccessible items retain identical 404. No other product choice
was changed. The original clarification answers remain historical text with an explicit
later refinement note rather than being rewritten as if the user had said something else.

## Commands and actual outcomes

Commands below omit only Tee-Object log redirection for readability. Maven commands ran
from the working directory above. Logs/probes/effective models are generated under the
ignored target/phase1 directory; they are evidence artifacts, not application code.

```powershell
java -version
javac -version
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-17'
$phase1MavenExe = 'C:/Program Files/JetBrains/IntelliJ IDEA 2025.2.5/plugins/maven/lib/maven3/bin/mvn.cmd'
& $phase1MavenExe --version

& $phase1MavenExe -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=D:/oroumand-course/assignment1/ChangeFlow/target/maven-repository' test

& $phase1MavenExe -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=D:/oroumand-course/assignment1/ChangeFlow/target/maven-repository' 'org.apache.maven.plugins:maven-help-plugin:3.5.1:effective-pom' '-Doutput=D:/oroumand-course/assignment1/ChangeFlow/target/phase1/effective-pom.xml' 'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree' '-DoutputFile=D:/oroumand-course/assignment1/ChangeFlow/target/phase1/dependency-tree.txt'

& $phase1MavenExe -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=D:/oroumand-course/assignment1/ChangeFlow/target/maven-repository' 'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath' '-Dmdep.outputFile=D:/oroumand-course/assignment1/ChangeFlow/target/phase1/classpath.txt'

& 'C:/Program Files/Java/jdk-17/bin/java.exe' --class-path 'D:/oroumand-course/assignment1/ChangeFlow/target/maven-repository/org/yaml/snakeyaml/2.4/snakeyaml-2.4.jar' 'D:/oroumand-course/assignment1/ChangeFlow/target/phase1/ValidateConfiguration.java' 'D:/oroumand-course/assignment1/ChangeFlow/src/main/resources/application.yml'

$projectClasspath = (Get-Content -LiteralPath 'D:/oroumand-course/assignment1/ChangeFlow/target/phase1/classpath.txt' -Raw).Trim()
& 'C:/Program Files/Java/jdk-17/bin/java.exe' --class-path "D:/oroumand-course/assignment1/ChangeFlow/target/classes;$projectClasspath" 'D:/oroumand-course/assignment1/ChangeFlow/target/phase1/VerifyBootstrap.java'

& 'C:/Program Files/Java/jdk-17/bin/javap.exe' -verbose 'D:/oroumand-course/assignment1/ChangeFlow/target/classes/com/changeflow/ChangeFlowApplication.class'
```

| Check | Actual result |
| --- | --- |
| Initial sandboxed Maven test | Failed before compilation: Maven Central connection denied; no tests executed. Log: target/phase1/maven-test-sandbox.log |
| Maven test retried with network permission | BUILD SUCCESS, 1:51 minutes; compiled 7 source files with release 17. Surefire: No tests to run. Log: target/phase1/maven-test.log |
| Effective POM + dependency tree | BUILD SUCCESS, 47.455 seconds; actual resolved evidence files emitted. Log: target/phase1/maven-model.log |
| Offline dependency classpath | BUILD SUCCESS, 2.506 seconds using the downloaded local repository. Log: target/phase1/maven-classpath.log |
| Optional Python YAML check | Not executed successfully: PyYAML module was absent. Replaced with the actually resolved Java YAML parser; no Python package was installed |
| SnakeYAML 2.4 parser probe | PASS: 20 configuration type/value assertions, including external placeholders, Flyway/JPA settings, timeouts and quoted string OFF log levels. Log: target/phase1/configuration-check.log |
| Compiled bootstrap/Clock probe | PASS: 5 checks for UTC Clock, Configuration/Bean metadata and public static Boot main metadata. Main was not invoked. Log: target/phase1/bootstrap-check.log |
| Class-file target | Both ChangeFlowApplication and ApplicationConfiguration have major version 61 (Java 17) |
| Resource copy | Compiled application.yml SHA-256 equals source application.yml SHA-256 |
| Scope/integrity checks | No controller, entity, service, migration, test-class implementation, app data directory, or packaged executable JAR created |

The Maven test lifecycle executed **0 JUnit tests**: none discovered, zero test failures
reported, and no skipped test suite claimed. This is a successful compilation/lifecycle
check, not proof that any business acceptance test passed. The first planned test task
is T008, outside this batch. The 20 parser and 5 metadata checks are standalone setup
assertions, not JUnit/HTTP integration tests.

## Actually resolved versions

Read from the generated effective POM and dependency tree, rather than assuming the
research table had already been resolved. These values matched the reviewed expectations.

| Component | Resolved version |
| --- | --- |
| Boot parent / plugin | 3.5.16 / 3.5.16 |
| Spring MVC | 6.2.19 |
| Spring Security core | 6.5.11 |
| Spring Data JPA | 3.5.13 |
| Hibernate ORM / Validator | 6.6.53.Final / 8.0.3.Final |
| Jackson databind / Java-time module | 2.21.4 / 2.21.4 |
| Flyway core | 11.7.2 |
| H2 | 2.3.232 |
| JUnit Jupiter / Platform engine | 5.12.2 / 1.12.2 |
| SnakeYAML | 2.4 |
| Maven Compiler / Surefire | 3.14.1 / 3.5.6 |
| Evidence CLI Help / Dependency plugins | 3.5.1 / 3.8.1 (also managed by the reviewed Boot BOM) |

The effective compiler release is 17. The Boot plugin has one inherited repackage goal,
main class com.changeflow.ChangeFlowApplication, and finalName changeflow. Repackage
execution and launching a JAR were not performed in this phase.

## Changed files

Added:

- [pom.xml](../../pom.xml)
- [.gitignore](../../.gitignore)
- [ChangeFlowApplication.java](../../src/main/java/com/changeflow/ChangeFlowApplication.java)
- [ApplicationConfiguration.java](../../src/main/java/com/changeflow/configuration/ApplicationConfiguration.java)
- [application.yml](../../src/main/resources/application.yml)
- [domain/package-info.java](../../src/main/java/com/changeflow/domain/package-info.java)
- [application/package-info.java](../../src/main/java/com/changeflow/application/package-info.java)
- [infrastructure/package-info.java](../../src/main/java/com/changeflow/infrastructure/package-info.java)
- [api/package-info.java](../../src/main/java/com/changeflow/api/package-info.java)
- [configuration/package-info.java](../../src/main/java/com/changeflow/configuration/package-info.java)
- [validation.md](validation.md)

Modified:

- [spec.md](spec.md): only the authorized authentication refinement and affected context.
- [checklists/requirements.md](checklists/requirements.md): stale notes only, no marker changes.
- [tasks.md](tasks.md): only T001-T006 completion markers; later tasks remain unchecked.

AGENTS.md, constitution, plan, research, data model, HTTP contract and quickstart remain
byte-for-byte unchanged. Generated build output, downloaded Maven repository and diagnostic
probes are ignored under target/; no secret values are included in this record.

## Remaining work and limits

- Stop before T007. No db/migration SQL or foundation/story task has been implemented.
- No Spring application context, database connection, Flyway migration, schema validation,
  HTTP server, administrative command, packaging launch or feature acceptance was run.
  Configuration inspection does not prove migration ordering or actual lock-wait behavior.
- Database credentials are unresolved external placeholders by design; no default user or
  password was introduced, and no test was made to appear successful by disabling JPA,
  Flyway, or schema validation.
- The bearer security chain, local administration, entities, Domain rules, transactions,
  endpoints and required JUnit/HTTP/concurrency suites remain later tasks. This bootstrap
  is not a completed or secure feature deployment.
- The exact pinned Boot support limitation and offline H2 administration constraint remain
  documented in research. No PostgreSQL compatibility or production-readiness claim.
- No current blocker remains for these six setup tasks. A saved formal analysis report was
  not produced; the actual scoped checks and prerequisite limitations are stated above.

## Source snapshot (SHA-256)

The evidence file is excluded from its own hash to avoid circular self-reference. Task
markers are hashed after marking only the verified T001-T006 complete.

| File | SHA-256 |
| --- | --- |
| pom.xml | AC66E684CFDAF160D76B329FC5CDECD39AC7FC56E186CB5AB513A4A58D7C25E8 |
| .gitignore | D8ED066E54E620FD20A629BE174FC2CFEBE0D38AB66608D657500A775DAAD835 |
| src/main/java/com/changeflow/ChangeFlowApplication.java | B94FBFF922EFFF4281EDC79E49BC63AB037A18FC2A877E450F4E8E616DB4CA71 |
| src/main/java/com/changeflow/configuration/ApplicationConfiguration.java | 4526616B16C60C5CE50284D417F706BADF8C73DCD9AED54DEE65BD0A4162DD4D |
| src/main/resources/application.yml | C064610E089E9D275E82C3820DD05790C7A77F5F3404A20AA08FB94A06DFAF57 |
| src/main/java/com/changeflow/domain/package-info.java | DF7E8BDF4ADE9129268D427BDD96869AC123D4B3DA974326DA3733B1711973E9 |
| src/main/java/com/changeflow/application/package-info.java | 8AE76C26B3D7188980849B88F0F8D33E0C2915141501156AC86399E3019BC3FB |
| src/main/java/com/changeflow/infrastructure/package-info.java | E835043B25DD17E2989F76A5767938A089EEC9E05F2CA1FB570FAB62B21A9535 |
| src/main/java/com/changeflow/api/package-info.java | FC59B4089A2F78236A200F8C36AA380D7CCED8173ADB201272DA0324D2398637 |
| src/main/java/com/changeflow/configuration/package-info.java | BDAE884F960A4B6E0715A6B4635C7B1292E0A53B79CF5D087D1F78ACA528A5EB |
| specs/001-change-request-management/spec.md | 02DE5F1D91B8727A7145FD53059E89832A66851C9CC207E81E6C47B553B42182 |
| specs/001-change-request-management/checklists/requirements.md | BED4C57BBDB6CD8167810DEC6B79FB3E4B7BD38D1BA3687D40BDF42F53424D13 |
| specs/001-change-request-management/tasks.md | 391B148F712146A869A062B05A8EC4D012C4B9B3F8ED3B556D287AF7AA8A1949 |

# Phase 2 Shared Foundation Validation Evidence

Date: 2026-11-16 (Asia/Tehran). Authorized scope: T007-T030 inclusive, sequentially.
No additional agents were used. T031 and all later tasks remain unstarted.

## Prerequisite verification

- Read AGENTS.md, constitution v1.0.0, spec, plan, research, data model, HTTP contract,
  quickstart, tasks and the implementation skill. The active-feature prerequisite script
  succeeded. No extensions.yml or before/after implementation hooks exist.
- requirements.md remains read-only: 16 checked, 0 unchecked, PASS.
- Before editing, verified all 13 Phase 1 snapshot hashes above: all matched. T001-T006
  were marked complete. Re-executed the offline Maven test baseline: BUILD SUCCESS,
  zero tests before foundation tests existed. The Phase 1 checkpoint was satisfied.
- No saved formal analysis report was present. The actual scoped artifact review found
  no unresolved blocking finding for this batch; this does not claim a formal analysis
  run or human acceptance. All recorded OPEN/FR/scenario IDs and product decisions remain
  unchanged. The existing authentication refinement is preserved.
- POM, application.yml, spec and requirements checklist hashes still match Phase 1 after
  implementation. No new dependency, version override or build exclusion was introduced.
  Root AGENTS.md's historical absent-POM note is reserved for the explicit T079 update;
  verified commands for this batch are recorded here. It did not block the authorized work.

## Task completion and evidence

All rows below are complete. Test-authoring tasks preceded the behavior they test;
initial compilation blockers are distinguished from executed assertion failures below.

| Task | Delivered artifact / verification |
| --- | --- |
| T007 | V1__create_changeflow_schema.sql: four tables, scalar UUID FKs, digest uniqueness, decision-version uniqueness, enum checks, nanosecond timezone columns; no seeds/history text |
| T008 | MigrationStartupTest: fresh file, all four mapped entities, migration history, constraints, repeated startup, broken migration and schema mismatch |
| T009 | ContentRulesTest authored before rules: normalization, blank/null, inclusive limits, supplementary code points |
| T010 | Pure JDK ContentRules and exact RequestState/UserRole/ReviewAction enums; rule tests green |
| T011 | Independent immutable ChangeRequest rehydration, normalized content/feedback invariants, immutable IDs/time fields; no lifecycle methods or revision |
| T012 | UserAccessEntity/repository with both independent flags and current-access lock query |
| T013 | CredentialEntity/repository: immutable ID/user/hash, defensive digest copies, irreversible revocation |
| T014 | ChangeRequestEntity/repository: large Unicode fields, timestamp precision, nullable-before-insert Hibernate Long @Version |
| T015 | ReviewDecisionEntity/repository: metadata only, no explanation column; four mappings validated against Flyway |
| T016 | ChangeRequestMapper: independent values, immutable association checks, no version assignment |
| T017 | Six narrow Application ports, immutable caller/snapshot results and transport-independent failure categories |
| T018 | JpaChangeRequestStore/JpaReviewDecisionLog: fresh loads, predicate before arbitrary-offset pagination, managed update/flush, explicit OPTIMISTIC no-op check, same-transaction metadata append/flush |
| T019 | OpaqueCredentialCodecTest authored before codec: generated format, 256-bit secret, digest-only material, invalid inputs, defensive copies and distinct issuance |
| T020 | SecureRandom opaque codec, SHA-256, canonical parsing and MessageDigest.isEqual; three codec tests green |
| T021 | Credential/access stores and guard: fresh locked reads, Credential then UserAccess; guard refresh tested after previously cached reads and independently committed removal/revocation |
| T022 | LocalAdministrationTest authored before service: migrated file storage, both flags, digest-only persistence, revocation, failure and rollback outcomes |
| T023 | Constructor-injected public transactional LocalAdministrationService; real Spring proxy, READ_COMMITTED, 10 seconds, rollbackFor Exception; existing-access locking before new credential insert |
| T024 | Explicit pre-start admin parsing, forced non-web mode, one operation then context close, safe failure status and post-proxy one-time output; four actions exercised through the actual entry-point method |
| T025 | ProblemResponseTest authored before mapper/wiring, using test-only HTTP probes; no production story routes |
| T026 | Shared ProblemFactory and MVC advice: safe stable JSON, parsing/body/method errors, distinct state/version/precondition/storage cases; unknown commit acknowledgement does not promise rollback |
| T027 | Java-time/deterministic JSON, strict unknown fields/string types, normalization before constraint validation, code-point NormalizedText constraint |
| T028 | Trusted immutable caller IDs, header-only bearer authentication, stateless chain, shared 401/403/503 writer; one chain registration and no servlet filter duplication |
| T029 | HttpTestSupport/TestDatabaseConfiguration: RANDOM_PORT, Java 17 HttpClient, generated users, one file database per context, all four seeded states, fresh snapshots and pool/database cleanup |
| T030 | Full normal Surefire test lifecycle green; actual settings/bootstrap/security checks below; no story implementation |

The current-access protocol is retained: sensitive writes/revocation acquire Credential
then UserAccess; role removal acquires only UserAccess; new credentials may be inserted
after the access lock. Locked rows are refreshed rather than trusting filter snapshots.
Request versions remain entirely Hibernate-managed. No request pessimistic lock, bulk
request update, manual commit/rollback, automatic sensitive-operation retry or transactional
self-invocation was introduced. Store adapters require an existing caller transaction.

## Commands actually executed

Working directory: `D:/oroumand-course/assignment1/ChangeFlow`. The JDK/Maven installation
and resolved dependency/plugin versions remain those recorded in Phase 1; POM unchanged.
Compilation used release 17; javap again reported class-file major version 61.

```powershell
.specify/scripts/powershell/check-prerequisites.ps1 -Json -RequireTasks -IncludeTasks
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-17'
$foundationMavenExe = 'C:/Program Files/JetBrains/IntelliJ IDEA 2025.2.5/plugins/maven/lib/maven3/bin/mvn.cmd'
New-Item -ItemType Directory -Force target/phase2/tmp | Out-Null

# Phase 1 baseline (before new tests existed):
& $foundationMavenExe -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=D:/oroumand-course/assignment1/ChangeFlow/target/maven-repository' test

# Final full foundation run; no test selection, exclusion or skip:
& $foundationMavenExe -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=D:/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=D:/oroumand-course/assignment1/ChangeFlow/target/phase2/tmp' test

& 'C:/Program Files/Java/jdk-17/bin/javap.exe' -verbose target/classes/com/changeflow/ChangeFlowApplication.class
```

Focused runs used the same Maven executable/cache, with `-Dtest=MigrationStartupTest`,
`ContentRulesTest`, `MigrationStartupTest,ContentRulesTest`, `OpaqueCredentialCodecTest`,
`LocalAdministrationTest` or `ProblemResponseTest`. The workspace java.io.tmpdir option
was added after the initial sandbox cleanup failure. One online retry (without `-o`,
with network permission) downloaded the pinned Surefire JUnit provider; no version changed.

## Actual red, blocked and corrective runs

Logs are ignored generated evidence under target/phase2; no credential captures are saved.
Two early migration rechecks inadvertently overlapped while correcting the fixture. This
deviated from the requested sequential-check execution; subsequent checks, including the
final full checkpoint, ran one at a time. No task implementation was delegated or run
by another agent. The final result does not rely on those overlapping rechecks.

| Log / check | Actual outcome |
| --- | --- |
| t008-red.log | Blocked: offline cache lacked surefire-junit-platform 3.5.6; no JUnit execution |
| t008-red-resolved.log | Provider resolved with network permission; 4 tests, 2 assertion failures because no entities existed and schema validation could not detect a missing unmapped column |
| t009-red.log | Compilation blocked by not-yet-created rule/enumeration types; not a failed business assertion |
| t010-green.log | 9 content-rule tests passed |
| t015-green.log (historical filename) | Failed: history assertion counted Flyway's table-creation marker, plus sandbox access errors cleaning default Windows temp directories; the filename does not indicate success |
| t015-recheck.log | Workspace temp location fixed cleanup; 4 tests, 1 assertion failure from the overly broad history count |
| t017-foundation.log | Corrected history assertion to successful version 1; 13 migration/content tests passed |
| t019-red.log | Compilation blocked by absent codec, not a failed credential assertion |
| t020-green.log | 3 codec tests passed |
| t022-red.log | Compilation blocked by absent administration service and an AssertJ generic overload ambiguity; neither is a business-rule test result |
| t023-green.log | 4 administration tests passed after service implementation and explicit Boolean assertion |
| t025-red.log | 4 HTTP test failures: unauthenticated probes received 401 before the planned error mapper/security wiring existed |
| t030-first.log | Compilation failed on ProblemDetail's HttpStatusCode signature; corrected to HttpStatusCode.valueOf |
| t030-second.log | 30 tests, 14 context errors: Windows backslashes were interpreted by the test property parser; fixed helper URL to forward slashes |
| t030-third.log | 30 tests passed with completed security/problem wiring |
| t030-fourth.log | Compilation failed on three new generic TransactionTemplate/AssertJ assertions; replaced with explicitly typed local snapshots |
| t030-fifth.log | 35 tests passed, including added storage-port and filter-failure coverage |
| foundation-final.log | Final complete run: BUILD SUCCESS; 36 tests, 0 failures, 0 errors, 0 skips; 34.082 seconds |

The final log contains expected error diagnostics from the deliberately broken migration
and missing-column startup tests. Those negative checks pass by asserting startup fails;
they are not unresolved startup failures. No failing check was disabled or excluded.

## Final discovered suites and observations

Counts were independently read from target/surefire-reports/TEST-*.xml.

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| ContentRulesTest | 10 | 0 | 0 | 0 |
| OpaqueCredentialCodecTest | 3 | 0 | 0 | 0 |
| LocalAdministrationTest | 10 | 0 | 0 | 0 |
| MigrationStartupTest | 4 | 0 | 0 | 0 |
| ProblemResponseTest | 9 | 0 | 0 | 0 |
| **Total** | **36** | **0** | **0** | **0** |

- Fresh file H2 migration succeeds before validation of four mappings. Broken migration
  and a removed mapped column fail startup. Restart preserves rows. FKs, invalid enums,
  unique secret digest and unique decision/request-version constraints are exercised.
- Real persistence round-trips 200/5,000/2,000 supplementary code points and nanosecond
  instants. Managed changed writes increment the version; an unchanged optimistic check
  preserves it. A stale expected revision rejects. A controlled failure after parent
  and metadata flush rolls back both, including feedback and version, inspected freshly.
- Fresh access checks observe independently committed changes despite prior cached reads.
  Repeated revocation keeps the original timestamp. Removing one role preserves the
  other; removing both leaves neither flag. Provisioning rollback leaves both tables
  unchanged. No public credential issuance/provisioning route exists.
- Actual connection CALL LOCK_TIMEOUT() returns 2000 ms; actual Hikari acquisition setting
  is 3000 ms. The proxied administrative transaction attribute is READ_COMMITTED/10 s,
  with checked-exception rollback enabled. JPA validate, open-in-view false, deferred
  initialization false and SQL init never are checked in the running context. These are
  settings checks; measured contention/pool/transaction wait tests remain T064.
- Real HTTP on RANDOM_PORT verifies problem JSON even for text/html Accept; safe body and
  method validation, strict controlled/unknown fields, 405 Allow, 406/415, and distinct
  409/412/428. Generic constraints remain 500 rather than fictitious version conflicts.
  Storage timeout/unavailability are distinct 503 responses in MVC and the bearer filter.
  The filter tests inject storage failures around the real adapter; they do not claim an
  actual database outage or measured lock timeout. Persisted data is inspected afterward.
- Missing/malformed/invalid/revoked credentials produce identical JSON 401 for the same
  path plus Bearer challenge. Cookie/query/role-header input grants no authentication.
  Authenticated probe success creates no session cookie; denial is safe 403. Exactly one
  bearer filter exists in the security chain, with no duplicate servlet registration.
- The actual administrative entry-point method executes with the configured HTTP port
  already occupied and even an attempted servlet-mode override. It performs provision,
  issue-credential, revoke-credential and remove-role; generated credential output is
  captured privately in memory and emitted once after successful proxy return. Invalid
  arguments and failed issue operations return status 1 without credential output. The
  context is closed by try-with-resources. Packaged JAR/process-exit verification remains
  T075/T076; same-JVM storage inspection alone is not proof of separate-process file locking.
- Pure Domain imports only Java. A scoped import scan found zero forbidden Domain or
  Application imports. Source inspection found no story controller, manually assigned
  version, production manual commit/rollback or AUTO_SERVER. No packaged JAR or default
  application data/ directory was created. Ignore patterns cover generated/cache/DB data.
- A scoped credential-pattern scan and source review found no live credential, fixed
  password, private key or committed token capture. Generated credential assertions avoid
  printing secret material. This is a scoped check, not a third-party secret audit.

## Verified administrative argument grammar

These argument forms were exercised through ChangeFlowApplication.execute by the
foundation tests, with generated credentials and temporary file databases. They are not
claims that a packaged JAR command has been executed. Normal/admin modes use the same
datasource configuration and working-directory path resolution. Separate-JVM administration
of the default embedded H2 file remains offline-only: stop HTTP and close its pool first.

```text
--changeflow.mode=admin --changeflow.action=provision --changeflow.roles=Requester,Reviewer
--changeflow.mode=admin --changeflow.action=issue-credential --changeflow.user-id=<UUID>
--changeflow.mode=admin --changeflow.action=revoke-credential --changeflow.credential-id=<UUID>
--changeflow.mode=admin --changeflow.action=remove-role --changeflow.user-id=<UUID> --changeflow.role=Requester
```

Provision accepts a comma-separated nonduplicated subset of Requester/Reviewer or explicit
None; remove-role accepts one of the two roles. Unknown/duplicate administrative options,
invalid IDs/roles, missing action and invalid mode fail before application creation.
No public administrator role or HTTP endpoint was added. No raw output is recorded here.

## Remaining work and checkpoint disposition

- T007-T030 are complete and the shared foundation is green. T001-T030 are now marked [X];
  all 51 tasks from T031-T081 remain unchecked. Stop before T031.
- No owner lifecycle, independent-review or revision Application service/HTTP endpoints,
  story DTOs, ETag parser or Domain lifecycle methods exist. Store fixtures construct
  states for foundation verification; they do not implement or accept those user stories.
- No full assignment/feature acceptance, self-review/Approved immutability acceptance,
  genuine competing HTTP review race, no-op commit race, revocation/write race, required
  100 repetitions, actual outage/wait measurement, packaging or production readiness is
  claimed. Their explicit later tasks remain required.
- No unresolved failure or blocker remains for Phase 2. The pinned Boot support limitation
  and offline embedded-H2 operating restriction remain as reviewed. No PostgreSQL claim.
- Constitution, reviewed behavior/design, quickstart and root guidance remain unchanged.
  T079/T080 own their later current-state documentation updates; no new product decision
  or constitutional amendment was made here.

## Phase 2 changed files and source snapshot

The following manifest lists every Phase 2 source/document addition or modification.
SHA-256 identifies the source snapshot because this workspace has no Git repository.
validation.md is excluded from its own hash. Generated target output is ignored.

| Status | File | SHA-256 |
| --- | --- | --- |
| Added | src/main/java/com/changeflow/api/ApiExceptionHandler.java | 6E002CC0F66A67955D9F624E3F6A98C465812ECC545DE845D409ED62D97757B6 |
| Added | src/main/java/com/changeflow/api/ProblemFactory.java | 7C8F940D71AF13559D8F62FF04E3C6C93F9F7590DE57A222018995EA83703BD2 |
| Added | src/main/java/com/changeflow/api/SecurityProblemHandlers.java | 4BD42315381E4F761B544DC31CFCF3B78857543973DA2394B49C1045A754B8E6 |
| Added | src/main/java/com/changeflow/api/validation/NormalizedText.java | 429B3D20EC4813752FBC49FCE3DF7BD688A4107166AB6FE9F36052DCFABD6082 |
| Added | src/main/java/com/changeflow/api/validation/NormalizedTextValidator.java | 6CC4D70F5C974209D468AC2407AD6C3ADE13B643444C16A77EFD6B1435FF3C4A |
| Added | src/main/java/com/changeflow/application/ApplicationFailure.java | AF0208EA7E81E4310E09FE1C73C987E07E79473A1151FFACB11FC10A9EF21B52 |
| Added | src/main/java/com/changeflow/application/AuthenticatedCaller.java | 6684838C673D3DB3A1AC4FEB9D2D5FCAEE9E1A9E3CB24853289F27109AAB2B19 |
| Added | src/main/java/com/changeflow/application/CredentialAuthenticationService.java | FED951D62C3625C50B22A0BA06800F35D830A62F01B739B8A2A36FF65240C129 |
| Added | src/main/java/com/changeflow/application/LocalAdministrationService.java | B65C68AA76E7ADA806593897AADCD0D0BD1C23C61B952B21723967E5CB373D8F |
| Added | src/main/java/com/changeflow/application/port/ChangeRequestStore.java | C9960F383DAF2A701FB3107A7F21B2DA2F7F232AE4133DC83C1F57FC86A93696 |
| Added | src/main/java/com/changeflow/application/port/CredentialCodec.java | F5345859D30AC9ECB1D4E933450C0DA946AA8D0F20F861C2480D4AD380EF1922 |
| Added | src/main/java/com/changeflow/application/port/CredentialStore.java | ADC9353E5BDB895D3800717500AA64989387E7536C9C950914A53552B2F0A122 |
| Added | src/main/java/com/changeflow/application/port/CurrentAccessGuard.java | 5EE5F4F8001A46E1E462756F9EFE198763AA81A4B60D45C41C6F50C8313C8215 |
| Added | src/main/java/com/changeflow/application/port/ReviewDecisionLog.java | 35EDD16BFFCD61BB38685E3A1F8E2D91EB142270C9C8D8283AF3A10E61B61A67 |
| Added | src/main/java/com/changeflow/application/port/UserAccessStore.java | 244E240C57D55E91A14A24EAB2C10AEE74827A51841F4AC53A3D23025BF8EB5F |
| Added | src/main/java/com/changeflow/application/RequestSnapshot.java | 456572986ED289F637ECE9A1E493763FBCA197E0123E039AD7947692FE28F8AA |
| Modified | src/main/java/com/changeflow/ChangeFlowApplication.java | C95E3ABE9C64A64E6BF572E1B6F6E961A673E53C70373A28F463B2F98C8C420D |
| Added | src/main/java/com/changeflow/configuration/AdministrativeModeConfiguration.java | D4399E8BFF2A4D14E5530248CF34EC16F99A09027F4A0E6CA2DCED5D679AFF75 |
| Added | src/main/java/com/changeflow/configuration/JsonConfiguration.java | 3AD5E937580895DD300DCB5434F83794760AA25B5DE8420042301C567A35CA8F |
| Added | src/main/java/com/changeflow/configuration/LocalAdministrationRunner.java | B007B6C91650DB84ED2CC1266CBFD3ABCB31B2CD0453EF8A3BDB5FA629CA7319 |
| Added | src/main/java/com/changeflow/configuration/SecurityConfiguration.java | CB3CBF7246712845822ADCB8EA78CF10E625C89F37E48775DE4BDB7B9876C501 |
| Added | src/main/java/com/changeflow/domain/ChangeRequest.java | 8668778ED42BD4441569FF27D0E67E35FF6F840EC9302C878B9CE2617DA7D5DD |
| Added | src/main/java/com/changeflow/domain/ContentRules.java | 5B4873B0F88D9B0A6767A64D037ABBC9EDC5164B30F4891BE07BFDE96EE33835 |
| Added | src/main/java/com/changeflow/domain/RequestState.java | 386D2D71D9FC9B32A6894993BDEC1DCE4AB73697D59B21EA65898363A3982E1D |
| Added | src/main/java/com/changeflow/domain/ReviewAction.java | CC2EC628030A166544583ECCA79CBA7D09CCC5D7E8BD19B5E366F88E3CD7F412 |
| Added | src/main/java/com/changeflow/domain/UserRole.java | F7D3858C14F431F1F1EBB33715160C9F9D3D45B0CA6A3B9FC0F1AAE79F0F3FE1 |
| Added | src/main/java/com/changeflow/infrastructure/persistence/ChangeRequestEntity.java | 16F25D4EAC9F87DE420939DCE1D579EC9F502C9E8F2DED921682055AB06F8349 |
| Added | src/main/java/com/changeflow/infrastructure/persistence/ChangeRequestJpaRepository.java | 09ACA7A948790214323E9F0C3466BDDA593A581801DA570E87049C5B87DB10C6 |
| Added | src/main/java/com/changeflow/infrastructure/persistence/ChangeRequestMapper.java | 22F2D643206E29DB5B9CD94B9499E7B6CBDE18365DAC1EC6D8A4178362C35626 |
| Added | src/main/java/com/changeflow/infrastructure/persistence/CredentialEntity.java | 3A559387D72E353BE04648942003DD177F05A2018B66190CD91E6EB8D671926C |
| Added | src/main/java/com/changeflow/infrastructure/persistence/CredentialJpaRepository.java | B9EC494CED22D7A1F1F702FE515D35AF2076B82690C048F9FE8508C8BE3C2212 |
| Added | src/main/java/com/changeflow/infrastructure/persistence/JpaChangeRequestStore.java | 6D19A991CB581BB2987DB5929B8976BB08DE62C8CDC221004791DFEF71DE2447 |
| Added | src/main/java/com/changeflow/infrastructure/persistence/JpaCredentialStore.java | 46FF7CFB49CF9144D7A6FF24D461D0095161DFC34FDFF8356870A1E8CC47C5F0 |
| Added | src/main/java/com/changeflow/infrastructure/persistence/JpaCurrentAccessGuard.java | E1E62A7AF55133D2034BCDDFD16C6308801B82C0C1D6F5857445895E41C865DA |
| Added | src/main/java/com/changeflow/infrastructure/persistence/JpaReviewDecisionLog.java | 8234F5611FF463F646CBBA7908898A8A344F85F87AAD8AA59B92A4928FD3F194 |
| Added | src/main/java/com/changeflow/infrastructure/persistence/JpaUserAccessStore.java | 0C81AAFA6F496DC98B2FEE222E2B9D3E94397726BF23F3D8C61B2C09A7FD14D9 |
| Added | src/main/java/com/changeflow/infrastructure/persistence/ReviewDecisionEntity.java | 9BECB61CE05876A9D7D8B980A904E5DD10B58260E5B29C0724D39B505C92E1C3 |
| Added | src/main/java/com/changeflow/infrastructure/persistence/ReviewDecisionJpaRepository.java | E50CCB6B2942951B19BF6C562AF629B8EF6A449CCA48D20A28C6DEECD0782272 |
| Added | src/main/java/com/changeflow/infrastructure/persistence/UserAccessEntity.java | 1B56BC9DE17294518B2FD115E5BADC764047F085733296C03B1FEA56838C977A |
| Added | src/main/java/com/changeflow/infrastructure/persistence/UserAccessJpaRepository.java | 232EE23974EB8D480258D1F5C0FB12176C802D09BFA43E49FDF2B606A4657DBB |
| Added | src/main/java/com/changeflow/infrastructure/security/BearerAuthenticationFilter.java | 12B4075A301E19647B7C6EEF7A1A2231E7FA03444C8689408369FA6CA24442F8 |
| Added | src/main/java/com/changeflow/infrastructure/security/OpaqueCredentialCodec.java | A1C629D8E856A8628B86F7DA1FE56A55422C10F08932593FBB81D8C8143C0E2C |
| Added | src/main/resources/db/migration/V1__create_changeflow_schema.sql | 78C21A322FB090C528EB09A4422D24D86DA543820E04299DEE40C550D25C8019 |
| Added | src/test/java/com/changeflow/domain/ContentRulesTest.java | 7F4D5CC9708C41A13ADD9F3B6316D3AFCE36E2C80BC91B7FBBAB6457E0AD3870 |
| Added | src/test/java/com/changeflow/infrastructure/security/OpaqueCredentialCodecTest.java | 7E0BE932DDCE5B122D59733660B012AFFA2426487EB8FB561DF54AC98A46EC9B |
| Added | src/test/java/com/changeflow/integration/LocalAdministrationTest.java | 8760345C9BCB07BCFF76CB4AA2888F7E60A7A01A433D774405A438D9E08361D3 |
| Added | src/test/java/com/changeflow/integration/MigrationStartupTest.java | 28EBA29CE75418DBDE83E58CE23B41A0F4580C3FAD52ACEC5D4408775467D262 |
| Added | src/test/java/com/changeflow/integration/ProblemResponseTest.java | A65531ADE13FA641ECD3ABF1D99D8B578C80EE597E5863C1A960622A83B32D84 |
| Added | src/test/java/com/changeflow/integration/support/HttpTestSupport.java | A055A8F18BA427DCDC323B5AD530CDED06789287946FC766AF8246F575DF554F |
| Added | src/test/java/com/changeflow/integration/support/TestDatabaseConfiguration.java | 67711FE533C8F3D331298D09C019B875B7856020DB562C8449288FBD80FA0C85 |
| Modified | specs/001-change-request-management/tasks.md | E97CBB368875B4F1EA4D90EB7D3072E98BDDB059CF663F18003A8E1865E35205 |
| Modified | specs/001-change-request-management/validation.md | Excluded (self-reference) |

# Phase 3 Owner Lifecycle Validation Evidence

Date: 2026-11-16 (Asia/Tehran). Scope: T031-T042 only, sequential execution, no
additional agents. This section supersedes the historical Phase 2 stop-before-T031
disposition above. T001-T030 were preserved and reverified before owner work began.

## Governance and scope gate

- Read root AGENTS.md, constitution v1.0.0, the implementation skill and active spec,
  plan, tasks, research, data model, HTTP contract and quickstart. The current user
  request identifies the artifacts as reviewed and authorizes only this Phase 3 batch;
  reviewer/authorizing party: project owner (user in this chat). Disposition: proceed
  after verifying T030 green, stop before T043. No separate formal analysis or full
  feature acceptance is claimed.
- Prerequisite script selected the absolute feature directory
  `F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/specs/001-change-request-management`.
  Available documents: research.md, data-model.md, contracts/, quickstart.md, tasks.md.
- requirements.md is a read-only gate: 16 total, 16 checked, 0 unchecked, PASS. No
  checklist marker was changed. No nested AGENTS.md was found under the source tree.
- `.specify/extensions.yml` did not exist before implementation and was checked again
  after execution: no pre/post hooks to dispatch. Existing .gitignore covers generated
  Maven/cache/test/database output and secrets. No Docker or other ignore file was needed.
- `git status --short` failed because this workspace has no Git repository. No commit,
  branch, or Git diff is claimed. The source manifest below identifies this batch.

## Actual environment and commands

The current machine differs from the historical Phase 1/2 environment. Working directory
for every command below:
`F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow`.

| Property | Observed in this batch |
| --- | --- |
| Maven | Apache Maven 3.8.5, revision 3599d3414f046de2324203b78ddcf9b5e4388aa0 |
| Maven executable | C:/Program Files/apache-maven-3.8.5/bin/mvn.cmd (on PATH) |
| Java used by Maven | Oracle GraalVM Java 17.0.11, C:/graalvm-ee-java17-windows-amd64-21.3.10/graalvm-ee-java17-21.3.10 |
| Compilation | POM release 17; javac log confirms release 17; javap reports major version 61 |
| POM | Unchanged, Boot parent 3.5.16; SHA-256 AC66E684CFDAF160D76B329FC5CDECD39AC7FC56E186CB5AB513A4A58D7C25E8 |
| Repository/cache | target/maven-repository, existing dependency cache inside this workspace |
| Test temporary files | target/phase3/tmp and isolated file H2 databases under target/test-databases |

The first offline attempt did not execute JUnit: Maven 3.8.5 needed cached-plugin
dependency plexus-utils:1.1 that was not present. The permitted online retry against
the machine-configured mirror timed out. A temporary settings file under ignored
target/phase3 pointed at Maven Central. An initial attempt with a different repository
ID was interrupted while resolving cached artifacts; it ran no tests. Reusing the cache's
`central` ID resolved the missing dependency and the foundation checkpoint passed.
No global settings, POM, dependency version or plugin version was changed. Subsequent
commands ran offline with the completed cache. These initial dependency failures are
environment blocks, not business-test red evidence.

Commands actually executed (log redirection omitted):

```powershell
.specify/scripts/powershell/check-prerequisites.ps1 -Json -RequireTasks -IncludeTasks
git status --short
mvn -version
New-Item -ItemType Directory -Force target/phase3/tmp | Out-Null

# Initial offline attempt and an online retry used the same properties; the final
# successful foundation resolution additionally used the temporary Central settings:
mvn -B -ntp -s target/phase3/maven-settings.xml '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase3/tmp' test

# Each initial HTTP red run used the following command, substituting the single
# listed class: OwnerHttpContractTest, OwnerLifecycleHttpTest,
# ContentValidationHttpTest, NoOpPatchHttpTest, VisibilityHttpTest.
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase3/tmp' '-Dtest=OwnerHttpContractTest' test

# Domain authoring/implementation checks:
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-Dtest=OwnerLifecycleTest' test
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-Dtest=OwnerLifecycleTest,ContentRulesTest' test

# Run after each of T038, T039 and T040:
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' compile

# Two combined owner runs, before and after correcting observed failures:
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase3/tmp' '-Dtest=OwnerLifecycleTest,OwnerHttpContractTest,OwnerLifecycleHttpTest,ContentValidationHttpTest,NoOpPatchHttpTest,VisibilityHttpTest' test

# Additional T042 boundary check, before correcting structured-media acceptance:
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase3/tmp' '-Dtest=OwnerHttpContractTest#submitRejectsNonJsonStructuredMedia,OwnerLifecycleHttpTest#applicationRechecksPermissionsAndCredentialsAfterAuthentication+publicApplicationBoundariesUseTheReviewedTransactionPolicy' test

# Complete regression, run before and after the final two corrections. No test
# selection, exclusions, skips, parallel workers or agents:
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase3/tmp' test

javap -verbose target/classes/com/changeflow/ChangeFlowApplication.class
```

JUnit tests use generated credentials in memory and random private database configuration.
Normal application startup still requires external database username/password. No actual
credential, password, hash, or token capture is recorded here. Maven logs and temporary
settings are ignored generated artifacts. Existing root guidance/quickstart build-document
consolidation remains assigned to T079/T077; this batch's verified commands are above.

## Red, green and corrective results

All following log names are under target/phase3. The test-authoring sequence was
T031, T032, T033, T034, T035, T036, before T037-T041 behavior. There was no overlapping
task execution. T031-T035 each wrote only its own test file and used unchanged helpers.

| Log | Actual result |
| --- | --- |
| foundation.log | Dependency resolution blocked offline; no JUnit |
| foundation-resolved.log | Configured mirror connection timed out; no JUnit |
| foundation-central.log | Interrupted dependency resolution with alternate repository ID; no JUnit |
| foundation-recheck.log | Foundation green before T031: 36 tests, 0 failures/errors/skips; BUILD SUCCESS, 24.660 s |
| t031-red.log | 5 tests, 5 assertion failures; absent owner routes return 404 instead of required responses such as 201/428/415 |
| t032-red.log | 4 tests, 4 assertion failures against absent lifecycle/creation/rollback behavior |
| t033-red.log | 20 test cases, 20 assertion failures: absent routes return 404 instead of required content validation/success responses |
| t034-red.log | 3 tests, 3 assertion failures against absent no-op PATCH behavior |
| t035-red.log | 3 tests, 3 assertion failures against absent reads/listing |
| t036-blocked.log | Test compilation blocked by missing Domain lifecycle methods/Edit type; not a failed business assertion or executed JUnit run |
| t037-green.log | 6 owner Domain + 10 content tests passed; 16 total, no failures/errors/skips |
| t038-build.log / t039-build.log / t040-build.log | Each compilation succeeded with release 17 |
| owner-first.log | 41 cases, 2 failures: invalid stored proposal returned 500, and a visibility assertion mistook the safe word credential in a 401 detail for a sensitive field |
| owner-corrected.log | 41 cases, 0 failures/errors/skips; BUILD SUCCESS, 52.639 s |
| t042-boundary-red.log | 3 tests, 1 assertion failure: submit accepted application/problem+json (200 rather than 415); direct Application access-recheck and transaction-policy tests passed |
| final-tests.log | Full discovery: 81 cases, 2 failures: empty text/plain submit got 415, and the old foundation probe still expected the now-implemented collection route to be absent |
| final-corrected.log | Final full run: 81 tests, 0 failures, 0 errors, 0 skips; BUILD SUCCESS, 43.128 s; finished 2026-11-16 19:37:13 +03:30 |

Corrected the failures in their owning files. ChangeRequestMapper preserves Domain
invalid-content failures as ApplicationFailure before repository exception translation,
so submission rejects an invalid stored proposal with safe 400 and no repair/write.
Visibility checks now inspect sensitive property names rather than banning a safe word.
Submit handles optional raw bytes at the transport boundary, requiring application/json
only when bytes exist, strictly parsing the empty object and rejecting structured-media
alternatives with 415. Empty bytes need no media type, even with an unrelated type header.
The foundation test retains all administrative-route/filter-registration assertions;
only the obsolete pre-story absence assertion/name was removed. No failing test was
disabled or excluded.

## Final discovered suites

Read all target/surefire-reports/TEST-*.xml after the final run, independently of the
console summary. All 11 source classes ending in Test are discovered. Counts include
15 parameterized invalid-field invocations in ContentValidationHttpTest; loops within
one test are not counted as additional JUnit tests.

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| ContentRulesTest | 10 | 0 | 0 | 0 |
| OwnerLifecycleTest | 6 | 0 | 0 | 0 |
| OpaqueCredentialCodecTest | 3 | 0 | 0 | 0 |
| ContentValidationHttpTest | 20 | 0 | 0 | 0 |
| LocalAdministrationTest | 10 | 0 | 0 | 0 |
| MigrationStartupTest | 4 | 0 | 0 | 0 |
| NoOpPatchHttpTest | 3 | 0 | 0 | 0 |
| OwnerHttpContractTest | 7 | 0 | 0 | 0 |
| OwnerLifecycleHttpTest | 6 | 0 | 0 | 0 |
| ProblemResponseTest | 9 | 0 | 0 | 0 |
| VisibilityHttpTest | 3 | 0 | 0 | 0 |
| **Total** | **81** | **0** | **0** | **0** |

The six new owner suites account for 45 tests; the foundation still accounts for 36.
The passing negative migration/schema tests deliberately log startup failures; those
diagnostics are not unresolved application startup errors.

## Stored data and behavior actually verified

- Real HTTP POST/GET collection, GET item, PATCH item and POST submit operate against
  isolated file H2 with Flyway plus JPA validation. Creation returns 201/Location/ETag;
  read/edit/submit return 200, deterministic DTO order, stored UTC timestamps, no-store,
  parent-specific strong ETags and no roles/credential/entity internals.
- `stored(id)` uses a REQUIRES_NEW read-only verification transaction after the HTTP
  operation. Equality compares every Domain record field, revision, and every decision
  row (ID, reviewer, action, time, resulting revision), not just state or response status.
  Create-count checks use separately committed/autocommit database reads.
- Actual creation stores normalized title/description/reason, authenticated owner,
  Draft, null feedback, matching creation/update instants and a generated revision.
  Repeated valid creation produces distinct IDs. Changed edits increment revision by
  one and preserve identity/owner/state/omitted fields. Submit increments by one,
  preserves proposal/owner/creation time/feedback and stores PendingReview. These owner
  operations create no decision rows.
- Denied creation, invisible/non-owner writes, removed Requester permission, revoked
  credentials, stale preconditions and current-version invalid states leave business
  data unchanged. Both owner mutations protect visibility/permission before weak or
  otherwise nonmatching tag disclosure. Old versions yield 412 before invalid state;
  current versions with invalid state yield 409. No-op in PendingReview/Approved rejects.
- Empty/identical normalized Draft PATCH returns the identical JSON, ETag, version and
  timestamp, including seeded `2026-11-16T10:00:00.123456789Z`. The real store's
  verifyUnchanged path is observed exactly three times, with no update call in that
  test. Stale no-op after an intervening changed PATCH preserves the newer stored row.
  This is not a claim that the later overlapping no-op commit race was executed.
- Each proposal field is tested for missing/null/empty/Java-whitespace/excessive input,
  normalized exact maxima and max+1, including supplementary Unicode. Valid mixed
  fields do not partially persist alongside invalid input. Controlled fields, including
  matching values and response-only fields, reject with FORBIDDEN_FIELD; unknown fields
  reject. Corrupted legacy text is seeded separately for each stored proposal column;
  submit returns INVALID_CONTENT, and fresh SELECT * maps plus decision counts remain
  identical. The invalid fixture is not repaired by submission.
- Checked exceptions injected after actual parent flush roll back creation, changed
  edit and submission. Fresh snapshots preserve proposal, owner, state, feedback,
  timestamps, version and empty decision rows; failed creation leaves row count unchanged.
  This proves these controlled rollbacks, not an unknown/lost commit acknowledgement.
- An already-authenticated caller is passed directly to the proxied service after role
  removal/revocation. Writes reject current removed permissions; all five operations
  reject revoked credentials without relying on another filter pass. Actual service
  transaction attributes are READ_COMMITTED, timeout 10 s, rollback on checked Exception;
  read/list are read-only and the service is a Spring proxy.
- All four roles/role combinations, ownership and all four states are seeded for
  visibility. Lists and item reads match the union policy; authenticated hidden and
  missing problems differ only by instance path. Feedback follows parent visibility.
  Pagination is checked with 106 visible and 106 invisible Drafts, sorted by creation
  time then ID: default 50, arbitrary offset 3 with limit 100, tail and empty pages,
  correct hasMore. Every seeded row/decision snapshot remains unchanged after reads.
- Media, body shape, unsupported methods/actions, malformed parameters and If-Match
  grammar are exercised. Missing tags give 428; malformed/overflow tags give 400;
  wildcard/list/multiple headers give 400; weak/cross-item/stale/noncanonical strong
  comparisons give 412 after access checks. Submit rejects null/scalar/array/content
  fields/malformed or trailing JSON. It accepts absent bytes or an empty JSON object.
  Review actions remain unmapped; their 404 is absence, not review acceptance evidence.

## Scenario traceability and deferred work

| US1 scenario | Executed evidence (Draft slice where shared) |
| --- | --- |
| AS01 | OwnerLifecycleHttpTest.createEditSubmitAndRepeatedCreatePreserveIdentityAndProposal; OwnerLifecycleTest creation |
| AS02 | Same HTTP journey plus ContentValidationHttpTest.partialEditPreservesOmissionAndInternalWhitespace |
| AS03 | Same journey and OwnerLifecycleTest.submissionPreservesAllProposalAndIdentityFields |
| AS04 | OwnerLifecycleHttpTest.unauthorizedCreationAndForgedHeadersCannotGrantAuthority |
| AS05 | OwnerLifecycleHttpTest.nonOwnersWrongStatesAndCurrentRemovedPermissionsCannotMutate |
| AS06 | ContentValidationHttpTest.matchingControlledFieldsAndUnknownFieldsRejectWholeCreateAndEdit |
| AS07 | OwnerLifecycleHttpTest repeated creation with distinct stored IDs |
| AS08 | Same journey: old-tag repeat 412, current-tag PendingReview repeat 409; returned origin deferred |
| AS09 | HTTP journey normalization, partial-edit whitespace test and normalized boundary loops |
| AS10 | Partial-edit omission test; ChangesRequested variant deferred |
| AS11 | 15 invalidFields parameterized cases and separate nontext/nonobject checks |
| AS12 | Mixed partial-edit invalid-field cases, fresh whole-record equality; ChangesRequested variant deferred |
| AS13 | Exact/max+1 ASCII and supplementary Unicode for each column, create and Draft edit; returned edit variant deferred |
| AS14 | submitRejectsCorruptedStoredContentWithoutRepairOrAnyWrite for each column; ChangesRequested variant deferred |
| AS15 | Matching controlled-field creation loop, unchanged row counts |
| AS16 | Mixed valid/controlled-field edit loop, complete stored snapshot equality |

T031-T042 are complete and marked [X]. T043-T081 remain unchecked and unstarted.
ChangesRequested mutation variants (including returned-origin AS08, AS10/12/13/14,
returned no-ops, revision/resubmission, explanation retention/replacement) remain
T053-T059. Read visibility across seeded ChangesRequested is tested here, but no
returned-state edit/submit behavior was implemented. Domain owner mutations currently
accept Draft only. No review operation or decision-writing service/route was added.

No unresolved failure or blocker remains for this batch. Full assignment acceptance,
independent review, feedback lifecycle, overlapping optimistic/owner/revocation races,
required 100 repetitions, measured contention/outages, packaged startup/admin/restart
and final documentation/traceability remain future tasks. These were not executed or
marked accepted. No packaged JAR or default data/ directory was created.

Scoped source review found the five requested mappings only, no forbidden Domain or
Application imports, no manual version assignment, no production manual commit/rollback,
no transactional self-invocation, no request bulk updates or AUTO_SERVER. A scoped
credential-pattern scan found only generated/external test password references, no fixed
credential or private key. This is a source check, not a third-party security audit.
No dependency, migration, shared helper, authentication or governance change was needed.

## Phase 3 changed-file manifest

SHA-256 values below identify the delivered snapshot without a Git commit. validation.md
is excluded from its own hash; generated target logs/databases/settings are not source.

| Status | File | SHA-256 |
| --- | --- | --- |
| Modified | src/main/java/com/changeflow/domain/ChangeRequest.java | FB3D3C4E3885D113ED03DD27783F4A72E1B7B63D88C5ADFC24D6CF14D4D9BAE0 |
| Added | src/main/java/com/changeflow/application/ChangeRequestService.java | B3807368C6F623237D41C0DB4285D775D27B2A4A29A37A92C2C5602395D1188A |
| Added | src/main/java/com/changeflow/api/ChangeRequestController.java | 1979B2C0BFB6E3A2B4D2941F6A1184233CB7F9ED1A0E032F1697303002769548 |
| Added | src/main/java/com/changeflow/api/ChangeRequestDtoMapper.java | 7220461E15EC6EC1A6B5B5E6D9D23AED5F48B42FC130DED93771C373E6659F11 |
| Added | src/main/java/com/changeflow/api/RequestEtags.java | 08EB843C5C00DC3B7C40492EEE12842FE8B4AE1E5D0A26AB20C6A4754832AC04 |
| Added | src/main/java/com/changeflow/api/dto/CreateRequest.java | 945FF10785785A0F8CD3054C80F536CB58EB260176A4F4DF10D9F40D10A6745A |
| Added | src/main/java/com/changeflow/api/dto/EditRequest.java | E13620C29C56CB9FF76D974E993214B93312AC3B37F2DE4848184A04DD385881 |
| Added | src/main/java/com/changeflow/api/dto/ChangeRequestResponse.java | 5A4D28EB4408C42C62992C775F5AE0DAC5989A0C22694E0C44A2BB860727D14F |
| Added | src/main/java/com/changeflow/api/dto/ChangeRequestListResponse.java | 91DD13E879BD9298BEB0E38548A3E703775AAA8A0DD2090086A5A1A81B20CA6F |
| Modified | src/main/java/com/changeflow/infrastructure/persistence/ChangeRequestMapper.java | 4C4B6E58D73DB316B2FD262A559DF4E552FAFE0E33533360BE7D60B9E1DFBDB5 |
| Added | src/test/java/com/changeflow/domain/OwnerLifecycleTest.java | 192D30A261745365A26237DD5E7ED48CEBAC5425DD2DEDC525F7E99817126A38 |
| Added | src/test/java/com/changeflow/integration/OwnerHttpContractTest.java | AAE0C4C4E80D8B572F1E9C217E38BC99CB550071AAB672FAAF640B718A351B64 |
| Added | src/test/java/com/changeflow/integration/OwnerLifecycleHttpTest.java | 1EACC5FE15819AABAA5D27E636F45000118E54853D5CDFC642B12B5A4F426EE4 |
| Added | src/test/java/com/changeflow/integration/ContentValidationHttpTest.java | 1A8365AE7880600B84491AD9CBFF7AACB810DC8097B7246EE1CA7B187E145DF1 |
| Added | src/test/java/com/changeflow/integration/NoOpPatchHttpTest.java | 8E5E461CD88E70415ECD654D81A60DC4AC516CC4D855E47AA6912CBD89DAAD04 |
| Added | src/test/java/com/changeflow/integration/VisibilityHttpTest.java | 35324D34663B9FF423CB20E9CE77E5835D73BD47F7A6FC4AE91C35595A59B645 |
| Modified | src/test/java/com/changeflow/integration/ProblemResponseTest.java | DC63F8E4F2ECA0552D0E1AA579F5E216E9E84B7C8C7F389E1C9DE846048CCAD9 |
| Modified | specs/001-change-request-management/tasks.md | 5155BC056477F1750A9F30D0998A0BC03FC4A4DEAAE5EC20BB26A490CFE4C425 |
| Modified | specs/001-change-request-management/validation.md | Excluded (self-reference) |

# Phase 4 Independent Review Validation Evidence

Date: 2026-11-16 (Asia/Tehran). Scope: T043-T052 only. All work and test runs were
sequential, without additional agents. Historical stop-before-T043 and route-absence
statements in the Phase 3 record describe that earlier batch; this section records the
new independent-review behavior. Stop before T053; ChangesRequested owner mutations
are still deferred to Phase 5.

## Governance, prerequisites and task status

Read AGENTS.md, constitution v1.0.0, the implementation skill, and the reviewed active
specification, plan, tasks, HTTP contract, data model, research and quickstart. The
project owner (user in this chat) explicitly identified the artifacts as reviewed and
authorized this batch only. No independent formal review or full assignment acceptance
is claimed. The prerequisite script selected
`F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/specs/001-change-request-management`;
available documents remained research.md, data-model.md, contracts/, quickstart.md and
tasks.md. Dependencies T042 onward were followed in numeric order.

| Checklist | Total | Checked | Unchecked | Status |
| --- | ---: | ---: | ---: | --- |
| requirements.md | 16 | 16 | 0 | PASS |

Checklist files were not modified. `.specify/extensions.yml` was absent before and
after implementation; there were no pre/post hooks to execute. Existing .gitignore
already covers Maven output, caches, temporary file databases, local configuration and
credential captures. No other ignore file was needed. `git rev-parse --git-dir` again
reported that this is not a Git repository; file hashes identify the source snapshot.

| Tasks | Disposition |
| --- | --- |
| T001-T042 | Preserved; fresh checkpoint before T043: 81 tests passed |
| T043-T046 | Tests authored before corresponding behavior; real HTTP red and Domain compilation block recorded separately |
| T047-T050 | Independent Domain review, feedback DTO/validation, transactional Application orchestration and thin review routes implemented |
| T051 | Real parent/metadata flush, checked/runtime failure, database constraint, commit-phase rollback and lost-confirmation tests passed |
| T052 | Review/feedback/rollback and complete regression green; evidence and task markers updated |
| T053-T081 | Unstarted and unchecked; not included in this batch |

## Current environment and executed commands

Working directory throughout:
`F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow`.
The user's changeFlow casing refers to this same Windows directory.

- Maven on PATH: C:/Program Files/apache-maven-3.8.5/bin/mvn.cmd, Apache Maven 3.8.5,
  revision 3599d3414f046de2324203b78ddcf9b5e4388aa0.
- Maven runtime: Oracle GraalVM Java 17.0.11 at
  C:/graalvm-ee-java17-windows-amd64-21.3.10/graalvm-ee-java17-21.3.10.
- POM remains unchanged: Boot parent 3.5.16 and compiler release 17. Compilation logs
  report release 17; javap reports class-file major version 61. No dependency resolution
  or version change was needed; all Maven commands ran offline with the existing cache.
- Temporary files: target/phase4/tmp and isolated file H2 databases under
  target/test-databases. Credentials/configuration were generated privately in memory.
  No packaged JAR or default data/ directory was created. Root build-guide consolidation
  remains assigned to its later task; the commands verified for this batch are below.

Commands executed (log redirection omitted):

```powershell
.specify/scripts/powershell/check-prerequisites.ps1 -Json -RequireTasks -IncludeTasks
git rev-parse --git-dir
mvn -version
New-Item -ItemType Directory -Force target/phase4/tmp | Out-Null

# Checkpoint, initial full regression, and final corrected regression used this
# same complete test invocation, without test selections/exclusions/skips:
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase4/tmp' test

# Three sequential initial HTTP runs used this command with the single class
# ReviewHttpContractTest, IndependentReviewHttpTest, then ReviewFeedbackHttpTest:
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase4/tmp' '-Dtest=ReviewHttpContractTest' test

mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-Dtest=ReviewRulesTest' test
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-Dtest=ReviewRulesTest,OwnerLifecycleTest,ContentRulesTest' test

# Executed separately after T048 and T049:
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' compile

mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase4/tmp' '-Dtest=ReviewRulesTest,ReviewHttpContractTest,IndependentReviewHttpTest,ReviewFeedbackHttpTest' test
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase4/tmp' '-Dtest=ReviewAtomicityHttpTest' test

javap -verbose target/classes/com/changeflow/ChangeFlowApplication.class
```

## Actual red, green and correction history

Log names below are under ignored target/phase4. No HTTP red was inferred from missing
classes: T043-T045 compiled and executed against the existing application. T046's
missing methods are explicitly recorded as a compilation block, not a failed business
assertion. No failing test was disabled or excluded.

| Log | Actual outcome |
| --- | --- |
| owner-checkpoint.log | 81 tests, 0 failures/errors/skips, BUILD SUCCESS, 40.414 s; checkpoint green before T043 |
| t043-red.log | 5 tests, 5 assertion failures against absent review routes |
| t044-red.log | 10 cases, 8 assertion failures; 2 authentication/visibility cases already passed |
| t045-red.log | 10 cases, 10 assertion failures against absent feedback route |
| t046-blocked.log | Compilation blocked by absent approve/requestChanges Domain methods; no JUnit execution |
| t047-green.log | 22 tests passed: 6 review Domain, 6 owner Domain, 10 content rules |
| t048-build.log | Compilation succeeded after feedback DTO and trailing-token validation wiring |
| t049-build.log | Compilation succeeded after transactional review orchestration |
| review-first.log | 31 Domain/review/feedback cases passed, BUILD SUCCESS, 31.462 s |
| t051-first.log | 14 atomicity cases passed, BUILD SUCCESS, 28.033 s |
| t052-full-first.log | 128 tests, 1 failure: old OwnerHttpContractTest expected review route absence (404), actual missing-precondition response was 428 |
| t052-final.log | 128 tests, 0 failures, 0 errors, 0 skips, BUILD SUCCESS, 48.630 s; finished 2026-11-16 20:03:40 +03:30 |

The only regression correction was to the obsolete owner test's unsupported-action
list: approve/request-changes are now real routes, while cancel/withdraw/execute/publish/
history remain absent. Their denial and unchanged-storage assertions remain. New review
tests cover both live routes. Strict trailing-token rejection was enabled with the
existing Jackson configuration so malformed request-changes JSON cannot accept a valid
first object and ignore a second root value. No new library was introduced.

The expected unique-constraint and negative migration/schema tests log database/startup
errors. They pass by checking the intended failure and fresh storage. These are not
unresolved operational failures or simulated successful acceptance.

## Final discovered tests

Read all TEST-*.xml files in target/surefire-reports after the final run and compared
them to source classes ending in Test: 16 classes, 16 report suites, no default exclusion.
Parameterized invocations are included in counts; loops are not counted as extra tests.

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| ContentRulesTest | 10 | 0 | 0 | 0 |
| OwnerLifecycleTest | 6 | 0 | 0 | 0 |
| ReviewRulesTest | 6 | 0 | 0 | 0 |
| OpaqueCredentialCodecTest | 3 | 0 | 0 | 0 |
| ContentValidationHttpTest | 20 | 0 | 0 | 0 |
| IndependentReviewHttpTest | 12 | 0 | 0 | 0 |
| LocalAdministrationTest | 10 | 0 | 0 | 0 |
| MigrationStartupTest | 4 | 0 | 0 | 0 |
| NoOpPatchHttpTest | 3 | 0 | 0 | 0 |
| OwnerHttpContractTest | 7 | 0 | 0 | 0 |
| OwnerLifecycleHttpTest | 6 | 0 | 0 | 0 |
| ProblemResponseTest | 9 | 0 | 0 | 0 |
| ReviewAtomicityHttpTest | 14 | 0 | 0 | 0 |
| ReviewFeedbackHttpTest | 10 | 0 | 0 | 0 |
| ReviewHttpContractTest | 5 | 0 | 0 | 0 |
| VisibilityHttpTest | 3 | 0 | 0 | 0 |
| **Total** | **128** | **0** | **0** | **0** |

The five new review suites account for 47 tests. All 81 pre-existing test cases also
pass, with the obsolete route-absence assertion updated to the current scope.

## Persisted behavior and transaction evidence

- Review tests seed PendingReview independently of owner routes and use real HTTP,
  isolated file H2, Flyway and JPA validation. Fresh stored(id) verification runs in
  REQUIRES_NEW after each HTTP operation. Record equality compares all parent fields,
  revision and decision rows, not just status/state. Shared test helpers were unchanged.
- Independent Reviewer and dual-role non-owner approval/request-changes succeed.
  Parent proposal/owner/identity/createdAt remain unchanged. Approval preserves null or
  prior latest feedback; request-changes stores only the normalized new latest feedback.
  updatedAt and generated revision change with the state; revision increases by one.
  Each success has exactly one metadata row with actual authenticated reviewer ID,
  action, matching resulting revision and the same Instant as parent updatedAt.
- Responses match later parent GET byte-for-byte, including timestamps, feedback,
  deterministic DTO order, generated revision and ETag. Action If-Match conditions the
  parent representation obtained from GET. Malformed/overflow/wildcard/list/multiple/
  weak/cross-item/stale tags follow the shared 400/412/428 contract. Self-review returns
  403 before failed comparison; old tags return 412 before current-state rejection.
- Both forms of self-review reject for Requester, Reviewer and dual-role visible owners.
  Neither permission spoofing in headers nor ownership grants review authority. Other
  Requester-only/neither users receive invisible 404, unauthenticated/revoked credentials
  receive 401, visible forbidden owners receive 403. Invalid visible states/retries give
  409 with current tags; invisible Drafts remain 404. Every rejection preserves the full
  fresh parent/feedback/version/decision snapshot.
- Already-authenticated proof is passed directly to the public service proxy, without
  another HTTP filter pass. Each review operation observes Reviewer removal and subsequent
  credential revocation before version/state checks. The existing transaction-policy test
  also inspects the new public methods: READ_COMMITTED, timeout 10 s, rollback on checked
  Exception, not read-only. Credential then UserAccess locks are still provided by the
  unchanged current-access guard inside the public Application transaction.
- Missing/null/blank/empty/oversized feedback rejects; numeric/array/non-object/malformed/
  trailing JSON rejects. Exact 2,000 normalized code points succeed for ASCII and
  supplementary characters; 2,001 reject without truncation. Java outer whitespace is
  stripped and internal spaces/newlines remain. Matching controlled fields and unknown
  fields reject the entire write; no valid explanation partially persists with them.
- Required-feedback errors, optional Approve body rules, 406/415 media handling and
  empty bytes without Content-Type are checked. Approve accepts only absent bytes or
  an empty JSON object. Empty bytes may carry an unrelated media header; nonempty bodies
  require application/json. Controllers contain transport mapping only; Domain/Application
  own state, self-review, current permission, expected revision and atomic orchestration.
- Authorized owner/Reviewer/dual-role reads expose latest feedback; invisible callers
  receive the same missing problem apart from instance path, with no feedback/existence
  detail. Lists omit hidden requests. Actual INFORMATION_SCHEMA inspection confirms
  ReviewDecision has exactly ID, REQUEST_ID, REVIEWER_ID, ACTION, DECIDED_AT and
  RESULTING_REQUEST_VERSION. Stored decision rows contain neither old nor latest
  explanation text. No history column/table or public history route was added.
- Approved is immutable through owner edit/submit, either new review, deletion and
  unsupported operations. Tests start from an actually approved request with a decision
  row, then compare all stored fields/revision/feedback/metadata after each denial.

T051 uses file-local test decorators around the real JPA ports. They do not replace
storage with mocks or add production fault endpoints. Twelve cases exercise both actions
against runtime and checked exceptions after actual parent UPDATE/flush, runtime and
checked exceptions after actual metadata INSERT/flush, a real duplicate request/version
constraint failure, and a beforeCommit synchronization failure. All return safe 500
without a success ETag, report STATUS_ROLLED_BACK through transaction synchronization,
and leave the complete freshly inspected parent and decision list unchanged. Counters
verify the intended flush point was reached rather than failing earlier.

The controlled commit-phase failure occurs in Spring beforeCommit **before physical
database commit**. It demonstrates that proxy commit processing failure cannot produce
an API success and that this known failure rolls back. It is not a claim that an actual
ambiguous database commit acknowledgement failure was injected.

Two separate test-only response-discard cases capture the successful HTTP representation
after the real service commit, replace the confirmation with safe 503, and verify
STATUS_COMMITTED plus the complete changed parent and one metadata row. This models
unavailable confirmation; it is not an actual network outage. It explicitly shows why
non-success alone cannot prove rollback or justify automatic retries. No uncontrolled
physical commit/transport failure outcome is claimed.

## US2 traceability and deferred cases

| Scenario | Executed evidence / disposition |
| --- | --- |
| US2-AS01 | IndependentReviewHttpTest success cases and ReviewHttpContractTest committed-parent response checks |
| US2-AS02 | Same success cases plus ReviewFeedbackHttpTest normalization/metadata checks |
| US2-AS03 | Both Domain and HTTP self-approval denials, including dual-role owner, fresh unchanged data |
| US2-AS04 | HTTP missing permission/authentication, removed role/revoked credential, direct service recheck |
| US2-AS05 | Wrong-state checks; invisible Draft 404, visible ChangesRequested 409; Domain rejects both |
| US2-AS06 | Dual-role non-owner succeeds with both actions and matching stored metadata |
| US2-AS07 | Both Domain and HTTP self-request-changes denial, including dual-role owner |
| US2-AS08-10 | Deferred to T061: genuinely overlapping approve/approve, changes/changes and mixed Reviewers with required repetitions |
| US2-AS11 | Both repeated decisions reject with old tag 412/current tag 409, no replay or further metadata |
| US2-AS12 | Seven parameterized invalid explanation cases plus non-object/invalid JSON cases; unchanged snapshots |
| US2-AS13 | Normalized internal whitespace and 2,000/2,001 ASCII/supplementary boundaries |
| US2-AS14 | ReviewAtomicityHttpTest controlled rollback and distinct unavailable-confirmation evidence |
| US2-AS15 | Actual reviewed feedback read privacy for owner/Reviewer/dual-role vs hidden users |
| US2-AS16 | Deferred to US4 T055/T059: replacement after actual revise/resubmit journey |

Seeded PendingReview with prior feedback proves approval preservation and request-changes
replacement now. It does not prove owner revision/resubmission or the end-to-end US4
feedback loop. T053-T059 retain those cases, including US4-AS05. The Domain still permits
owner edit/submit only in Draft. Sequential stale retry checks here are not a substitute
for T061's true overlapping review races or 100 repetitions. No concurrency acceptance,
full assignment acceptance, packaging/admin-process/restart acceptance or PostgreSQL
compatibility is claimed.

No unresolved failure or blocker remains for this batch. No dependencies, migration,
decision schema, shared helper, public administration/publication endpoint or historical
explanation storage changed. Scoped import review found no forbidden Domain/Application
dependency, manual version assignment, transaction self-invocation, production manual
commit/rollback, request bulk update or AUTO_SERVER. The credential-pattern scan matched
only generated/external test password references, with no live credential/private key;
this is a scoped source check, not an external security audit. Post-hook check again
found no extensions.yml.

## Phase 4 changed-file manifest

Hashes identify this batch without a Git commit. validation.md is excluded from its own
hash. Generated target/phase4 logs, caches and temporary databases are not source files.

| Status | File | SHA-256 |
| --- | --- | --- |
| Modified | src/main/java/com/changeflow/domain/ChangeRequest.java | 58C8105C41ABF07235CDBE97D5900DB8F534848FB497433AE80858719A4DB71B |
| Modified | src/main/java/com/changeflow/application/ChangeRequestService.java | 5DE2FC1044C91A6A7E82BCE1D8630F6B1E5F018BFF1AD34479F84B78568D9614 |
| Modified | src/main/java/com/changeflow/api/ChangeRequestController.java | 2C2336AE15978B49AC20CAC02EE71EF47593048A01C90177BCC38CA95A787F1C |
| Added | src/main/java/com/changeflow/api/dto/RequestChangesRequest.java | EA287C0571DB7A2B73490C8E11CEFD8E7C75EA2BBC92165E7DF979A522FD2EFB |
| Modified | src/main/java/com/changeflow/configuration/JsonConfiguration.java | 41BD3887CAE364BF168432215786E468A59F49E7A56E5A1753BA9382A5225AA7 |
| Added | src/test/java/com/changeflow/domain/ReviewRulesTest.java | F68CDD8BC066E8EF5174640640A592E0B291B8B039D351A07227C795616C673C |
| Added | src/test/java/com/changeflow/integration/ReviewHttpContractTest.java | 897261271F1A2D1462B87C5577D963AF59FCCE6E60306AB998040F30BE6C8458 |
| Added | src/test/java/com/changeflow/integration/IndependentReviewHttpTest.java | 579193B3BC4C94342D62CF65DE850F3F82EDDB6DC9552FBDB8E5E8EB86C09E24 |
| Added | src/test/java/com/changeflow/integration/ReviewFeedbackHttpTest.java | DC4E2BCFD853861F9B8B861C08813E0BEFD11A1BAD4965D6758325F57DBB1F3A |
| Added | src/test/java/com/changeflow/integration/ReviewAtomicityHttpTest.java | 464579E7D53BCC40C55D341058DE28F068CD588A183E22A49CFF5AF9A1298C57 |
| Modified | src/test/java/com/changeflow/integration/OwnerHttpContractTest.java | D26A826AD7B935FD9D143129AAF49AD315B589EFD9B3F9538495733166CBA192 |
| Modified | specs/001-change-request-management/tasks.md | A184B576351A74E01601844CFC3E1F2702B3DD805D06369403400E608905B265 |
| Modified | specs/001-change-request-management/validation.md | Excluded (self-reference) |

# Phase 5 Revision and Resubmission Validation Evidence

Date: 2026-11-16 (Asia/Tehran). Scope: T053-T059 only, sequential task/test execution,
without additional agents. This section supersedes earlier historical statements that
ChangesRequested owner mutations and US4 journeys were pending. T060 and later work
remain unstarted; this is functional lifecycle evidence, not full assignment acceptance.

## Gates, environment and scope

Read the governing AGENTS.md/constitution, implementation skill and reviewed active
feature artifacts, including spec, plan, tasks, HTTP contract, data model, research and
quickstart. The project owner (user in this chat) identifies these artifacts as reviewed
and authorizes Phase 5 only after verifying T052. No independent formal review is claimed.
Prerequisite script selected the absolute feature directory
`F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/specs/001-change-request-management`
with the expected research.md, data-model.md, contracts/, quickstart.md and tasks.md.

| Checklist | Total | Checked | Unchecked | Status |
| --- | ---: | ---: | ---: | --- |
| requirements.md | 16 | 16 | 0 | PASS |

Checklist markers were read only. `.specify/extensions.yml` was absent at the pre- and
post-execution checks; there were no hooks to dispatch. Existing .gitignore covers Maven
output/cache, local databases, temporary files and secret captures; no other ignore file
was required. `git rev-parse --git-dir` again found no Git repository. No branch/commit
or Git diff is claimed; the manifest below identifies the delivered snapshot.

Working directory for all commands:
`F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow` (same Windows directory as
the user's changeFlow spelling). Maven -version reported Apache Maven 3.8.5 on PATH at
C:/Program Files/apache-maven-3.8.5/bin/mvn.cmd, using Oracle GraalVM Java 17.0.11 at
C:/graalvm-ee-java17-windows-amd64-21.3.10/graalvm-ee-java17-21.3.10. All Maven invocations
ran offline against the existing target/maven-repository cache; no resolution block,
network access, new dependency or version change was needed. The unchanged POM pins Boot
3.5.16/release 17; javap again reported major version 61. POM SHA-256 remains
AC66E684CFDAF160D76B329FC5CDECD39AC7FC56E186CB5AB513A4A58D7C25E8.

## Test-first sequence and actual commands

Commands executed (log redirection omitted):

```powershell
.specify/scripts/powershell/check-prerequisites.ps1 -Json -RequireTasks -IncludeTasks
git rev-parse --git-dir
mvn -version
New-Item -ItemType Directory -Force target/phase5/tmp | Out-Null

# Before T053, then again for T059 after implementation. No test exclusions/skips:
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase5/tmp' test

# Three sequential initial HTTP runs used this invocation with the single class
# RevisionHttpContractTest, RevisionResubmissionHttpTest, then FeedbackRetentionHttpTest:
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase5/tmp' '-Dtest=RevisionHttpContractTest' test

mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-Dtest=RevisionRulesTest' test
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-Dtest=RevisionRulesTest,OwnerLifecycleTest,ReviewRulesTest,ContentRulesTest' test
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase5/tmp' '-Dtest=RevisionHttpContractTest,RevisionResubmissionHttpTest,FeedbackRetentionHttpTest,RevisionRulesTest' test

javap -verbose target/classes/com/changeflow/ChangeFlowApplication.class
```

Every test class T053-T056 was authored and executed before T057/T058 behavior. Unlike
earlier missing-method blocks, all four initial suites compiled and ran real assertions.
Domain tests use existing methods and assertDoesNotThrow to make rejection of a valid
returned-state operation an explicit assertion failure. Existing validation/authorization
cases that already passed are counted as passes, not invented red evidence.

| Log under target/phase5 | Actual result |
| --- | --- |
| review-checkpoint.log | 128 tests, 0 failures/errors/skips, BUILD SUCCESS, 46.184 s; green through T052 before T053 |
| t053-red.log | 17 cases, 3 assertion failures: valid returned operations still got 409; 14 existing rejection cases passed |
| t054-red.log | 8 cases, 3 assertion failures on valid returned-owner journeys; 5 permission/invalid-state cases passed |
| t055-red.log | 2 lifecycle cases, 2 assertion failures at revision after actual create/submit/request-changes succeeded |
| t056-red.log | 6 pure Domain cases, 5 assertion failures on returned-state rules; PendingReview/Approved rejection passed |
| t057-green.log | 28 Domain/content tests, 0 failures/errors/skips, BUILD SUCCESS, 21.588 s |
| t058-integration.log | All 33 new revision/retention cases passed, BUILD SUCCESS, 31.628 s |
| t059-full.log | 161 tests, 0 failures, 0 errors, 0 skips, BUILD SUCCESS, 49.984 s; finished 2026-11-16 20:19:50 +03:30 |

No business assertion was disabled or excluded. The implementation resolved the real
state-rejection failures directly; no full-regression failure remained to fix.

## Implementation disposition

The only functional production change is the shared independent Domain guard used by
edit and submit: Draft and ChangesRequested are editable/submittable, while PendingReview
and Approved still reject. Existing immutable copies already preserve ID, owner,
createdAt, omitted proposal fields and latestExplanation. Actual content changes retain
the current state; submission/resubmission changes it to PendingReview. No-op behavior
continues to return the unchanged value without a timestamp or version mutation.

T058 reuses the existing public Application edit/submit transactions without adding a
second state branch. They already lock/recheck current access, enforce visibility and
ownership, compare expected revision before Domain state checks, flush changed writes,
and explicitly verify unchanged versions for no-ops. Those exact paths now invoke the
extended Domain behavior. Only their method documentation was clarified; no duplicate
business rule or alternative transaction path was introduced. Direct-proxy and HTTP
tests verify the existing boundary for ChangesRequested, including removed roles and
revoked proof. Controller, DTOs, routes, enums, adapters, migration and dependencies are
unchanged. Review still requires an independent current Reviewer and PendingReview.

## Fresh persisted data and full lifecycle observations

The tests use real RANDOM_PORT HTTP and isolated temporary file H2 databases with
Flyway/JPA validation. Shared helpers were unchanged. stored(id) inspects committed
parent/metadata in a REQUIRES_NEW transaction after operations; whole-record equality
includes identity, owner, state, proposal, feedback, createdAt, updatedAt, revision and
decision rows. Corrupted-storage cases use separate transactions and fresh SELECT * maps.

- Owned Requester and dual-role ChangesRequested revisions succeed, preserve state,
  original identity/owner/creation time and feedback, normalize supplied content and
  preserve omitted content. Resubmission preserves all proposal/feedback and returns
  PendingReview. Each actual mutation increments the managed revision by one; owner
  revision/resubmission appends no review metadata.
- Empty and normalized-identical returned PATCH return unchanged JSON, ETag, timestamp,
  revision, feedback and metadata. Seeded nanoseconds
  `2026-11-16T10:00:00.123456789Z` survive the write/read representation unchanged.
  A no-op after an actual review/revision also preserves its complete stored snapshot.
  This is sequential no-op evidence, not the deferred overlapping commit-race test.
- Existing parent If-Match handling applies on the same PATCH/submit paths: missing
  428, invalid/unsupported 400, weak/cross-item/stale 412. Old-tag repeated resubmission
  rejects with 412; a current tag in PendingReview rejects with 409. PendingReview and
  Approved reject both owner operations, including empty PATCH, with unchanged storage.
- Non-owner Requester/neither users receive invisible 404; visible Reviewer/dual-role
  non-owners receive 403 even with a mismatching version. Removed Requester permission
  overrides ownership: a remaining Reviewer can read the returned parent but cannot
  revise/resubmit (403); neither role yields 404. Revoked credentials yield 401. A
  remaining Requester still completes revision/resubmission. Direct service attempts
  with previously authenticated proof recheck permissions/revocation before revision,
  without relying on another bearer-filter pass.
- Each proposal column is checked independently for supplied null, empty, Java-whitespace
  and max+1 content alongside a valid change to another field (12 parameterized cases).
  The entire operation rejects with unchanged stored fields/feedback/version/metadata.
  Exact 200/5,000/2,000 normalized limits succeed for ASCII and supplementary code points;
  one above rejects without truncation. Partial title edits preserve description/reason.
  Matching controlled fields, response-only feedback, unknown fields and malformed JSON
  reject the whole write. Submitted controlled fields also reject without state change.
- For each of title/description/reason, an invalid stored ChangesRequested column is
  deliberately seeded. Submit returns INVALID_CONTENT and fresh full-row maps plus
  decision counts remain unchanged. It does not repair or rewrite the invalid text.
- The first complete HTTP journey creates a Draft, submits it, receives independent
  request-changes with normalized feedback, revises, performs a no-op, resubmits and
  receives independent approval. Every state/version step and complete proposal/identity
  snapshot is checked. The same feedback survives revision, resubmission and approval.
  It finishes Approved with two metadata rows (RequestChanges, Approve); earlier metadata
  remains identical and each new row matches the generated parent revision/time and
  authenticated reviewer. Owner attempts to replace, clear or even resubmit matching
  latestExplanation reject. Both dual-role self-review attempts and later Approved
  mutations reject with unchanged parent and metadata.
- The second complete journey actually revises and resubmits a returned request, then
  obtains a new independent request-changes decision. Premature, stale and invalid
  explanation attempts leave the first feedback and metadata unchanged. Only the valid
  new decision replaces First feedback with Second feedback. A further resubmission and
  approval retain Second feedback; the final metadata list has three rows (RequestChanges,
  RequestChanges, Approve). Prefix rows are unchanged. Actual schema/row inspection
  confirms metadata contains only ID/request/reviewer/action/time/resulting revision,
  with neither explanation text stored there. Parent GET exposes only the latest feedback.

Full regression also re-executed the prior real JPA checked/runtime/commit-phase rollback
and unavailable-confirmation suites. No new rollback, overlap, outage or restart guarantee
is inferred solely from the additional sequential lifecycle tests.

## Final test discovery

All target/surefire-reports/TEST-*.xml files were read after the final run: 20 source
Test classes and 20 report suites. Loops within one case are not counted as additional
JUnit tests. The four new suites account for 33 tests; all 128 earlier cases still pass.

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| ContentRulesTest | 10 | 0 | 0 | 0 |
| OwnerLifecycleTest | 6 | 0 | 0 | 0 |
| ReviewRulesTest | 6 | 0 | 0 | 0 |
| RevisionRulesTest | 6 | 0 | 0 | 0 |
| OpaqueCredentialCodecTest | 3 | 0 | 0 | 0 |
| ContentValidationHttpTest | 20 | 0 | 0 | 0 |
| FeedbackRetentionHttpTest | 2 | 0 | 0 | 0 |
| IndependentReviewHttpTest | 12 | 0 | 0 | 0 |
| LocalAdministrationTest | 10 | 0 | 0 | 0 |
| MigrationStartupTest | 4 | 0 | 0 | 0 |
| NoOpPatchHttpTest | 3 | 0 | 0 | 0 |
| OwnerHttpContractTest | 7 | 0 | 0 | 0 |
| OwnerLifecycleHttpTest | 6 | 0 | 0 | 0 |
| ProblemResponseTest | 9 | 0 | 0 | 0 |
| ReviewAtomicityHttpTest | 14 | 0 | 0 | 0 |
| ReviewFeedbackHttpTest | 10 | 0 | 0 | 0 |
| ReviewHttpContractTest | 5 | 0 | 0 | 0 |
| RevisionHttpContractTest | 17 | 0 | 0 | 0 |
| RevisionResubmissionHttpTest | 8 | 0 | 0 | 0 |
| VisibilityHttpTest | 3 | 0 | 0 | 0 |
| **Total** | **161** | **0** | **0** | **0** |

## Traceability, completed deferrals and remaining work

| Scenario / obligation | Actual evidence |
| --- | --- |
| US4-AS01 | RevisionHttpContractTest partial/boundary cases, RevisionResubmissionHttpTest Requester/dual-role complete revision, pure RevisionRulesTest |
| US4-AS02 | Same routes and full resubmission assertions preserve identity/proposal/feedback and append no metadata |
| US4-AS03 | Non-owner Requester/Reviewer/dual-role/neither, removed-role and revoked-proof cases inspect unchanged data |
| US4-AS04 | PendingReview/Approved mutation and no-op denials; old/current repeat checks |
| US4-AS05 | FeedbackRetentionHttpTest.completeLifecyclePreservesFeedbackThroughRevisionResubmissionAndApproval |
| US2-AS16 | FeedbackRetentionHttpTest.onlyANewSuccessfulRequestChangesAfterResubmissionReplacesFeedback, with actual multiple cycles and metadata inspection |
| Returned origin of US1-AS08 | Actual resubmission followed by old-tag 412 and current-tag 409 repeat, unchanged committed data |
| Returned US1-AS10/12 | Omission/partial edit plus 12 mixed valid/invalid field cases |
| Returned US1-AS13 | Each column's exact/max+1 ASCII and supplementary Unicode boundaries |
| Returned US1-AS14 | Deliberately invalid stored content in each returned proposal column; submission rejects without repair |
| Returned no-op and controlled-field cases | Seeded nanosecond/identical representation and actual-reviewed-parent no-ops; whole-write rejection of matching protected fields |

T053-T059 are complete and marked [X]. Previously deferred returned-content and actual
resubmission-feedback journeys above are now executed, rather than merely tested with
seeded PendingReview. No unresolved failure or blocker remains for this batch.

T060-T081 remain unstarted and unchecked. Genuine overlapping review pairs, owner races,
revocation races, required 100 repetitions, measured contention/outages, provider-level
commit races, restart, packaging and final governance/docs acceptance remain later work.
US2-AS08/09/10 still belong to T061; this phase does not complete US3 hardening. No full
assignment acceptance, production readiness or PostgreSQL portability is claimed.

Scoped source scans found the same seven mappings, no forbidden Domain/Application
imports, manual request-version assignment, production manual commit/rollback, request
bulk update or AUTO_SERVER. A comparison against the Phase 4 hash manifest found only
the expected Domain, service documentation and tasks changes among those existing files.
The four new tests and this appended evidence are listed below. A scoped secret-pattern
scan matched only generated/external test password references, with no live credential
or private key. No default data/ directory or packaged JAR was created. This is scoped
source/validation evidence, not an external audit. Post-hook check found no extensions.yml.

## Phase 5 changed-file manifest

Hashes identify this batch without a Git commit. validation.md is excluded from its own
hash. Generated target/phase5 logs, caches and temporary file databases are not source.

| Status | File | SHA-256 |
| --- | --- | --- |
| Modified | src/main/java/com/changeflow/domain/ChangeRequest.java | F9AC7CA6C242402949154A9039A5D2FDC46B338C960F713ED52709230309E786 |
| Modified | src/main/java/com/changeflow/application/ChangeRequestService.java | EB9EF3C69F80DB34CC7EC06863E6E56C794ADE58BEC745246B171287A65CCC75 |
| Added | src/test/java/com/changeflow/integration/RevisionHttpContractTest.java | 0FEB6F3DB7B3CD162D9117B3013AC34DE9EB7E0413F5AF362BA0ABED671CBA74 |
| Added | src/test/java/com/changeflow/integration/RevisionResubmissionHttpTest.java | C2B0AB5BCF6675AB918F79D7091D199C316E2D56269B8566FD8A0BC0CAC76BBA |
| Added | src/test/java/com/changeflow/integration/FeedbackRetentionHttpTest.java | AE91112F09C2E27271AEC432500926978270BC66E79BACD4B612D3DB683FE079 |
| Added | src/test/java/com/changeflow/domain/RevisionRulesTest.java | FE8F306A0FD5F00C78DE8E61104A534CEDBDD634367F3E0B2ABEFB1398DBA518 |
| Modified | specs/001-change-request-management/tasks.md | C295B4C8C89B5ACC51B1FD5E4DD37CE298E9AEAD846080125FFC0D9948FD9057 |
| Modified | specs/001-change-request-management/validation.md | Excluded (self-reference) |

# Phase 6 Concurrency and Security Hardening Evidence

Date: 2026-11-16 (Asia/Tehran). Scope: T060-T072 only. Tasks and Maven runs executed
sequentially without additional agents. Concurrent threads described below are the
required HTTP/storage/admin test workers, not delegated implementation agents. Stop
before T073; final documentation, distribution and acceptance tasks remain unstarted.

## Gates and environment

Read AGENTS.md, constitution v1.0.0, the implementation skill and reviewed active feature
spec/plan/tasks/contract/data-model/research/quickstart. Authorization is the project
owner's scoped request in this chat, identifying those artifacts as reviewed. No separate
formal review or full assignment acceptance is invented. Prerequisite script selected
`F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/specs/001-change-request-management`
with the expected available documents. requirements.md remained read only:

| Checklist | Total | Checked | Unchecked | Status |
| --- | ---: | ---: | ---: | --- |
| requirements.md | 16 | 16 | 0 | PASS |

No extensions.yml existed before or after execution; there were no pre/post hooks.
Existing .gitignore covers generated Maven/cache/log/database/temp/secret output and
needed no change. git rev-parse again found no Git repository; source hashes below
identify this batch instead of a commit. No branch or Git diff is claimed.

Working directory throughout:
`F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow` (same Windows directory
as the user's changeFlow spelling). Maven -version again reported Maven 3.8.5 at
C:/Program Files/apache-maven-3.8.5/bin/mvn.cmd using Oracle GraalVM Java 17.0.11.
All Maven invocations used the existing target/maven-repository cache offline. POM,
Boot 3.5.16 pin, dependencies and compiler release 17 are unchanged; javap reports
major version 61. POM SHA-256 remains
AC66E684CFDAF160D76B329FC5CDECD39AC7FC56E186CB5AB513A4A58D7C25E8.
Temporary paths are target/phase6/tmp and isolated target/test-databases locations.
No default data/ or packaged JAR was created. Packaged/separate-process checks remain
Phase 7; these results concern real local HTTP plus embedded file H2 in test JVMs.

## Execution order, actual commands and outcomes

T059 was reverified before T060. The shared coordination support was completed and
compiled before T061-T069. Each cohort suite was then authored/run in numeric order.
The entire cohort was authored and executed before the T070 production correction.
T071 followed the corrected audit checks; T072 reran the entire normal test lifecycle,
including every required repetition. No count was reduced or excluded.

Commands executed (log redirection omitted):

```powershell
.specify/scripts/powershell/check-prerequisites.ps1 -Json -RequireTasks -IncludeTasks
git rev-parse --git-dir
mvn -version
New-Item -ItemType Directory -Force target/phase6/tmp | Out-Null

# Before T060, and again as the final T072 run, without selection/exclusion/skip:
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase6/tmp' test
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' test-compile

# Focused invocations used this exact option set with each -Dtest selection in
# the table below, sequentially. The smoke selection was diagnostic only:
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase6/tmp' '-Dtest=ReviewConcurrencyHttpTest#coordinationUsesDistinctRealHttpTransactionsAndContexts' test

javap -verbose target/classes/com/changeflow/ChangeFlowApplication.class
```

| Log under target/phase6 | -Dtest selection / actual outcome |
| --- | --- |
| functional-checkpoint.log | No selection: 161 tests, 0 failures/errors/skips, BUILD SUCCESS, 49.596 s |
| t060-compile.log | test-compile succeeded, 4.359 s; shared support ready before cohort |
| t061-smoke.log | ReviewConcurrencyHttpTest#coordinationUsesDistinctRealHttpTransactionsAndContexts: 1 passed; no repetition-completion claim from smoke alone |
| t061-full.log | ReviewConcurrencyHttpTest: 301 passed (300 required races plus support smoke) |
| t062-full.log | OwnerConcurrencyHttpTest: all 200 required owner races passed |
| t063-first.log | RevocationConcurrencyHttpTest: all 20 orderings passed |
| t064-first.log | StorageFailureHttpTest: 7 tests, 1 assertion failure; expired transaction returned 500 instead of 503 |
| t064-deadline-diagnostic.log | StorageFailureHttpTest#actualTransactionDeadlineIsEnforcedAfterATestControlledDelay: 1 failure; captured actual JpaSystemException -> Hibernate TransactionException, both with transaction timeout expired |
| t065-first.log | AuthenticationSecurityHttpTest: 8 passed; malformed-cookie fixture caused a Tomcat diagnostic requiring credential redaction |
| t065-corrected.log | AuthenticationSecurityHttpTest: 8 passed after using a valid ignored cookie value |
| t066-first.log | OptimisticPersistenceTest: 3 passed, including provider-level no-op commit conflict |
| t067-first.log | LifecycleRollbackHttpTest: 15 passed |
| t068-first.log | ForbiddenOperationHttpTest: 81 passed |
| t069-first.log | VisibilitySecurityHttpTest: 3 tests, 2 assertion failures from mistakenly treating ProblemDetail's title as proposal data |
| t069-corrected.log | VisibilitySecurityHttpTest: 3 passed after checking actual proposal/feedback leakage instead |
| t070-corrected.log | StorageFailureHttpTest,ProblemResponseTest,RevocationConcurrencyHttpTest: 37 passed, 56.697 s; correction/order audit green before restart |
| t071-first.log | RestartPersistenceTest: 1 passed, 29.053 s |
| t072-full.log | No selection: 801 tests, 0 failures, 0 errors, 0 skips, BUILD SUCCESS; Maven reports 01:47 min; finished 2026-11-16 21:09:47 +03:30 |

No failed business assertion was removed or disabled. One production gap was found:
the observed Hibernate transaction-deadline exception was not classified as storage busy.
ProblemFactory now recognizes that exact provider timeout and emits 503 STORAGE_BUSY.
It does not broadly classify all Hibernate transaction failures as timeouts; a new
regression probe verifies an unrelated provider failure still returns safe INTERNAL_ERROR.
This change belongs in the existing shared classifier used by advice/security handlers;
no change to Domain, Application, access guards or persistence protocol was needed.

T069's correction retained missing/invisible equality and changed the invalid leakage
assertion to reject actual proposal/feedback values, allowing the required ProblemDetail
title. T065's initial malformed cookie contained a generated transient credential and
Tomcat logged it in ignored test output. The fixture was corrected to a syntactically
valid cookie, still rejected as authentication. Two occurrences were redacted from the
ignored log/XML. After the final run, an exact credential-format scan of all Phase 6
logs and Surefire XML/text found zero raw credentials. No credential was put in source,
this evidence or a commit. One attempted scan while the full log was open encountered
a file-sharing lock; the complete scan was rerun successfully after Maven closed it.

## Real overlapping transactions and mandatory repetitions

T060's @TestConfiguration decorates the existing ports only in importing test contexts.
It always delegates real storage, locks and flushes. Events capture the actual
transaction-bound EntityManager and server thread identity; they assert a live transaction
exists. Bounded latches/barriers remain outside production code and public routes.

For review, both HTTP operations load the same parent revision and complete access/
expected-version checks before reaching BEFORE_UPDATE. That barrier is before the
managed entity mutation/flush. Reviewers have distinct Credential and UserAccess rows,
so access locks do not serialize this request race. Each pair asserts distinct contexts
and threads, both matching original-revision loads, one 200, one 412, one generated
parent revision and one metadata insert. Full parent/feedback/metadata snapshots match
only the winner, and the winning write representation matches later GET. No mock-only
version check or sequential stale call substitutes for this proof.

The final run reports successful completion counters after all per-race assertions:

| Required pair | Final successful repetitions |
| --- | ---: |
| approve / approve | 100 |
| request-changes / request-changes, distinct explanations | 100 |
| approve / request-changes | 100 |
| owner edit / edit | 100 |
| owner edit / submit | 100 |
| **Required total** | **500** |

The review suite also executes one separate coordination smoke case (301 JUnit cases
in that class). Actual mixed-pair winners in the final run were Approve 43 and
RequestChanges 57; each won only its own coherent state/feedback/metadata. These are
observed outcomes, not a fairness or priority guarantee. All pairs were also executed
in their initial focused runs; the table deliberately describes the final full run.

Owner races use two different credentials for the same owner. The first HTTP transaction
pauses after Credential/UserAccess locks; the second HTTP transaction locks its own
credential and is observed waiting before UserAccess acquisition in a distinct context.
After release it loads the winning revision and rejects the old If-Match with 412.
This deliberate serialization is the reviewed protocol, not sequential request dispatch.
Edit/submit alternates the lock winner: 50 submissions won and 50 edits won. No edit
after a winning submission succeeds. Each race compares every stored parent field,
version, feedback and decision list after both workers finish, preserving the winner.

## Revocation, authorization, visibility and rollback

- T063 covers create/edit/submit/approve/request-changes, both credential revocation and
  required-role removal, in both lock-winning orders (20 cases). The real proxied admin
  service and HTTP write run in separate worker/context transactions. A write winning
  locks commits first and survives later removal; the waiting remover then commits.
  Removal winning its locks first commits before the waiting write rechecks and rejects.
  The contender is observed blocked before release, not merely issued after removal.
- Event assertions confirm distinct writer/admin contexts. Writes and credential
  revocation lock Credential then UserAccess; role removal locks UserAccess without
  later locking a credential. Fresh checks compare request content/state/owner/time/
  revision/feedback/decision rows, credential revocation and both permission flags.
  Failed create leaves row count unchanged; successful create stores the authenticated
  owner. Removed roles never regain access through ownership or replayed old commands.
- T065 checks uniform JSON 401/Bearer challenges for missing/malformed/invalid/revoked/
  duplicate credentials across protected methods and existing/missing paths. Cookie,
  query and spoofed identity/role headers grant no authentication or authority. Explicit
  bearer writes succeed without CSRF tokens and establish no session cookie; browser
  session cookies cannot authenticate. Exactly one bearer filter is in the chain, with
  no Basic/form/CSRF filter fallback. No public admin/signup/issuer/introspection/history
  surface was added. Responses do not expose credentials, hashes, SQL or hidden feedback.
- T066 uses managed @Version updates in distinct real contexts: only one competing
  provider-level write commits. The HTTP no-op requests an actual optimistic lock/check,
  pauses after it is registered, and a separate test-owned managed writer commits first.
  The no-op then loses at commit with 412 and no success ETag; the winning stored snapshot
  is unchanged. An uncontended no-op does not increment version/time. A returned write
  snapshot remains immutable after a later writer. These provider tests supplement,
  rather than replace, the 300 true overlapping HTTP review races.
- T067 checks 15 real-flush failure cases: create, Draft/ChangesRequested edit, and
  Draft/ChangesRequested submit, each with runtime, checked and beforeCommit failure.
  The failure point is reached exactly once in the real transaction, returns safe 500
  without success ETag, reports STATUS_ROLLED_BACK and leaves full committed snapshots
  and counts unchanged. BeforeCommit faults occur before physical commit. The existing
  review rollback/metadata-constraint and post-commit confirmation-loss suites also run
  again: their known rollback versus committed-but-unconfirmed outcomes remain distinct.
- T068's 80 state/owner-role/actor combinations test stale writes and fresh forbidden
  actions, deletion and absent cancel/withdraw/execute/publish/history operations,
  checking unchanged full business snapshots after each. Fresh valid commands are outside
  this negative matrix and covered by the lifecycle suites; no JUnit case is skipped.
  Its additional controlled-field case rejects matching requesterId/roles/state on
  submit and both review commands, without partial changes. Approved never mutates.
- T069 tests all four states with own/foreign feedback and both role-removal orders.
  Each of the two visibility/removal cases uses 220 seeded rows and checks default/max/arbitrary-offset pages against
  independently computed visibility before pagination, and verifies role changes and
  forged headers do not restore removed visibility. Missing/invisible problems are
  identical apart from instance; unauthorized feedback is absent. All stored rows remain
  unchanged. Remaining Reviewer can independently approve others but cannot self-review
  or create/read owned Drafts; remaining Requester retains creation/owned visibility.
  Existing revision suites also reverify its edit/resubmit permissions.

## Measured storage waits and recovery

Actual final-run HTTP wall times (configured wait plus surrounding request overhead):

| Check | Configured mechanism | Measured ms | Actual response |
| --- | --- | ---: | --- |
| Credential row lock | H2 LOCK_TIMEOUT 2,000 ms | 2,027 | 503 STORAGE_BUSY |
| UserAccess row lock | H2 LOCK_TIMEOUT 2,000 ms | 2,040 | 503 STORAGE_BUSY |
| Pool exhausted with every connection checked out | Hikari acquisition 3,000 ms | 3,037 | 503 STORAGE_BUSY in bearer filter |
| Expired write transaction after controlled 10,500 ms delay | Public transaction timeout 10 s | 10,537 | 503 STORAGE_BUSY |

The first measured pool/row waits were 3,015/2,028/2,092 ms. The original expired
transaction returned 500 after 10,535 ms; its diagnostic retry returned 500 after 10,806 ms.
Actual cause capture was JpaSystemException -> Hibernate TransactionException with the
provider's transaction timeout expired marker. After T070, the same check returned 503
(10,553 ms in the correction run, 10,537 ms in the final run). This demonstrates deadline
enforcement at the next persistence operation after a deliberate delay, not preemptive
thread cancellation or a strict whole-request wall-clock SLO at exactly 10 seconds.

Held credential/user locks are real separate transactions; they are released and joined
before fresh state verification. Exhausted pool connections are released before checking
the unchanged parent. Injected unavailability around real ports checks matching safe
503 STORAGE_UNAVAILABLE in both MVC and filter paths; those are classified test faults,
not a claimed physical file outage. A separate real closed-pool test produces unavailable
in the bearer filter, then a fresh DriverManager connection to the same file verifies the
unchanged full row and decision count. It does not claim automatic recovery of the closed
application pool. Neither timeout/outage becomes 412, a fictitious 401 or success.

Known failures compare storage after recovery instead of inferring rollback from status.
No actual ambiguous physical database commit acknowledgement failure was injected. The
existing controlled post-commit response-discard tests model unavailable confirmation and
prove a non-success response can coexist with a complete committed parent/decision.
No automatic sensitive-operation retry or false rollback guarantee was introduced.

## Restart persistence

T071 starts the normal application with real HTTP on an isolated temporary file. Fresh
tables initially contain no users/credentials/requests/decisions. It creates/reviews/
revises/resubmits/approves real HTTP data, retains a returned request and pending request,
and inserts a managed Draft with nanosecond timestamps. It changes current roles and
revokes a second credential through actual transactional administration.

Before closing: 4 requests covering all states, 3 decision rows, 3 users and 4 credentials.
The first context, Hikari pool and entity manager factory are explicitly closed and
checked closed before the second app opens the identical absolute H2 file with the same
private external configuration. The pool/factory objects are distinct. Private in-memory
images compare every column in requests, decisions, user access, credential hashes/
revocation and Flyway history; equality is asserted as a boolean so digest values cannot
appear in assertion output. No raw credentials or database images are saved as evidence.

After reopening, Approved and nanosecond Draft GET JSON/ETags are byte-identical; returned
and pending states, feedback and all versions survive. Active credentials still work,
the revoked credential yields 401, removed roles yield the correct invisible 404, and
remaining Reviewer visibility remains correct. Flyway retains exactly one successful V1,
with no reset/seeded identities. A second image after reads remains identical. This is
same-JVM sequential file restart evidence, not packaged or simultaneous multi-JVM H2 use.

## Final discovery and scenario accounting

After the final run, all TEST-*.xml reports were independently read and matched to source:
30 Test classes, 30 report suites, 801 tests, zero failures/errors/skips. Parameterized
invocations count as cases; loops are not inflated into extra JUnit tests. No default
exclusion, alternate lifecycle, reduced repetition option or parallel Maven run was used.

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| ContentRulesTest | 10 | 0 | 0 | 0 |
| OwnerLifecycleTest | 6 | 0 | 0 | 0 |
| ReviewRulesTest | 6 | 0 | 0 | 0 |
| RevisionRulesTest | 6 | 0 | 0 | 0 |
| OpaqueCredentialCodecTest | 3 | 0 | 0 | 0 |
| AuthenticationSecurityHttpTest | 8 | 0 | 0 | 0 |
| ContentValidationHttpTest | 20 | 0 | 0 | 0 |
| FeedbackRetentionHttpTest | 2 | 0 | 0 | 0 |
| ForbiddenOperationHttpTest | 81 | 0 | 0 | 0 |
| IndependentReviewHttpTest | 12 | 0 | 0 | 0 |
| LifecycleRollbackHttpTest | 15 | 0 | 0 | 0 |
| LocalAdministrationTest | 10 | 0 | 0 | 0 |
| MigrationStartupTest | 4 | 0 | 0 | 0 |
| NoOpPatchHttpTest | 3 | 0 | 0 | 0 |
| OptimisticPersistenceTest | 3 | 0 | 0 | 0 |
| OwnerConcurrencyHttpTest | 200 | 0 | 0 | 0 |
| OwnerHttpContractTest | 7 | 0 | 0 | 0 |
| OwnerLifecycleHttpTest | 6 | 0 | 0 | 0 |
| ProblemResponseTest | 10 | 0 | 0 | 0 |
| RestartPersistenceTest | 1 | 0 | 0 | 0 |
| ReviewAtomicityHttpTest | 14 | 0 | 0 | 0 |
| ReviewConcurrencyHttpTest | 301 | 0 | 0 | 0 |
| ReviewFeedbackHttpTest | 10 | 0 | 0 | 0 |
| ReviewHttpContractTest | 5 | 0 | 0 | 0 |
| RevisionHttpContractTest | 17 | 0 | 0 | 0 |
| RevisionResubmissionHttpTest | 8 | 0 | 0 | 0 |
| RevocationConcurrencyHttpTest | 20 | 0 | 0 | 0 |
| StorageFailureHttpTest | 7 | 0 | 0 | 0 |
| VisibilityHttpTest | 3 | 0 | 0 | 0 |
| VisibilitySecurityHttpTest | 3 | 0 | 0 | 0 |
| **Total** | **801** | **0** | **0** | **0** |

| Scenario | Evidence executed |
| --- | --- |
| US3-AS01 | Approved owner/review/unsupported-write matrix; full unchanged snapshots |
| US3-AS02 | Spoofed role/identity headers grant no permission; controlled-body rejection; current role removal checks |
| US3-AS03 | 100 owner edit/edit races plus provider-level guarded-update conflict |
| US3-AS04 | 100 owner edit/submit races with both lock winners and post-submission edit denial |
| US3-AS05 | LifecycleRollbackHttpTest 15 cases plus review atomicity/constraint/commit-phase cases |
| US3-AS06 | Existing missing-item owner/review tests, missing/invisible equality and forbidden matrices |
| US3-AS07 | Uniform unauthenticated protected-method matrix, unchanged storage |
| US3-AS08-10 | Current Requester/Reviewer/dual-role visibility, all states, predicate-before-pagination, remaining-role checks |
| US3-AS11 | Same authenticated missing/invisible problems; uniform JSON 401 before lookup without credentials |
| US3-AS12-13 | All-state/role deletion and cancel/withdraw rejection with unchanged snapshots |
| US3-AS14 | Removed roles respected for owned reads/writes/create, including no-role and remaining-role cases |
| US3-AS15 | Actual admin/write races in both role/credential lock-winning orders |
| US3-AS16 | Remaining roles retain only their current visibility/writes; ownership never restores a removed role or permits review |
| US3-AS17 | Lock-winning committed writes survive subsequent removal, including their full parent/decision effect |
| US3-AS18 | Retry after revocation/removal rejects under current permission and preserves the earlier winner |
| US3-AS19 | Matching requesterId/roles/state rejected on submit and both review commands with no partial effect |
| US2-AS08 | 100 true overlapping approve/approve pairs |
| US2-AS09 | 100 true overlapping changes/changes pairs with distinct feedback and only the winner retained |
| US2-AS10 | 100 true overlapping mixed pairs; observed both winning actions, no loser feedback/metadata |

## Audit and final disposition

T060-T072 are complete and marked [X]; no required test in this batch is blocked,
unexecuted or reduced. T073-T081 remain unstarted/unchecked. Final distribution/docs,
packaged startup/admin/restart and final assignment acceptance remain Phase 7.
No PostgreSQL compatibility or production-readiness claim follows from H2 success.

T070 reviewed the service, current-access guard, request store, bearer filter and advice
against these outcomes. Current verified identity contains IDs only, permissions refresh
under the reviewed locks, expected revision precedes state, and managed @Version plus
explicit no-op optimistic checks protect writes. Public transactions retain checked
rollback, flush/snapshot and proxy-commit response ordering. The only production change
is the observed expired-provider-transaction mapping in their shared ProblemFactory.
No access/state policy, schema, metadata explanation storage, route, dependency, retry,
manual request version, production manual commit/rollback, shared EntityManager or bulk
request update was added. Scoped import scans found no forbidden Domain/Application
dependency; seven existing route mappings and compiler release remain unchanged.
The Phase 5 manifest comparison found its existing Domain/service/tests unchanged,
apart from this batch's expected task markers. All new/changed files are below.

## Phase 6 changed-file manifest

SHA-256 identifies this batch without a Git commit; validation.md is excluded from its
own hash. Ignored generated logs, reports, temporary databases and cache are not source.

| Status | File | SHA-256 |
| --- | --- | --- |
| Modified | src/main/java/com/changeflow/api/ProblemFactory.java | A68AFC98D11812429FF4B6D80D098752494909427BFFC5E951D180EF7F3E1EFA |
| Added | src/test/java/com/changeflow/integration/support/ConcurrencyTestConfiguration.java | C258BF1F301CE63C891A223BF2C0D4FC0D884E1964BC2899F80D78865E7775FB |
| Added | src/test/java/com/changeflow/integration/ReviewConcurrencyHttpTest.java | B41EE7A7D41182D01E2185387708AF73D57180694F5B47921E599CBF97E5ABFA |
| Added | src/test/java/com/changeflow/integration/OwnerConcurrencyHttpTest.java | 1D0B0944CF92251326DE87E7645B793D9C58D8A097A4451C2DD874E7B89272A5 |
| Added | src/test/java/com/changeflow/integration/RevocationConcurrencyHttpTest.java | 687A7CCB7DF4BCDC65A6871A60D038E084CE0B47884C3FF58FCDCAEE7CD2BAC0 |
| Added | src/test/java/com/changeflow/integration/StorageFailureHttpTest.java | 90EF6B834909443E3E7191A0C7F01F4CD827824045E2D67EFB3E48E560251B33 |
| Added | src/test/java/com/changeflow/integration/AuthenticationSecurityHttpTest.java | 71CF4DABE3C38CBC10BB9196F49D736FCCB38F6946AE7BA2930F53090B41A77E |
| Added | src/test/java/com/changeflow/integration/OptimisticPersistenceTest.java | 503E14ABCFBDA42C18E62DCF9F6782E9BE9080C722212A220EC3C7E48FD896B5 |
| Added | src/test/java/com/changeflow/integration/LifecycleRollbackHttpTest.java | 770A82E006BFE8FA4CEE4E141A5FCDD5404DFEA475A45FCF83B5628CC6998DED |
| Added | src/test/java/com/changeflow/integration/ForbiddenOperationHttpTest.java | 0751C8674E14F1C77749CECEDA1B6E9B0851F7C50C44C0423A6FE027C7380DB9 |
| Added | src/test/java/com/changeflow/integration/VisibilitySecurityHttpTest.java | 27F9814E5CEAC2DAC7B92F2D082ED2340034BC3CED7F7B359FDCA0C9B71A29FC |
| Added | src/test/java/com/changeflow/integration/RestartPersistenceTest.java | 80F6A05E5FDDE7A6D4063ECF013216A2D31E40B86F5456E278AE66C3A0E37F10 |
| Modified | src/test/java/com/changeflow/integration/ProblemResponseTest.java | E637180C31973DBCD08CE5525370B6A40B17EEF4667FAA9D32F6E6E02AB61EE8 |
| Modified | specs/001-change-request-management/tasks.md | 559EAA718851C24F2A2F2D2989F2E07055C939B70D255F7139276902AE9CA38C |
| Modified | specs/001-change-request-management/validation.md | Excluded (self-reference) |

# Phase 7 Validation and Final Documentation Evidence

Date: 2026-11-16 (Asia/Tehran). Scope: T073-T081, sequential work without additional
agents. Step 13 / speckit-converge is a separate future invocation and has not run.
Earlier planning/phase dispositions above are historical; this section records the
current source, executable checks, final documentation and traceability.

## Entry gate and T073 audit

The project owner explicitly authorized this phase against the reviewed artifacts.
AGENTS.md, constitution v1.0.0, skill and active spec/plan/tasks/supporting records were
consulted. Prerequisite script selected the absolute feature directory under
`F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/specs/001-change-request-management`.
requirements.md: 16 total, 16 checked, 0 unchecked, PASS, read only. No extensions.yml
or implementation hooks existed. Existing ignore rules cover generated/private output.
git rev-parse found no repository; no branch/commit/Git diff is claimed.

Entry verification compared every hashed Phase 6 manifest file: no change. Existing
Surefire reports showed 801 tests in 30 suites, 0 failures/errors/skips. T074 below
provides a fresh complete execution against the audited snapshot.

Working directory for this phase: `F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow`.
Maven is Apache 3.8.5 at C:/Program Files/apache-maven-3.8.5/bin/mvn.cmd; runtime is
Oracle GraalVM Java 17.0.11 at C:/graalvm-ee-java17-windows-amd64-21.3.10/graalvm-ee-java17-21.3.10.
Effective POM and dependency tree were generated afresh, offline, in target/phase7.
POM remains AC66E684CFDAF160D76B329FC5CDECD39AC7FC56E186CB5AB513A4A58D7C25E8.

| Component | Actually effective/resolved in this phase |
| --- | --- |
| Boot parent/starters/plugin | 3.5.16 |
| Spring MVC | 6.2.19 |
| Spring Security | 6.5.11 |
| Spring Data JPA | 3.5.13 |
| Hibernate ORM / Validator | 6.6.53.Final / 8.0.3.Final |
| Jackson databind / jsr310 | 2.21.4 / 2.21.4 |
| Flyway / H2 | 11.7.2 / 2.3.232 |
| JUnit Jupiter / Platform | 5.12.2 / 1.12.2 |
| Compiler / Surefire | 3.14.1 / 3.5.6 |
| Resources / JAR | 3.3.1 / 3.4.2 |

The 57 production Java files use the permitted dependency graph: API -> Application/
Domain; Infrastructure -> Application/Domain; Application -> Domain; Configuration ->
the composing packages. No forbidden edge or cycle was found, no Domain framework
annotation/import, and no HTTP/JPA/concrete adapter dependency in Application. Injection
is explicit constructor wiring; no @Autowired/@Value field injection was found. Public
Application transaction boundaries, independent immutable Domain records, separate DTOs/
entities, managed request versions, access lock order and safe error mapping match the
reviewed protocol. Existing real integration tests, rather than static scans alone,
provide atomicity/concurrency evidence. No speculative framework or dependency was added.

The effective model pins release 17 and the actual build plugins above. No unmanaged
production dependency or newly introduced unversioned plugin was found. Unbound inherited
plugin-management entries are not executed test plugins; effective-pom emitted offline
descriptor warnings for absent Enforcer/Failsafe artifacts but succeeded. Surefire owns
all 30 Test classes; no Failsafe-only or excluded integration/stress suite exists.
A dependency command initially contained a PowerShell-incompatible brace expression;
parsing stopped before execution, then the corrected explicit-path command succeeded.

Exact raw-credential/private-key scans of src/specs/POM/guidance found zero matches.
Database credentials remain external, opaque credential hashes only are persisted,
and no fixed account/token seed exists. A source hash inventory was saved under ignored
target/phase7/source-start.csv to detect changes during verification. These are scoped
source/effective-model checks, not an external security audit or current vendor-support
certification. The reviewed pinned-generation support caveat remains a local-demo limit.

Executed T073 commands (log redirection omitted):

```powershell
.specify/scripts/powershell/check-prerequisites.ps1 -Json -RequireTasks -IncludeTasks
git rev-parse --git-dir
mvn -version
New-Item -ItemType Directory -Force target/phase7/tmp | Out-Null
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' help:effective-pom '-Doutput=target/phase7/effective-pom.xml'
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree '-DoutputFile=target/phase7/dependency-tree.txt'
```

Audit output: target/phase7/architecture-audit.json, effective-pom.xml and dependency-tree.txt.
No application/POM change was required by T073.

## T074 — complete Maven test lifecycle

Executed, with no suite/repetition exclusions:

```powershell
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase7/tmp' test
```

BUILD SUCCESS, 2026-11-16 21:28:06 +03:30, 2:39 elapsed. Surefire XML: **801 tests,
30 suites, 0 failures, 0 errors, 0 skipped**. All 30 source `*Test` classes were discovered,
including the real-HTTP concurrency, timeout, rollback, visibility and restart suites.
The source snapshot is unchanged from the Phase 6 checkpoint. Detailed class counts
and acceptance mappings follow under T081; raw local output is target/phase7/t074-full.log
and test-inventory.json. Expected failure-injection SQL/error logs occurred inside passing
rollback/storage tests; they are not omitted or interpreted as unexpected build failures.

## T075 — packaged executable and normal restart

Offline packaging initially failed before JAR creation: maven-jar-plugin 3.4.2 was absent
from the cache. Retried with the existing private Maven settings selecting Central:

```powershell
mvn -s target/phase3/maven-settings.xml -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DskipTests' package
```

BUILD SUCCESS (31.359 s); Boot repackage produced target/changeflow.jar, SHA256
988D72E3AB8FAC632375F7B04679359A2D63DF5096C91831A7B909D66613876D. Duplicate test
execution in package was explicitly skipped after T074's complete 801-test green run;
no source/POM changed. Manifest: JarLauncher / com.changeflow.ChangeFlowApplication,
Boot 3.5.16, Build-Jdk-Spec 17; main class bytecode major 61. V1 SQL is packaged and
test coordination/JUnit/Mockito/Boot-test entries are absent.

Executed actual java -jar target/changeflow.jar --spring.application.admin.enabled=true
twice, in separate JVMs, using private child environment database settings and one fresh
file at jdbc:h2:file:F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase7/demo-588abebb-4e07-47a2-8c52-255d4bf6e167/changeflow. Both bound the default loopback :8080,
validated V1 before Hibernate initialization/HTTP readiness, and returned JSON 401 for
an unauthenticated protected request. First startup applied V1; second reported up to date
without migration/reset. Fresh JDBC connections between stopped processes found exactly
one successful migration and all four tables, initially empty, on both inspections.

Graceful shutdown used only Boot's built-in local admin MBean (JDK attach on same user),
not an administrative HTTP endpoint. The ignored Java helper invokes shutdown via
org.springframework.boot:type=Admin,name=SpringApplication; each JVM exited 0 and logged
Graceful shutdown complete plus Hikari Shutdown completed. The helper was compiled/run
only after packaging. Target-only helper commands:

```powershell
javac --release 17 --add-modules jdk.attach -d target/phase7/helper-classes target/phase7/GracefulStop.java target/phase7/InspectDatabase.java
java --add-modules jdk.attach -cp target/phase7/helper-classes GracefulStop <normal-process-id>
java -cp 'target/phase7/helper-classes;target/maven-repository/com/h2database/h2/2.3.232/h2-2.3.232.jar' InspectDatabase
```

Private database username/password were inherited via environment, never arguments or
recorded output. Local harness target/phase7/packaged-demo.ps1 captures and checks process
exit/readiness/closure; sanitized normal logs and JDBC summaries are under F:\oroumand-course-ai\oroumand-course\assignment1\ChangeFlow\target\phase7\demo-588abebb-4e07-47a2-8c52-255d4bf6e167.

## T076 — actual offline administrative commands and packaged lifecycle

Executed target/phase7/packaged-demo.ps1 only after T075 completed. Every administration
invocation ran its own packaged JVM with HTTP stopped, the same absolute database URL,
and private external credentials. A harness-owned TcpListener occupied :8080 throughout
each CLI invocation; all successful commands exited 0 without a Tomcat bind even when
passed --spring.main.web-application-type=servlet (the CLI enforces NONE).

Actual syntax, once per command invocation (identifiers below are descriptive placeholders):

```powershell
java -jar target/changeflow.jar --changeflow.mode=admin --changeflow.action=provision --changeflow.roles=Requester,Reviewer
java -jar target/changeflow.jar --changeflow.mode=admin --changeflow.action=provision --changeflow.roles=Reviewer
java -jar target/changeflow.jar --changeflow.mode=admin --changeflow.action=issue-credential --changeflow.user-id=<owner-user-id>
java -jar target/changeflow.jar --changeflow.mode=admin --changeflow.action=revoke-credential --changeflow.credential-id=<additional-credential-id>
java -jar target/changeflow.jar --changeflow.mode=admin --changeflow.action=remove-role --changeflow.user-id=<owner-user-id> --changeflow.role=Requester
```

Each of two provisions and the additional issue printed exactly one userId, credentialId,
and credential line after the transaction returned; output was parsed in memory and not
saved. Revoke/remove each printed Operation committed., zero credential lines. Invalid
role and issue-for-nonexistent-user each exited 1, zero credentials. Every successful admin
context closed its pool and released the file. Post-commit ordering/rollback is also checked
by LocalAdministrationTest; executable stdout alone is not proof of transactional ordering.

After issuance, normal HTTP startup accepted the additional credential. Actual packaged
HTTP sequence: create 201 (v0), submit 200 (v1), independent request-changes 200 (v2), PATCH
200 (v3), no-op PATCH 200 (same JSON/ETag/v3), resubmit 200 (v4), self-approve 403 (no effect),
independent approve 200 (v5), Approved PATCH 409. Ownership/identity, omitted content and
Please revise feedback were preserved. Both subsequent normal JVMs closed gracefully,
exit 0. Fresh offline JDBC inspection before/after administrative edits and final restart
found 2 users, 3 credential rows (32-byte digests only), one Approved request/version 5,
two metadata-only decisions (RequestChanges/v2, Approve/v5), and no explanation-history
column. Admin changes added exactly one revoked marker and removed only Requester from
the dual-role owner. Final HTTP restart: revoked token 401; removed Requester create 403;
remaining Reviewer owner and independent Reviewer read the exact prior approved JSON/ETag.
No administrative HTTP or publication route was added.

Default embedded file H2 requires normal HTTP to be stopped before another JVM opens
the file. This demo does not claim online/multiprocess H2 administration, AUTO_SERVER,
PostgreSQL portability, or production deployment support. Raw credentials and database
passwords were never persisted in evidence, process arguments or captured admin logs.
Sanitized inspection outputs live only under ignored F:\oroumand-course-ai\oroumand-course\assignment1\ChangeFlow\target\phase7\demo-588abebb-4e07-47a2-8c52-255d4bf6e167.

Supplemental console check: plain `java -jar target/changeflow.jar` also started and
migrated successfully on loopback :8080. Sending Ctrl+C through this automation PTY,
and a Windows console event, did not terminate its JVM; this is not reported as a
verified interactive stop method. The isolated verification JVM was then closed via
an ignored, locally attached Java agent requesting System.exit(0), which ran the Boot
shutdown hook: graceful HTTP, EntityManagerFactory and Hikari closure, process exit 0.
No product source was changed. The reproducible shutdown procedure documented below
uses the already verified local Boot MBean with its explicit startup flag.
The first packaged-demo helper attempt also stopped its JVM successfully but failed
a PowerShell return-value assertion before its checkpoint; corrected the helper and
reran the complete T075/T076 sequence successfully. Initial Flyway history count 2
included the synthetic TABLE row; SQL-only history inspection confirmed one successful
V1 SQL migration. Neither helper correction changed an application outcome.

## T077 — verified quickstart

Replaced the entire provisional guide with verified environment/build/package/start/
local-MBean stop/restart/CLI syntax, actual outcomes, seven-route contract links, private
configuration prerequisites and offline H2 constraints. Included the small target-only
JDK management helper so shutdown is reproducible without depending on an existing ignored
file. The command option values and class calls match the executed helper; Ctrl+C is not
claimed as verified. Placeholder identifiers are explicitly distinguished from secrets.
No application body or draft guide example replaces the reviewed HTTP contract.

## T078 — README

Created README.md with backend purpose, governance/artifact links, package responsibilities,
verified quickstart and evidence links, external configuration/offline admin prerequisites,
atomicity/visibility/version behavior and local-demo/support/portability limits. It does
not add or promise a new state, route, history store, deployment or production SLO.

## T079 — root operational guidance

Replaced the absent-POM/source note in AGENTS.md with the confirmed working directory,
JDK/Maven prerequisites, actual lifecycle goals and links to exact executed cache/mirror
commands. Added package-before-JAR and offline/external-config operating constraints;
retained concise governance, scope, dependency, secret and honest-evidence rules.

## T080 — design/evidence reconciliation

Updated spec/plan/research/data-model/HTTP-contract status and links to the delivered
implementation and actual evidence, while retaining stable FR/SC/scenario IDs and the
user's actual clarification answers. Historical planning gates and published expected
version evidence are labeled as historical; actual versions are separate. T001's narrow
401 synchronization is now recorded as completed rather than a pending contradiction.
Plan layout reflects actual pure Domain/codec and integration suites (no fictional
standalone application test package). Documented the narrow observed Hibernate timeout
classification and nanosecond storage detail without changing behavior, routes or policy.
The task path now matches the actual F:/ workspace; generation/earlier checkpoint records
are explicitly chronological rather than current claims that later work is unstarted.

A before/after extraction compared all 91 normative FR/SC/scenario blocks: byte-for-byte
unchanged. Constitution SHA256 remains
8A0B83962522310C39DC8CE79C3626882B07197B42CA4EE775AC1B3181120B8F. No amendment was made.
CF-OPEN-001/002's adoption-time questions are resolved by reviewed spec/plan references;
the historical constitution issue text was preserved. No approved requirement was weakened.

## T081 — final evidence matrix and disposition

This is the current implementation evidence, superseding earlier phase deferrals. The
reviewer for this implementation self-review is Codex, acting on the user's explicit
Phase 7 instruction, sequentially without additional agents. Reviewed artifacts: constitution
v1.0.0, AGENTS, spec, plan, tasks, research, data-model, contract, quickstart, POM and
all source/test packages. This is not an independent human audit or project-owner sign-off.

### Executed evidence inventory

**E-test:** T074 command above, target/phase7/t074-full.log plus all 30 Surefire XML
reports. Every case below was executed successfully in that full run; zero exclusions,
failures, errors or skips. Counts are test invocations, including parameterized cases,
not a count of unique Java methods or individual assertions. Test helper/control classes
are not additional suites. The exact executed method inventory is retained locally in
target/phase7/executed-methods.txt; all method references below were checked against it.

| Executed Test class | Cases | Failures / errors / skips |
| --- | ---: | --- |
| [domain.ContentRulesTest](../../src/test/java/com/changeflow/domain/ContentRulesTest.java) | 10 | 0 / 0 / 0 |
| [domain.OwnerLifecycleTest](../../src/test/java/com/changeflow/domain/OwnerLifecycleTest.java) | 6 | 0 / 0 / 0 |
| [domain.ReviewRulesTest](../../src/test/java/com/changeflow/domain/ReviewRulesTest.java) | 6 | 0 / 0 / 0 |
| [domain.RevisionRulesTest](../../src/test/java/com/changeflow/domain/RevisionRulesTest.java) | 6 | 0 / 0 / 0 |
| [infrastructure.security.OpaqueCredentialCodecTest](../../src/test/java/com/changeflow/infrastructure/security/OpaqueCredentialCodecTest.java) | 3 | 0 / 0 / 0 |
| [integration.AuthenticationSecurityHttpTest](../../src/test/java/com/changeflow/integration/AuthenticationSecurityHttpTest.java) | 8 | 0 / 0 / 0 |
| [integration.ContentValidationHttpTest](../../src/test/java/com/changeflow/integration/ContentValidationHttpTest.java) | 20 | 0 / 0 / 0 |
| [integration.FeedbackRetentionHttpTest](../../src/test/java/com/changeflow/integration/FeedbackRetentionHttpTest.java) | 2 | 0 / 0 / 0 |
| [integration.ForbiddenOperationHttpTest](../../src/test/java/com/changeflow/integration/ForbiddenOperationHttpTest.java) | 81 | 0 / 0 / 0 |
| [integration.IndependentReviewHttpTest](../../src/test/java/com/changeflow/integration/IndependentReviewHttpTest.java) | 12 | 0 / 0 / 0 |
| [integration.LifecycleRollbackHttpTest](../../src/test/java/com/changeflow/integration/LifecycleRollbackHttpTest.java) | 15 | 0 / 0 / 0 |
| [integration.LocalAdministrationTest](../../src/test/java/com/changeflow/integration/LocalAdministrationTest.java) | 10 | 0 / 0 / 0 |
| [integration.MigrationStartupTest](../../src/test/java/com/changeflow/integration/MigrationStartupTest.java) | 4 | 0 / 0 / 0 |
| [integration.NoOpPatchHttpTest](../../src/test/java/com/changeflow/integration/NoOpPatchHttpTest.java) | 3 | 0 / 0 / 0 |
| [integration.OptimisticPersistenceTest](../../src/test/java/com/changeflow/integration/OptimisticPersistenceTest.java) | 3 | 0 / 0 / 0 |
| [integration.OwnerConcurrencyHttpTest](../../src/test/java/com/changeflow/integration/OwnerConcurrencyHttpTest.java) | 200 | 0 / 0 / 0 |
| [integration.OwnerHttpContractTest](../../src/test/java/com/changeflow/integration/OwnerHttpContractTest.java) | 7 | 0 / 0 / 0 |
| [integration.OwnerLifecycleHttpTest](../../src/test/java/com/changeflow/integration/OwnerLifecycleHttpTest.java) | 6 | 0 / 0 / 0 |
| [integration.ProblemResponseTest](../../src/test/java/com/changeflow/integration/ProblemResponseTest.java) | 10 | 0 / 0 / 0 |
| [integration.RestartPersistenceTest](../../src/test/java/com/changeflow/integration/RestartPersistenceTest.java) | 1 | 0 / 0 / 0 |
| [integration.ReviewAtomicityHttpTest](../../src/test/java/com/changeflow/integration/ReviewAtomicityHttpTest.java) | 14 | 0 / 0 / 0 |
| [integration.ReviewConcurrencyHttpTest](../../src/test/java/com/changeflow/integration/ReviewConcurrencyHttpTest.java) | 301 | 0 / 0 / 0 |
| [integration.ReviewFeedbackHttpTest](../../src/test/java/com/changeflow/integration/ReviewFeedbackHttpTest.java) | 10 | 0 / 0 / 0 |
| [integration.ReviewHttpContractTest](../../src/test/java/com/changeflow/integration/ReviewHttpContractTest.java) | 5 | 0 / 0 / 0 |
| [integration.RevisionHttpContractTest](../../src/test/java/com/changeflow/integration/RevisionHttpContractTest.java) | 17 | 0 / 0 / 0 |
| [integration.RevisionResubmissionHttpTest](../../src/test/java/com/changeflow/integration/RevisionResubmissionHttpTest.java) | 8 | 0 / 0 / 0 |
| [integration.RevocationConcurrencyHttpTest](../../src/test/java/com/changeflow/integration/RevocationConcurrencyHttpTest.java) | 20 | 0 / 0 / 0 |
| [integration.StorageFailureHttpTest](../../src/test/java/com/changeflow/integration/StorageFailureHttpTest.java) | 7 | 0 / 0 / 0 |
| [integration.VisibilityHttpTest](../../src/test/java/com/changeflow/integration/VisibilityHttpTest.java) | 3 | 0 / 0 / 0 |
| [integration.VisibilitySecurityHttpTest](../../src/test/java/com/changeflow/integration/VisibilitySecurityHttpTest.java) | 3 | 0 / 0 / 0 |
| **Total: 30 suites** | **801** | **0 / 0 / 0** |

**E-race:** E-test included exactly 100 each of edit/edit, edit/submit, approve/approve,
request-changes/request-changes and mixed review, plus one review coordination smoke case.
AfterAll successful-pair counters were 100 for every pair, not just generated arguments.
Both owner lock-winning orders were exercised: submission won 50/100 edit/submit pairs.
Mixed review winners: approve 36, request-changes 64. Distinct Reviewer identities/access
rows and persistence contexts really overlapped before guarded updates; only the winner's
full parent/version/feedback/metadata remained. Owner workers used distinct credentials for
one owner and waited on that user's current-access lock. Each race checked complete fresh
snapshots after both workers, not just HTTP statuses. Revocation suite added 20 true
admin/write races: five operations × credential/role removal × both lock-winning orders.

**E-wait:** Actual current-run measured HTTP waits: credential lock 2,156 ms; UserAccess
lock 2,100 ms; pool acquisition 3,050 ms; transaction deadline 10,584 ms, HTTP 503
STORAGE_BUSY after a deliberate 10.5 s test-only delay. The deadline figure includes that
controlled delay; it is not a provider interrupt at precisely 10,000 ms. The real closed
pool yielded STORAGE_UNAVAILABLE, followed by fresh recovery inspection with no write.
Injected unavailable errors in MVC and bearer-filter paths are labeled injections, not
claimed physical disk/network outages. Error mappings were bounded and safe in exercised
conditions; no general latency or distributed-failure guarantee is inferred.

**E-restart:** RestartPersistenceTest closed both first application and pool, then reopened
the same file with distinct context/factory/pool and compared every table column privately
(including digest bytes, roles/revoked status, metadata), plus HTTP representations. It
covered four states, latest feedback, identity/ownership, timestamps and revisions.
**E-jar/E-admin:** T075/T076 separately verified the actual packaged JAR with distinct
JVMs, fresh migrations, graceful restart, full lifecycle and actual offline CLI; see those
sections for exit/status/storage results and secret-safe commands. **E-audit:** T073
records actual Java/dependency/package/secret checks, not a static-analysis certification.

### Named executed proofs

Keys below identify actual executed methods and assertions, so scenario rows reference
real evidence rather than only a proposed filename. All are part of E-test; E-jar/E-admin
are additional independent executable demonstrations, not extra Surefire test counts.

| Key | Actual executed test method(s) | Principal checked result |
| --- | --- | --- |
| O1 | `OwnerLifecycleHttpTest.createEditSubmitAndRepeatedCreatePreserveIdentityAndProposal` | Owned normalized Draft/edit/submit; preserved proposal/identity; distinct repeated create; repeated submit rejects |
| O2 | `OwnerLifecycleHttpTest.nonOwnersWrongStatesAndCurrentRemovedPermissionsCannotMutate` | Non-owner/current-role/state denials preserve fresh stored snapshot |
| O3 | `OwnerLifecycleHttpTest.unauthorizedCreationAndForgedHeadersCannotGrantAuthority` | Authentication/current Requester required; spoofed authority cannot create |
| O4 | `OwnerLifecycleHttpTest.applicationRechecksPermissionsAndCredentialsAfterAuthentication` | Direct public service rejects removed permission/revoked credential after earlier authentication |
| H1 | `OwnerHttpContractTest.fiveRoutesUseDeterministicDtosAndParentEtags` | Create/list/read/PATCH/submit DTOs, Location, parent item tags and deterministic JSON |
| H2 | `OwnerHttpContractTest.ifMatchGrammarAndStatePrecedenceOnBothMutations`; `OwnerHttpContractTest.permissionsAndVisibilityPrecedeFailedStrongComparison` | Missing/malformed/weak/cross-item/stale preconditions; current access before version and state |
| C1 | `ContentValidationHttpTest.invalidCreationAndMixedPartialEditAreAtomic` | All 15 invalid field/kind invocations preserve all stored data |
| C2 | `ContentValidationHttpTest.normalizedBoundariesAndSupplementaryUnicodeRoundTripWithoutTruncation` | Exact/max+1 boundaries, normalization and supplementary code points |
| C3 | `ContentValidationHttpTest.partialEditPreservesOmissionAndInternalWhitespace` | Omitted values retained, supplied values stripped at edges, internal whitespace retained |
| C4 | `ContentValidationHttpTest.matchingControlledFieldsAndUnknownFieldsRejectWholeCreateAndEdit` | Matching controlled and unknown fields reject whole create/edit |
| C5 | `ContentValidationHttpTest.submitRejectsCorruptedStoredContentWithoutRepairOrAnyWrite` | Each invalid stored proposal column prevents submission without rewriting |
| R1 | `IndependentReviewHttpTest.independentReviewerAndDualRoleNonOwnerPersistOneDecision` | Both actions and Reviewer/dual-role non-owner success; one matching metadata row |
| R2 | `IndependentReviewHttpTest.everyVisibleOwnerIsForbiddenFromSelfReviewRegardlessOfRoles` | Both actions reject every visible owner role combination, no stored change |
| R3 | `IndependentReviewHttpTest.missingPermissionSpoofedHeadersAndMissingResourcesHaveNoEffect` | Current Reviewer required; spoof/missing/invisible cannot mutate |
| R4 | `IndependentReviewHttpTest.wrongStatesAndRepeatedCommandsNeverReplaySuccess` | Draft/returned/Approved and old/current repeat failures, no extra metadata |
| R5 | `IndependentReviewHttpTest.approvedRequestCannotBeEditedSubmittedReviewedOrPublished` | Approved immutable across supported and unavailable actions |
| R6 | `IndependentReviewHttpTest.applicationRechecksAnAlreadyAuthenticatedReviewerAfterAccessChanges` | Current role/credential rechecked for both review actions |
| RH | `ReviewHttpContractTest.bothDecisionsReturnTheCommittedParentRepresentationAndMetadata`; `ReviewHttpContractTest.parentIfMatchGrammarIsRequiredForBothActions`; `ReviewHttpContractTest.accessPrecedesVersionAndVersionPrecedesState` | Committed DTO/parent tags; exact precondition grammar/precedence |
| F1 | `ReviewFeedbackHttpTest.invalidExplanationNeverChangesParentOrDecisions` | Seven invalid explanation invocations reject atomically |
| F2 | `ReviewFeedbackHttpTest.exactNormalizedUnicodeBoundariesAndInternalWhitespaceArePreserved` | 2,000 accepted / 2,001 rejected after strip, including Unicode |
| F3 | `ReviewFeedbackHttpTest.forbiddenMatchingAndUnknownFieldsCannotStorePartialFeedback` | Controlled/unknown/malformed feedback input has no partial effect |
| F4 | `ReviewFeedbackHttpTest.latestFeedbackFollowsParentVisibilityAndMetadataHasNoExplanationColumn` | Authorized feedback readers only; metadata schema has no explanation history |
| A1 | `ReviewAtomicityHttpTest.knownFailuresRollBackEveryParentFieldVersionAndDecision` | 12 real-flush runtime/checked/constraint/pre-commit failure cases roll back parent and metadata |
| A2 | `ReviewAtomicityHttpTest.lostConfirmationAfterCommitDoesNotImplyRollback` | Two deliberately discarded confirmations return non-success while committed parent/metadata remain; recovery distinguishes this from rollback |
| L1 | `LifecycleRollbackHttpTest.everyKnownFailureAfterRealFlushRollsBackTheCompleteOperation` | 15 create/Draft-edit/returned-edit/Draft-submit/returned-submit × runtime/checked/pre-commit failures preserve full snapshot |
| V1 | `VisibilityHttpTest.allRoleAndOwnershipCombinationsAcrossEveryState` | Requester/Reviewer/dual/neither ownership × four states; missing/invisible equality |
| V2 | `VisibilityHttpTest.visibilityPredicateRunsBeforeArbitraryOffsetPagination`; `LocalAdministrationTest.storageVisibilityPredicateRunsBeforeArbitraryOffsetPagination` | Visibility predicate applied before offsets/limits, stable permitted ordering |
| V3 | `VisibilitySecurityHttpTest.currentRoleRemovalChangesVisibilityBeforePaginationAndNeverChangesBusinessRows`; `VisibilitySecurityHttpTest.remainingRolesKeepOnlyTheirNormalWritesAndOwnershipStillForbidsReview` | Both role-removal variants, multi-page visibility and precise remaining-role permissions |
| S1 | `AuthenticationSecurityHttpTest.uniformBearerFailuresPrecedeEveryProtectedLookupAndWrite` | Missing/malformed/invalid/revoked/unsupported credentials uniformly 401 before lookup, challenge and no mutation |
| S2 | `AuthenticationSecurityHttpTest.cookieQueryAndForgedRoleHeadersNeverSupplyAuthenticationOrPermission`; `AuthenticationSecurityHttpTest.explicitBearerWritesNeedNoCsrfTokenAndNeverEstablishASession` | Header-only authority, no cookie/query spoofing, no server session |
| S3 | `AuthenticationSecurityHttpTest.noPublicProvisioningIssuerHistoryOrAdministrationSurfaceExists` | No public auth/admin/history/publication surfaces |
| K1 | `ForbiddenOperationHttpTest.forbiddenWritesAndOldVersionsPreserveAllStatesForEveryRoleCombination` | 80 all-state/owner-role/actor cases reject forbidden mutations and delete/cancel/withdraw; every field/existence unchanged |
| K2 | `ForbiddenOperationHttpTest.matchingControlledFieldsRejectWholeSubmitAndReviewCommands` | Matching owner/roles/state in submit/approve/request-changes rejects with no partial effect |
| N1 | `NoOpPatchHttpTest.emptyAndNormalizedIdenticalPatchesKeepNanosecondsRevisionAndRepresentation` | No timestamp/version/representation fabrication |
| N2 | `NoOpPatchHttpTest.noOpStillRequiresCurrentAccessVersionAndEditableState`; `NoOpPatchHttpTest.staleNoOpCannotConcealAnInterveningChangedWrite` | No-op current authorization/state/version and intervening-write safety |
| P1 | `OptimisticPersistenceTest.realManagedUpdatesInDistinctContextsCannotBothCommitTheObservedVersion`; `OptimisticPersistenceTest.noOpCommitChecksItsVersionWithoutIncrementAndRejectsARealInterveningWrite`; `OptimisticPersistenceTest.writeResponseRetainsItsOwnImmutableSnapshotAfterALaterWriter` | Actual managed optimistic conflicts, commit-time no-op check and own immutable response snapshot |
| Q1 | `OwnerConcurrencyHttpTest.ownerPairSerializesOnCurrentAccessWithoutLostChanges` | 200 real overlapping pairs, all fields inspected after workers, both edit/submit winners |
| Q2 | `ReviewConcurrencyHttpTest.overlappingReviewPair`; `ReviewConcurrencyHttpTest.coordinationUsesDistinctRealHttpTransactionsAndContexts` | 300 required overlapping review pairs plus smoke, independent access rows/contexts, winner-only complete effect |
| Q3 | `RevocationConcurrencyHttpTest.realAdministrationAndHttpWriteHonorBothLockWinningOrders` | 20 credential/role × five-operation × two-order races; completed writes retained, waiting unauthorized writes rejected |
| J1 | `RevisionHttpContractTest.sameRoutesDtoEtagsAndNoOpRulesApplyToReturnedRequests` | Same partial PATCH/submit routes, returned no-op/DTO/tags, retained identity/feedback |
| J2 | `RevisionHttpContractTest.invalidReturnedPartialEditsAreAllOrNothing` | 12 mixed valid/invalid returned-content invocations, zero partial updates |
| J3 | `RevisionHttpContractTest.normalizedExactAndExcessiveReturnedContentCoversEveryColumnAndUnicode` | Each column exact/max+1 boundaries and supplementary text |
| J4 | `RevisionHttpContractTest.invalidStoredReturnedContentRejectsSubmissionWithoutRepair` | Invalid stored returned content prevents submit, no repair |
| J5 | `RevisionHttpContractTest.controlledMatchingUnknownAndMalformedBodiesCannotChangeReturnedData`; `RevisionHttpContractTest.returnedMutationsRequireTheCurrentItemTagAndRejectInvalidFormsWithoutEffects` | Controlled/unknown/malformed/current-tag enforcement on returned request |
| G1 | `RevisionResubmissionHttpTest.ownerRequesterAndDualRoleCanReviseResubmitAndCannotReplay` | Both owner role variants preserve content/identity/feedback; old/current repeats reject |
| G2 | `RevisionResubmissionHttpTest.nonOwnersCannotReviseOrResubmitEvenWithBothRoles`; `RevisionResubmissionHttpTest.currentRemovedPermissionsAndRevocationOverrideAnEarlierAuthentication` | Non-owner/current role/revoked proof denials, no change |
| G3 | `RevisionResubmissionHttpTest.remainingRequesterRoleStillPermitsTheReturnedOwnerJourney`; `RevisionResubmissionHttpTest.pendingAndApprovedRejectBothOwnerOperationsIncludingNoOp` | Remaining Requester works; uneditable states forbid both operations/no-op |
| B1 | `FeedbackRetentionHttpTest.completeLifecyclePreservesFeedbackThroughRevisionResubmissionAndApproval` | Full real route loop preserves one latest explanation through independent approval |
| B2 | `FeedbackRetentionHttpTest.onlyANewSuccessfulRequestChangesAfterResubmissionReplacesFeedback` | Multiple actual cycles; only later successful return replaces latest feedback |
| D1 | `RestartPersistenceTest.closingBothAppAndPoolThenReopeningTheSameFilePreservesEveryTableAndRepresentation` | All four states and full table/HTTP equality after actual close/reopen |
| AD | `LocalAdministrationTest.actualAdminEntryPointHasNoHttpPortDisplaysOnceAndClosesFile`; `LocalAdministrationTest.failureAfterProvisioningRollsBackBothRows`; `LocalAdministrationTest.accessLockRefreshesRowsAlreadyReadInTheTransaction` | Non-web one-shot committed output, rollback, current locked row refresh; supplemented by E-admin actual JAR |
| M1 | `MigrationStartupTest.freshFileMigratesBeforeValidatingAllFourMappings`; `MigrationStartupTest.repeatStartupDoesNotResetStoredRows`; `MigrationStartupTest.brokenMigrationFailsStartup`; `MigrationStartupTest.schemaMismatchFailsValidationRatherThanRepairingIt` | Fresh/repeat startup, bad migration/schema fail closed; supplemented by E-jar |
| W1 | `StorageFailureHttpTest.actualCredentialAndAccessLockTimeoutsAreBoundedMvcStorageBusy`; `StorageFailureHttpTest.actualPoolExhaustionIsBoundedStorageBusyInTheBearerFilter`; `StorageFailureHttpTest.actualTransactionDeadlineIsEnforcedAfterATestControlledDelay`; `StorageFailureHttpTest.realClosedPoolIsUnavailableAndRecoveryInspectionFindsNoWrite` | Real provider lock/pool/deadline/unavailable outcomes and recovered stored data |
| W2 | `StorageFailureHttpTest.injectedStorageUnavailabilityHasTheSameSafeShapeInMvcAndFilter`; `ProblemResponseTest.filterLookupStorageFailuresUseMatching503AndLeaveStorageUnchanged`; `ProblemResponseTest.observedProviderDeadlineIsBusyWhileOtherProviderErrorsStayInternal` | Explicitly injected mapping/shape tests, safe narrow timeout classification |

### Functional requirement matrix

All rows passed within their defined, exercised acceptance cases; proof keys resolve to
executed methods above. No coverage percentage or universal absence-of-bugs claim is inferred.

| Requirement | Proof / observed result |
| --- | --- |
| FR-001 | V1/V3/R1/R2/G1: independent and combined roles; union visibility with ownership/state limits |
| FR-002 | O1/H1/M1/D1: individually identified complete four-state records, exact stored/serialized values |
| FR-003 | O1/O3/C4/S2: authenticated current Requester owns new Draft; supplied authority rejects |
| FR-004 | O1/O2/J1/J2/G1/G2: own Draft/returned partial edit only, identity/owner/state preserved |
| FR-005 | O1/C5/J4/G1/G3: valid stored Draft/returned content submitted, no rewrite/feedback loss |
| FR-006 | R1/R3/R4/RH/B1: independent current Reviewer approval only from PendingReview |
| FR-007 | R1/R2/F1/F2/B2: independent return with valid required latest explanation |
| FR-008 | R2/V3: both self-review actions denied for owner role combinations, no mutation |
| FR-009 | R5/G3/K1: Approved immutable, no edit/state/delete effect |
| FR-010 | O3/O4/R3/R6/G2/S1/S2/Q3: authenticated current permissions and ownership at sensitive write boundary |
| FR-011 | O2/R4/G3/K1: current state checked, illegal transitions/repeats rejected |
| FR-012 | L1/A1/P1/RH: full parent/feedback/version/metadata commit or rollback; no pre-commit success |
| FR-013 | H2/N2/P1/Q1/Q2: real optimistic/current-access races preserve winner, no lost updates |
| FR-014 | C1/C4/F1/A1/L1/K1/Q1/Q2: rejected/known failed operations have no effect and preserve concurrent winner; A2 separately covers uncertain acknowledgement |
| FR-015 | H2/R3/V1/S1: missing and authenticated invisible identical 404; invalid credentials uniform 401 before lookup |
| FR-016 | V1/V2/V3/F4/S1/D1: current roles, four-state union visibility before pagination, reads do not mutate |
| FR-017 | K1: delete absent for every state/actor combination, existence retained |
| FR-018 | K1: cancel/withdraw absent for every state/actor combination, no extra state |
| FR-019 | O4/R6/G2/V3/Q3/AD/E-admin: current permissions/revocation, both winning orders, remaining roles and earlier completed writes |
| FR-020 | Q2/E-race: 100 each review pair; exactly one completed winner and one conflict, winner metadata/feedback retained |
| FR-021 | O1/R4/G1/Q3/A2: distinct valid creates, old/current repeats never replay success; current permission still required |
| FR-022 | C1/C2/C3/C5/J1/J2/J3/J4: complete nonblank normalized code-point-limited content, omission/null distinction, atomic rejection |
| FR-023 | C4/F3/J5/K2: matching controlled fields reject whole write on all supported mutation routes |
| FR-024 | F1/F2/F4/A1/Q2/B1/B2/D1/E-admin: latest-only explanation validation, privacy, atomicity, winner retention and later replacement; metadata only |

### Success criterion matrix

| Criterion | Executed evidence / observed result |
| --- | --- |
| SC-001 | O1/R1/G1/B1 and E-jar: all defined successful owner/review/revision journeys reached expected state with unchanged ownership |
| SC-002 | R2: both self-review actions denied across owner-role combinations; fresh snapshots unchanged |
| SC-003 | O2/R3/R4/R5/G2/G3/K1: all defined forbidden/immutable/missing/delete/cancel/withdraw cases rejected, stored existence/fields unchanged |
| SC-004 | Q1/Q2/E-race: 100 completed repetitions for each of five pairs, 500 required total; zero incompatible successes, lost updates or partial effects |
| SC-005 | L1/A1/W1/W2: controlled rollback cases had no false success/no failed-operation effects; recovered pool inspection unchanged. A2 is acknowledgement loss after a completed operation, not a known failed transaction |
| SC-006 | H1/H2/RH/O1/R4/G1/A2: completed/rejected route outcomes distinct; defined retry policy verified. A2 deliberately demonstrates that lost confirmation does not prove rollback; current-state recovery is required, per the reviewed contract |
| SC-007 | V1/V2/V3/S1/F4: only permitted data listed/read in all defined role/state cases; identical authenticated invisible/missing 404 and uniform challenged 401 for invalid credentials |
| SC-008 | Q3/V3/G2/G3/E-admin: removed roles cease granting access, remaining roles keep normal access; lock order prevents unauthorized completion and preserves earlier commits |
| SC-009 | C1/C2/C3/C5/J1/J2/J3/J4: all defined proposal omission/null/nonblank/normalized boundary cases, no invalid stored effects or truncation |
| SC-010 | C4/F3/J5/K2: all defined controlled-field writes reject in full, including matching owner/roles/state |
| SC-011 | F1/F2/F4/A1/Q2/B1/B2/D1: required valid latest feedback commits atomically, invalid/conflicting attempts preserve it, readers obey visibility, revision/resubmit/approve retain it until later successful return |

SC-005/006 and FR-012/014 do not promise exactly-once delivery or omniscience after a
lost response. The approved HTTP contract explicitly distinguishes known rollback from
an unacknowledged commit; A2 verifies that distinction rather than silently assuming a
non-success response proves unchanged data. No acceptance requirement was edited to hide
this limit. Local controlled-case coverage is established; final owner acceptance is separate.

### Individual acceptance scenario matrix

Every row below resolves to actual named methods above and the successful T074 run.
All 56 scenarios are included, including previously deferred returned-content/review races.

| Scenario | Executed proof and result |
| --- | --- |
| US1-AS01 | O1/H1: created complete owned Draft, normalized fields and server identity |
| US1-AS02 | O1/C3: owned Draft content edit preserved identity/owner/state |
| US1-AS03 | O1: submission reached PendingReview with same proposal/owner |
| US1-AS04 | O3/S1: unauthenticated/non-Requester create denied; no record |
| US1-AS05 | O2: non-owner and noneditable states rejected without stored effect |
| US1-AS06 | C4/O3: supplied roles/owner/state rejected, forged headers grant no authority |
| US1-AS07 | O1: repeated valid create generated a distinct Draft |
| US1-AS08 | O1/G1: both Draft and returned submission repeats reject with old/current tags and unchanged committed winner |
| US1-AS09 | O1/C2/C3: strip supplied edges and preserve internal whitespace |
| US1-AS10 | C3/J1: partial edits preserve omitted Draft and returned fields |
| US1-AS11 | C1: every required proposal field omission/null/empty/blank/overflow creation case rejected atomically |
| US1-AS12 | C1/J2: mixed valid/invalid partial Draft and returned edits leave entire record unchanged |
| US1-AS13 | C2/J3: exact/max+1 limits for every column, supplementary Unicode, no truncation |
| US1-AS14 | C5/J4: invalid stored Draft/returned content prevents submit without repair |
| US1-AS15 | C4: even matching controlled creation values reject whole write |
| US1-AS16 | C4/J5: even matching controlled editable-request values reject whole patch |
| US2-AS01 | R1/RH: independent approval commits Approved and one matching metadata row |
| US2-AS02 | R1/F2/RH: independent return commits ChangesRequested and normalized latest feedback atomically |
| US2-AS03 | R2: owner self-approval denied across role combinations |
| US2-AS04 | R3/R6/S1: missing/current-removed permission or revoked credential denies review |
| US2-AS05 | R4: Draft/returned review denied with correct visibility/state outcomes |
| US2-AS06 | R1: dual-role independent non-owner can perform both review actions |
| US2-AS07 | R2: owner self-request-changes denied, including dual role |
| US2-AS08 | Q2: 100 true overlapping approve/approve pairs; one winner/one conflict, one metadata row |
| US2-AS09 | Q2: 100 overlapping changes/changes pairs; only winner explanation/metadata retained |
| US2-AS10 | Q2: 100 mixed pairs, both actions observed winning, no priority or loser feedback/metadata |
| US2-AS11 | R4: repeated decisions old-tag 412/current-tag 409, no replay/extra insert |
| US2-AS12 | F1/F3: missing/null/blank/overflow/malformed explanation rejects without partial effect |
| US2-AS13 | F2: trimmed/internal whitespace and 2,000/2,001 code-point boundaries |
| US2-AS14 | A1/A2: known failed return fully rolls back; acknowledgement loss after real commit separately demonstrated |
| US2-AS15 | F4/V1: feedback visible only with authorized parent, not to hidden/unauthenticated readers |
| US2-AS16 | B2: later successful return after actual resubmission replaces latest feedback, no historical explanation storage |
| US3-AS01 | R5/K1/G3: Approved edits/transitions/delete all reject, immutable snapshot |
| US3-AS02 | O3/O4/R6/S2/K2: current server permissions enforced; supplied authority cannot bypass |
| US3-AS03 | Q1/P1: 100 edit/edit races and real managed provider conflict, no silent lost update |
| US3-AS04 | Q1: 100 edit/submit races, both winning orders, post-submit edit denied |
| US3-AS05 | L1/A1: 15 owner and 12 review controlled real-flush/commit-phase rollback cases preserve full snapshots |
| US3-AS06 | H2/R3/V1: missing mutation never creates/changes a row; invisible is indistinguishable |
| US3-AS07 | S1: unauthenticated protected writes uniformly 401 with no stored change |
| US3-AS08 | V1/V2: Requester own four-state visibility, others excluded before pagination |
| US3-AS09 | V1/V2: Reviewer sees all non-Drafts, never another's Draft |
| US3-AS10 | V1/V3: dual-role union, still no other-owner Draft access |
| US3-AS11 | V1/S1: authenticated missing/invisible identical 404; unauthenticated challenged JSON 401 before lookup |
| US3-AS12 | K1: delete unavailable for all defined states/actor roles; row retained |
| US3-AS13 | K1: cancel/withdraw unavailable across all combinations, no return to Draft/extra state |
| US3-AS14 | V3/O4/G2: removed roles grant no owned/non-owned read/write/create access beyond remaining role |
| US3-AS15 | Q3: credential/role administration and HTTP writes truly overlap in both lock-winning orders |
| US3-AS16 | V3/G3: remaining role keeps exactly normal permissions; ownership grants no missing role/self-review |
| US3-AS17 | Q3: write winning locks completes and retains full effects after subsequent removal |
| US3-AS18 | Q3/G2: retry after removal/revocation denied; prior completed operation retained |
| US3-AS19 | K2: matching identity/roles/state on submit and both reviews reject whole operation |
| US4-AS01 | J1/J2/J3/G1: owned returned revision preserves identity/owner/state/feedback and validates atomically |
| US4-AS02 | G1/B1: returned resubmission keeps proposal/feedback/identity and reaches PendingReview |
| US4-AS03 | G2: non-owner/removed-permission/revoked returned mutation denies with no effect |
| US4-AS04 | G3: PendingReview/Approved owner edit and submit reject, including no-op |
| US4-AS05 | B1/B2: real revision/resubmit/independent-approval feedback retention and later-success-only replacement |

### Final review disposition and remaining limits

T073-T081 are complete. Phase 6 was verified green at entry; this phase changed only the
10 documentation/guidance/evidence files listed below. All source files and POM are
unchanged from the audited/full-tested snapshot. No behavior correction, dependency,
route, state, historical explanation store or constitution amendment was introduced.

The automated final checks resolved **77 distinct named executed method references**
against actual Surefire results; no missing reference. Source Test discovery and report
suite names matched exactly (30/30). The matrix has exactly **91 unique required rows**:
24 FR, 11 SC, and 56 scenarios (16 US1 + 16 US2 + 19 US3 + 5 US4). All 91 extracted
normative requirement/scenario blocks stayed byte-for-byte unchanged during T080.
All **93 local file/heading links** in README, AGENTS and the eight active feature
artifacts resolved; zero broken local targets. The 33 historical external documentation
links were retained, not freshly availability/support-certified by this local link check.

Final scoped scans of source, feature artifacts, POM/guidance, generated evidence/logs
and Surefire reports found **zero exact raw-bearer-format or private-key matches**.
This is scoped secret-handling evidence, not a universal scanner certification. Admin
stdout/passwords were kept in memory; no live credential is published here. All 71
packaged production class files have bytecode major 61 (Java 17); no test support/classes
or test-only libraries entered the JAR. POM, constitution and JAR hashes remain as above.
Final checker: `& ./target/phase7/final-checks.ps1`; output: target/phase7/final-checks.json.

| Task | Current status | Actual completion evidence |
| --- | --- | --- |
| T073 | Complete | Effective POM/tree and architecture/Java/dependency/secret audit |
| T074 | Complete | Full test lifecycle, 801/30, all required repetitions, zero failures/errors/skips |
| T075 | Complete | Package/repackage then fresh normal JVM migration/validation/graceful restart |
| T076 | Complete | Actual offline provision/issue/revoke/remove, occupied-port proof, full packaged HTTP loop and persisted post-restart effects |
| T077 | Complete | Verified quickstart including reproducible local-MBean stop helper and private configuration |
| T078 | Complete | Backend README, source-of-truth/architecture/setup/evidence/limits |
| T079 | Complete | Actual Maven goals/prerequisites/cwd in root AGENTS |
| T080 | Complete | Delivered design/evidence reconciled; stable business blocks and constitution unchanged |
| T081 | Complete | Complete FR/SC/scenario matrix, real method/discovery/link/hash/secret checks and this disposition |

No required Phase 7 verification remains failing, blocked, skipped or reduced. Resolved
execution issues: offline packaging cache miss, and the explicitly recorded verification
helper corrections. Duplicate test execution during packaging was deliberately skipped
after the complete green test run; it is not a skipped required test case. Additional
console Ctrl+C delivery was unverified in the automation PTY; the required graceful stop
was verified through local Boot management and documented. Real outages beyond the
controlled/provider cases, production deployment, vendor-support certification and
PostgreSQL portability were not exercised or claimed. No source change requires a new
regression run after T074. All demonstration JVMs have stopped and released their files.

Changed files in this phase: README.md (new), AGENTS.md, and active feature spec.md,
plan.md, research.md, data-model.md, contracts/http-api.md, quickstart.md, tasks.md,
validation.md. Generated logs, private H2 data, helper scripts/classes and the JAR remain
ignored under target/; no credentials are added to deliverable files. No Git repository
exists, so the following file/source/artifact hashes identify the reviewed snapshot.

Stop after Phase 7. **Converge was not invoked. Step 13 must invoke it separately against
the then-current codebase.** Implementation self-review and executed evidence do not
substitute for independent human review or the project owner's final acceptance.

### Phase 7 final file manifest

validation.md omits its own hash to avoid a self-referential digest. Source hashes are
unchanged from the Phase 6 manifest/source-start.csv; the nine other delivered files:

| File | SHA256 |
| --- | --- |
| README.md | 8D352C68E7ED896F6912169E30A8EBA70E0B5AC81B366DBA7099F19A78A0FA82 |
| AGENTS.md | 30613EB10A44B2C3DF273996B28538EEA5066D700A429C2E5BDA1C33788BE33A |
| specs/001-change-request-management/spec.md | 80950FBBD4BFBB557AD03C3555D9EBCE623A305D2BEFBFEE820C359146262822 |
| specs/001-change-request-management/plan.md | 3C63E7990CDB48E02A6A5DA25444FDDC47905FD85E9A0879944CA1A88FED28C3 |
| specs/001-change-request-management/research.md | 1E4E84BED188AC3857DFFC8AC022CD2A86459735775A67ED4FB89A79B6ED6752 |
| specs/001-change-request-management/data-model.md | 4BA9D3089105FFD69C1C42C37E2E9D9ED246DD1A25AB2349A2B2DC348988AAC0 |
| specs/001-change-request-management/contracts/http-api.md | 877B7B596CB6E86AB13D73D6B7C86DA7A9B1FA4C1738E175549ACF021484CEA7 |
| specs/001-change-request-management/quickstart.md | 6984B58FEC65694D10BD7CBB4157444F8443727533ECC6BD388B338E545B3EA5 |
| specs/001-change-request-management/tasks.md | 94B16B99F5CADA65FBB78C663C34996BC48D933891DC9496719DB1F362AAD75A |

# Phase 8 Convergence Corrections Evidence

## Scope, governance and entry

Requested scope: T082 and T083, executed sequentially without additional agents. Reviewed
constitution v1.0.0, AGENTS, spec/plan/tasks and the supporting data-model/research/HTTP
contract/quickstart. requirements.md gate: 16 total / 16 checked / 0 unchecked, read-only
PASS. No before-implement extensions file exists. `check-prerequisites.ps1 -Json
-RequireTasks -IncludeTasks` resolved the absolute active feature directory. Existing
Java/private-output ignore rules are adequate. git rev-parse again reported no repository;
file hashes identify this snapshot, not a commit or branch.

Working directory: F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow. Existing
Maven 3.8.5 / Java 17.0.11 and target/maven-repository cache are used. No POM, dependency,
Domain rule, schema, transaction policy, state, route or constitution change is required.
The convergence findings and original historical phase evidence remain above.

## T082 — hidden invalid-content visibility

Tests were authored before the behavior change in InvalidStoredVisibilityHttpTest.
45 parameterized hidden cases cover all four states / each of title-description-reason /
other Requester, removed Requester owner, removed Reviewer, plus Draft-hidden Reviewer,
dual-role and owner-with-only-Reviewer variants. Each case compares GET, PATCH, submit,
approve and request-changes to a missing ID using valid bodies and item-specific tags.
It compares the complete problem excluding only the necessarily different instance URI,
rejects feedback leakage/ETags and inspects every parent column plus decision metadata
in fresh REQUIRES_NEW raw snapshots. Six additional Draft/returned owner submission cases
verify INVALID_CONTENT without repair. Invalid fixture SQL changes content only, never
manually assigns a request version.

Executed test command prefix (all subsequent invocations here use these exact options):

```powershell
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase8/tmp' '-Dtest=InvalidStoredVisibilityHttpTest' test
```

Red: 51 tests, 45 failures / 0 errors / 0 skipped, BUILD FAILURE at 23:12:09 +03:30,
39.768 s. Each hidden case returned 400 rather than 404 on its first GET; the six owner
invalid-submission cases already passed. This was a real business assertion failure,
not a missing class/tool or compilation failure. Local log: target/phase8/t082-red.log.

Correction: added a narrow findVisible port operation. Its JPA item query applies the
same parenthesized visibility predicate as lists before Domain construction; refresh then
rehydrates only an eligible row. Application still checks current roles/visibility and
all owner/version/state rules, and existing access locks/transaction boundaries are intact.
Domain independently validates stored values as before, so authorized invalid submissions
remain 400. Three test decorators now delegate the new operation; the concurrency decorator
still emits LOADED with the real transaction-bound context/revision. No production hook,
error suppression or permissive rehydration fallback was added.

Green command: same prefix with the following exact test selection:

```text
-Dtest=InvalidStoredVisibilityHttpTest,OwnerHttpContractTest,OwnerLifecycleHttpTest,ContentValidationHttpTest,NoOpPatchHttpTest,VisibilityHttpTest,VisibilitySecurityHttpTest,AuthenticationSecurityHttpTest,RevisionHttpContractTest,RevisionResubmissionHttpTest,ReviewHttpContractTest,ReviewFeedbackHttpTest
```

141 tests in 12 suites, zero failures/errors/skips, BUILD SUCCESS at 23:14:05 +03:30,
52.658 s. All 51 new cases passed; all five hidden item-route checks actually ran in each
of the 45 cases, with complete raw parent/decision equality after each attempt. Existing
owner/revision/visibility/auth/review-contract cases remained green. Log:
target/phase8/t082-green.log. Refs: FR-015/016, SC-007, US3-AS06/11, US2-AS15 and US1-AS14.

## T083 — one byte representation per strong version tag

RepresentationEncodingHttpTest was authored before production encoding changes. First
attempt did not compile because its HTTP helper omitted Builder.build(); corrected only
that helper and reran. The compilation failure is recorded in target/phase8/t083-red.log
and is not meaningful red behavior evidence.

Meaningful red command: the T082 Maven prefix with
`-Dtest=RepresentationEncodingHttpTest test`. Actual result: 18 cases, 13 failures,
0 errors/skips, BUILD FAILURE at 23:16:31 +03:30, 32.045 s. Ten incompatible-charset
cases returned 200 instead of 406 on GET; three weighted Accept/fallback cases selected
UTF-16 rather than the canonical representation. Five already-supported UTF-8/wildcard
variants passed. Log: target/phase8/t083-red-assertions.log.

Correction: the controller declares application/json;charset=UTF-8. JsonConfiguration's
MVC header negotiation strategy filters incompatible charset alternatives before handler
invocation and serialization; if none remain it uses the existing NOT_ACCEPTABLE 406.
It delegates Accept parsing/quality ordering to Spring's existing strategy, so a lower
quality UTF-8-compatible alternative remains eligible instead of selecting a higher
quality UTF-16 representation later. Explicit UTF-8/case variants, no charset and
wildcards continue working. This is HTTP composition only; no transaction, state, tag
format, route, DTO or provider dependency changed. Problem responses remain JSON and
authentication still runs before MVC negotiation.

Green command: T082 Maven prefix with the following exact selection:

```text
-Dtest=RepresentationEncodingHttpTest,OwnerHttpContractTest,ReviewHttpContractTest,RevisionHttpContractTest,NoOpPatchHttpTest,ProblemResponseTest,AuthenticationSecurityHttpTest
```

68 tests / seven suites, zero failures/errors/skips, BUILD SUCCESS at 23:17:47 +03:30,
42.053 s. Log: target/phase8/t083-green.log. The 18 new cases consist of 10 charset ×
mutation variants (both UTF-16 endian forms × create/PATCH/submit/approve/request-changes)
and eight supported/fallback Accept alternatives. They inspect raw response bytes,
Content-Type, parent ETag/If-Match, deterministic write-vs-GET identity, no-op byte/tag/
stored-state equality and the independent review/return/resubmit/approval loop. Rejected
GET/mutations are 406, no success ETag, same parent/decision snapshot and no extra created
row. All eight completed loops retain feedback/owner, reach Approved at v5 and store two
metadata-only decisions. Raw credential values are never recorded.

## Complete regression on the corrected source

Executed without test selection or repetition reduction:

```powershell
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase8/tmp' test
```

BUILD SUCCESS at 2026-11-16 23:20:30 +03:30, 1:45 elapsed: **870 cases in all 32 suites,
zero failures/errors/skips**. The previous 801 cases remain present; the new classes add
51 + 18. All 32 source Test classes match Surefire suite discovery. The current class
inventory is target/phase8/test-inventory.json and full output target/phase8/full.log.
Successful-pair counters: edit/edit 100; edit/submit 100; approve/approve 100;
request-changes/request-changes 100; mixed review 100. Owner submission won 50/100;
mixed review winners were Approve 41 / RequestChanges 59. Coordination smoke adds one
review case; it does not replace a required repetition. The unchanged 20 revocation
orderings, owner/review rollback suites, provider optimistic/no-op commit checks,
feedback retention, Approved immutability, current-role visibility and file restart all
executed again against the corrected lookup and negotiation code.

Actual measured waits: Credential lock 2,036 ms; UserAccess lock 2,051 ms; pool 3,044 ms;
transaction-deadline response 10,537 ms (503 STORAGE_BUSY, including the deliberate 10.5 s
probe delay). Closed-pool recovery/explicitly injected unavailability checks remain
separate from real lock/pool/deadline evidence. No stronger latency/production outage
claim is made. All concurrency paths still inspected fresh complete parent/feedback/
version/metadata snapshots after workers finished.

The Phase 7 FR/SC/scenario matrix remains historical executed coverage. Current supplemental
mapping: InvalidStoredVisibilityHttpTest.invisibleInvalidContentIsIndistinguishableFromMissingAcrossAllItemRoutes
extends FR-015/016, SC-007 and US3-AS06/11 / US2-AS15; its authorizedInvalidSubmissionStillRejectsWithoutRepair
preserves US1-AS14 / FR-022. RepresentationEncodingHttpTest.incompatibleEncodingRejectsBeforeAnyMutation
and supportedNegotiationAndFallbackKeepIdenticalBytesThroughTheLifecycle cover the strong
representation/If-Match/no-op contract, FR-013 and deterministic output in SC-006. Every
previous mapped method also executed in this full run; no old scenario was removed.

Packaging followed the full green run, with unchanged source between test and package:

```powershell
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DskipTests' package
```

BUILD SUCCESS at 23:21:40 +03:30, 1.849 s, Boot repackage refreshed target/changeflow.jar.
The explicit skip avoids a duplicate 870-case execution during package; all required tests
already ran above, with zero skipped cases. No JAR-dependent check preceded this package.

## Packaged correction smoke and final disposition

Only after the new package succeeded, executed `& ./target/phase8/packaged-smoke.ps1`.
The target-only harness launched actual Java 17 child JVMs with private environment
configuration on a new isolated file, keeping admin stdout/credential/password in memory:

```text
java -jar target/changeflow.jar --changeflow.mode=admin --changeflow.action=provision --changeflow.roles=Requester
java -jar target/changeflow.jar --server.port=<available-loopback-port> --spring.application.admin.enabled=true
java --add-modules jdk.attach -cp target/phase7/helper-classes GracefulStop <normal-jvm-pid>
```

The port/PID placeholders above were actual discovered runtime values. The local shutdown
helper/prerequisites are documented in quickstart; no admin HTTP endpoint is introduced.
Provision exited 0 and printed exactly one credential privately after commit. Normal
startup migrated/validated the configured file and became ready on loopback. Actual HTTP:
valid create 201; UTF-16LE/BE item read and create 406 without success ETag; weighted UTF-8
fallbacks 200 with exactly the original response bytes/tag; no-op PATCH 200 with unchanged
bytes/tag; final visible list contained exactly one row, so rejected creates had no effect.
Local management shutdown confirmed HTTP and Hikari graceful closure, normal exit 0.
Sanitized output: target/phase8/packaged-smoke.log. No older packaged-demo script was rerun
because it records historical Phase 7 evidence; current smoke is explicitly narrower.
The full real restart/administrative/rollback suites did run again above.

Refreshed JAR SHA256: E34EBFACF43B48C533F42D4D1EE82622E7FC91602D7EC62F9DB5C4B10B7B45BB.
Manifest remains Boot 3.5.16 JarLauncher / com.changeflow.ChangeFlowApplication, JDK 17.
All 72 packaged production class files are bytecode major 61 and exactly match current
compiled class hashes; zero test-only libraries/support entries. The extra class is the
small header-negotiation strategy inside the existing configuration, not a new layer.
No production source changed after the successful full run/package.

T082 and T083 are complete. Source changes are limited to access-aware lookup and
canonical JSON negotiation plus required port-decorator forwarding and the two regression
classes. Supporting plan/data-model/research/contract notes now describe those enforceable
choices. README/quickstart/spec status point to current evidence; no FR/SC/scenario or
actual clarification answer was rewritten. All 91 normative FR/SC/scenario blocks compare
byte-for-byte equal to the prior reviewed snapshot. POM SHA256 remains
AC66E684CFDAF160D76B329FC5CDECD39AC7FC56E186CB5AB513A4A58D7C25E8; constitution remains
8A0B83962522310C39DC8CE79C3626882B07197B42CA4EE775AC1B3181120B8F. No dependency/schema/
Domain rule/access-lock order/transaction policy/latest-feedback/state/route change occurred.

No required check is failing, skipped, blocked or reduced. Resolved authoring issue: the
T083 test helper's initial missing build() call, followed by genuine red/green evidence.
A documentation patch initially missed its context and was reapplied to the correct lines;
this did not change application code or test results. Packaging skipped only duplicate
execution after the green full run. Verification targets ignored local artifacts; no raw
credential or database password is recorded. Final scoped source/artifact/log/report secret
and local-link checks are recorded below. No vendor support certification, external audit,
PostgreSQL compatibility or production SLO is claimed.

The original Converge did run and produced F1/F2/T082/T083. **A follow-up Converge has not
been invoked against these corrections**; this implementation pass does not claim its result.

### Phase 8 changed-file manifest

validation.md is also changed and omits its own self-referential hash. The 19 other
delivered changed/new files follow (20 total); target-only evidence/helpers are ignored.

| File | SHA256 |
| --- | --- |
| README.md | 3D73E4CFFE59B329F3823C2CE28FCD03C8812153942FB6AA0729BCAC66327BBA |
| specs/001-change-request-management/contracts/http-api.md | 75D6E908D2978A867765B15515A493E6AB0AFCF1BC62D4A1623D4461B993A36E |
| specs/001-change-request-management/data-model.md | 1C8D48434C626301538C5E8037FABD34324777F4D8FA60764DD3F0B3663F53EA |
| specs/001-change-request-management/plan.md | 8E606E9A258D36E32B05F9C355500218B58DD29166817BF32DC02EB71EE972E7 |
| specs/001-change-request-management/quickstart.md | 4A3B5FD63CD101496CF8EDDD86082F3D1F5EA0F8BB068B70EC7D0120F4CA9F11 |
| specs/001-change-request-management/research.md | 09CDE9707EDF3E73E1FE42C4B729F9BB431D1BBE7D1B2C81B51A6CE77F65702D |
| specs/001-change-request-management/spec.md | 42CFB84BF013C4C349F62C3DC46B4928A0BE178A9FCF61058B8B187B2061FED8 |
| specs/001-change-request-management/tasks.md | 6E6582C6C7456ADA4DCA732495AFBCCB5F12AE76654A7369BA645AB9D9309030 |
| src/test/java/com/changeflow/integration/InvalidStoredVisibilityHttpTest.java | 51E361DCBE828474A7B19C0109614DFABE57A54126E7B666C6CB127E9B2739E6 |
| src/test/java/com/changeflow/integration/RepresentationEncodingHttpTest.java | F6DD0D8B5C63D1D92DF55E84452B7A2A8F46BDA71DDE99D11D32C6F1CA067B84 |
| src/main/java/com/changeflow/api/ChangeRequestController.java | 81C6C618154FF006F795D77B971F2E3481E9246E9B81643925F7D00DA7001DD6 |
| src/main/java/com/changeflow/application/ChangeRequestService.java | C1D25369850080A74F09C232AC926BFD12AE2E44CCEC8E347198F94CAE6EABF2 |
| src/main/java/com/changeflow/application/port/ChangeRequestStore.java | 512529AE302CC12232EBF2275F3C19BDBB178F7F19764612EA4F1FEDF72274B8 |
| src/main/java/com/changeflow/configuration/JsonConfiguration.java | F269593FD1FE0B322600EA2A1731799272F34CDC91F6AC599F4CB5D4B7D8CAB4 |
| src/main/java/com/changeflow/infrastructure/persistence/ChangeRequestJpaRepository.java | 435C5C1F243ACFA00E6A80C9FA26375D035725F67BC1F51C095E917B6CED3409 |
| src/main/java/com/changeflow/infrastructure/persistence/JpaChangeRequestStore.java | E826B8DF61378AF97323234AFB465BFC3E59CC763944D846524DE497E6EFF154 |
| src/test/java/com/changeflow/integration/OwnerLifecycleHttpTest.java | E6A75E25558721616B2E46579E831CD9A262ADC2DBFC376DB1835ABFBAC9323A |
| src/test/java/com/changeflow/integration/ReviewAtomicityHttpTest.java | 1CB5BD460E3E3F2B6B257296417FFAB26E3CB764C0BA5D401488D7C52AC3BED5 |
| src/test/java/com/changeflow/integration/support/ConcurrencyTestConfiguration.java | 077D72C3B64A206B8697555D69C3F328C1FC571C69D6689804025CF6F662B19B |

Final local checks: 96 file/heading links resolved, zero broken; scoped scan of
247 source/artifact/log/report text files found zero exact raw-token/private-key
matches. All 32 source Test class names matched report suites. Import/package checks
found zero forbidden edges/framework leaks and retained the acyclic reviewed direction.
All before/after implementation extension checks found no extensions.yml/hooks. No task
beyond T083 was introduced or executed. Current task markers: 83 complete, zero open.
