# ChangeFlow Local Quickstart

Verified on 2026-11-16 in
`F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow`, using Maven 3.8.5,
Oracle GraalVM Java/Javac 17.0.11, and Boot 3.5.16. The executable was packaged before
any JAR-dependent check. [Validation](validation.md#phase-7-validation-and-final-documentation-evidence)
records commands, failures/recovery, 801 passing tests and separate-JVM demonstrations.
The [subsequent correction evidence](validation.md#phase-8-convergence-corrections-evidence)
records T082/T083's 870-test / 32-suite full run, including the original required races.
The [spec](spec.md), [plan](plan.md), [data model](data-model.md) and
[HTTP contract](contracts/http-api.md) remain authoritative.

## Prerequisites

Use a full JDK 17 and Maven 3.6.3+ on PATH, a writable private database directory,
and loopback port 8080 available for normal mode. No Docker, external identity service,
H2 console or PostgreSQL server is required. Run every command from the same project root:

```powershell
Set-Location F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow
mvn -version
```

Database settings must be supplied externally to every normal/admin invocation:
`CHANGEFLOW_DATABASE_URL`, `CHANGEFLOW_DATABASE_USERNAME`, `CHANGEFLOW_DATABASE_PASSWORD`.
The URL defaults to `jdbc:h2:file:./data/changeflow`; username/password have no defaults.
Prefer an absolute file URL so another working directory cannot open a different database.
Use private environment configuration; never paste real values into source, evidence,
process arguments, shell history or tracked files. Keep credentials outside transcripts.
The verification harness supplied fresh random values directly to child-process environments.
No fixed users, passwords, tokens or seed records exist.

## Build and complete tests

These are the exact verified commands for this machine's existing local Maven cache:

```powershell
New-Item -ItemType Directory -Force target/phase7/tmp | Out-Null
mvn -o -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DargLine=-Djava.io.tmpdir=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/phase7/tmp' test
```

Latest result: **870 tests, 32 suites; 0 failures/errors/skips** (Phase 8). All unit and HTTP integration
classes end in `Test` and run through Surefire; no separate Failsafe invocation is needed.
This includes 100 overlapping repetitions of each edit/edit, edit/submit, approve/approve,
request-changes/request-changes and mixed-review pair (500 required pairs), with fresh
stored-state assertions after both workers. It also includes revocation order, rollback,
no-op, visibility, authentication, bounded storage failure and restart checks.

The packaging cache initially lacked the JAR plugin. The successful command used a
local settings file at `target/phase3/maven-settings.xml` containing this non-secret mirror:

```xml
<settings><mirrors><mirror><id>central</id><mirrorOf>*</mirrorOf><url>https://repo.maven.apache.org/maven2</url></mirror></mirrors></settings>
```

Create that ignored settings file with the shown content if it is absent and your configured
mirror is unavailable. Network access is required for missing artifacts; `-o` works only
with a populated cache. No project dependency override was introduced.

```powershell
mvn -s target/phase3/maven-settings.xml -B -ntp '-Dstyle.color=never' '-Dmaven.repo.local=F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow/target/maven-repository' '-DskipTests' package
```

Result: BUILD SUCCESS, `target/changeflow.jar`, Boot JarLauncher main with
`com.changeflow.ChangeFlowApplication` start class, Java 17 bytecode and V1 SQL included.
`-DskipTests` avoids repeating the already completed full run on unchanged source; it is
not a substitute for the test command above. The standard project lifecycle goals are
`test` and `package`; the cache/settings options describe this verified environment.

## Normal startup, graceful stop and restart

With private environment configuration already set, the verified startup is:

```powershell
java -jar target/changeflow.jar --spring.application.admin.enabled=true
```

Plain `java -jar target/changeflow.jar` also started successfully. The explicit flag adds
Boot's local management MBean for the verified shutdown procedure below, without an admin
HTTP endpoint or a remote JMX port. Normal HTTP binds `127.0.0.1:8080`. On a fresh file,
Flyway applies V1 before Hibernate validates the four entity mappings and HTTP is ready.
Restarting with the same environment reopens the same file, validates V1 and preserves data.

To reproduce the automated graceful stop, save this verification helper as
`target/phase7/GracefulStop.java` (create that directory if needed):

```java
import com.sun.tools.attach.VirtualMachine;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
public class GracefulStop {
    public static void main(String[] args) throws Exception {
        var vm = VirtualMachine.attach(args[0]);
        String address;
        try { address = vm.startLocalManagementAgent(); } finally { vm.detach(); }
        try (var connection = JMXConnectorFactory.connect(new JMXServiceURL(address))) {
            connection.getMBeanServerConnection().invoke(
                new ObjectName("org.springframework.boot:type=Admin,name=SpringApplication"),
                "shutdown", new Object[0], new String[0]);
        } catch (java.io.IOException closedOnShutdown) {
            System.out.println("Local management connection closed during shutdown.");
        }
    }
}
```

From a second shell under the same OS user, compile it and use the normal JVM PID shown
by its startup log. The PID variable below must be assigned that actual value:

```powershell
javac --release 17 --add-modules jdk.attach -d target/phase7/helper-classes target/phase7/GracefulStop.java
java --add-modules jdk.attach -cp target/phase7/helper-classes GracefulStop $normalProcessId
```

Wait for process exit, `Graceful shutdown complete` and Hikari `Shutdown completed.` before
opening its database from another JVM. A closed management connection alone is insufficient.
The packaged demonstrations observed exit 0 and both closure messages on every stop.
Console Ctrl+C delivery was not verified through the automation PTY; see the supplemental
check in validation.md. Restart with the same startup command and environment.

## Offline administrative commands

Stop HTTP first. Plain embedded H2 has one owning JVM; these are **offline** commands.
Use the same absolute database URL and private username/password as normal mode.
Each invocation performs one operation, closes the pool and exits. The following syntax
was executed against the packaged artifact; identifier variables represent actual IDs
returned by successful operations and contain no secret credential value:

```powershell
java -jar target/changeflow.jar --changeflow.mode=admin --changeflow.action=provision --changeflow.roles=Requester,Reviewer
java -jar target/changeflow.jar --changeflow.mode=admin --changeflow.action=provision --changeflow.roles=Reviewer
java -jar target/changeflow.jar --changeflow.mode=admin --changeflow.action=issue-credential --changeflow.user-id=$ownerUserId
java -jar target/changeflow.jar --changeflow.mode=admin --changeflow.action=revoke-credential --changeflow.credential-id=$additionalCredentialId
java -jar target/changeflow.jar --changeflow.mode=admin --changeflow.action=remove-role --changeflow.user-id=$ownerUserId --changeflow.role=Requester
```

Provision and issue output one `userId`, `credentialId` and `credential` line **after commit**.
Keep the raw credential privately; it cannot be retrieved from storage. Issue another
credential if necessary; revocation is irreversible. Successful revoke/remove print
`Operation committed.` without a credential. Check `$LASTEXITCODE`: success 0, invalid
arguments/failed operation 1. Actual invalid-role and nonexistent-user issue checks returned
1 and no credentials. The test suite covers `Requester`, `Reviewer`, both, and explicit
`None` role selection. There is no role-add or public administration endpoint.

The executable checks held port 8080 occupied during every admin invocation and even
supplied a servlet override: admin still performed no HTTP bind. After actual offline
revoke/remove and HTTP restart, the revoked credential returned 401, removed Requester
create returned 403, and the remaining Reviewer read the unchanged approved representation.
Live revocation races are tested via the same application service inside the HTTP JVM,
not by claiming separate embedded-file JVMs can administer concurrently.

## HTTP walkthrough and rejection checks

Use `Authorization: Bearer <privately held credential>` and contract DTOs, never JPA
entities or caller-selected owner/roles/state. Do not record the header. Item GET and
successful writes return a quoted item-specific strong ETag. Send that exact tag as
`If-Match` for PATCH, submit, approve and request-changes. Collection listing has no ETag.

The packaged full loop was executed using Java/.NET HTTP clients:

| Action | Body | Observed |
| --- | --- | --- |
| POST /api/change-requests as dual-role owner | title, description, reason | 201 Draft, v0, Location and ETag |
| POST /{id}/submit as owner | absent | 200 PendingReview, v1 |
| POST /{id}/request-changes as independent Reviewer | explanation | 200 ChangesRequested, v2 |
| PATCH /{id} as owner | supplied title only | 200, v3; omitted proposal and feedback retained |
| PATCH /{id} as owner | {} | 200, identical JSON/ETag/v3 |
| POST /{id}/submit as owner | absent | 200 PendingReview, v4 |
| POST /{id}/approve as owner | absent | 403, no effect |
| POST /{id}/approve as independent Reviewer | absent | 200 Approved, v5; feedback retained |
| PATCH /{id} as owner | changed title | 409, immutable |

All item suffixes above are under `/api/change-requests`. GET collection/item visibility,
normalized Unicode limits, controlled/unknown-field rejection, all If-Match forms and
negative operations are covered by the full test run; exact schemas, precedence and problem
codes are in the [HTTP contract](contracts/http-api.md). Fresh stored inspection confirmed
one approved parent/v5, two metadata-only decisions (v2/v5), three hash-only credentials,
one revoked marker and the intended current roles. Final GET JSON/ETag matched before stop.

## Boundaries and evidence

No delete/cancel/withdraw, publication/execution, history, signup, admin HTTP or frontend
surface exists. Latest feedback is replaced only by a later successful request-changes;
approval retains it. H2 is the verified local demonstration database, not evidence of
PostgreSQL portability or multi-process administration. The reviewed Boot-generation
support caveat remains; this phase did not certify production/vendor support.

Known rollback and lost confirmation differ: a 503 after an unacknowledged commit cannot
prove nothing happened. Read current state after recovery; no automatic write retries or
previous-success replay are provided. Actual row-lock waits were 2,100/2,156 ms, pool wait
3,050 ms, transaction-deadline response 10,584 ms including a deliberate 10.5 s test delay.
These measurements demonstrate bounded behavior in the exercised cases, not a latency SLO.

See [validation.md](validation.md#t081--final-evidence-matrix-and-disposition) for every
FR, SC and acceptance-scenario mapping, test-class counts, source/artifact hashes and
honest limitations. The first Converge appended T082/T083, now corrected; a follow-up
Converge against the corrected source is separate from this implementation run.
