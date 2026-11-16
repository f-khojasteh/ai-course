# ChangeFlow Agent Guidance

- Read `.specify/memory/constitution.md` as the source of truth for project governance.
  Use the active feature's `spec.md`, `plan.md`, `tasks.md`, and supporting artifacts
  under `specs/` as the sources of truth for behavior, design, and ordered work once
  they exist. Resolve conflicts against the constitution; do not silently override it.
- Keep this file operational and concise. Reference governing artifacts instead of
  duplicating their rules or feature requirements.
- Limit changes to the requested scope and relevant tasks. Preserve unrelated work;
  avoid speculative features, abstractions, dependencies, and broad refactoring.
  Do not begin implementation during governance or specification-only work.
- Use Java 17 and Spring Boot. Respect the constitution's dependency direction and
  the plan's architecture, existing dependencies, and task prerequisites. Justify
  dependency changes against a current requirement.
- Never commit secrets, credentials, or live tokens, including in fixtures or evidence.
- Report validation honestly: state the commands or checks actually run, their outcomes,
  and any skipped, blocked, or failing checks. Never invent test, review, or acceptance
  results or claim that unexecuted checks passed.
- POM/source now exist. Work from
  `F:/oroumand-course-ai/oroumand-course/assignment1/ChangeFlow` with JDK 17 and Maven
  3.6.3+. Verified lifecycle goals: `mvn test` (all 30 Test suites, including stress)
  and `mvn -DskipTests package` after a green full test run on unchanged source;
  output is the executable `target/changeflow.jar`. This machine uses a separate
  Maven cache/mirror: exact executed commands, prerequisites and results are in
  [quickstart](specs/001-change-request-management/quickstart.md) and
  [validation](specs/001-change-request-management/validation.md).
- For executable checks, package first. Supply database URL/username/password externally;
  stop normal HTTP and close its pool before separate offline admin opens the same
  embedded H2 file. Follow verified startup/shutdown/admin commands in quickstart.
