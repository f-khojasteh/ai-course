# ChangeFlow Constitution

## Core Principles

### I. Behavior Before Implementation

ChangeFlow MUST use Specification-Driven Development (SDD) through Spec Kit and a coding
agent. Observable behavior, acceptance scenarios, and failure outcomes MUST be defined
and reviewed before implementation begins. Requirements MUST have stable identifiers;
design decisions, ordered tasks, code changes, and verification evidence MUST reference
the requirements they address. Behavior changes MUST update the affected artifacts so
later sessions can reconstruct what was required, decided, built, and verified.

### II. Business Rules and Adapter Responsibilities

Business invariants and use-case decisions MUST reside in Domain or Application.
HTTP endpoints MUST be limited to transport concerns, authentication integration,
mapping, and composition; they MUST delegate business decisions to Application.
Persistence code MUST implement storage and transaction mechanisms without becoming
the sole enforcer of business rules. Application MUST define each business operation's
atomic boundary, and persistence infrastructure MUST enforce that boundary. These
responsibilities keep business behavior testable independently of delivery and storage.

### III. Independent Domain and Directed Dependencies

Domain MUST remain independent of Application, HTTP, Spring, and persistence frameworks.
Application MAY depend on Domain and MUST express external capabilities as contracts
when needed. Infrastructure MUST implement Application contracts; API MUST compose
Application and Infrastructure. Domain and Application MUST NOT depend on API or concrete
Infrastructure implementations. Circular dependencies are forbidden. Application
boundaries MUST use explicit constructor injection. New interfaces, layers, modules,
and dependencies MUST serve a current requirement; speculative abstractions are forbidden.

### IV. Authoritative Authorization

Every sensitive write MUST verify authenticated identity, current permissions, ownership
under the specified ownership rules, and current request state before committing a
change. Identity MUST come from the trusted authentication context; client-supplied roles
or requester identity MUST NOT confer authority. A requester MUST NOT approve their own
request, even when they hold approval permissions. Authorization and state validation
MUST remain valid for the committed operation under concurrent changes; a stale check
MUST NOT authorize a write. Denied operations MUST leave persisted business state unchanged.

### V. Atomic Operations and Protected State

Every business operation MUST commit all of its persisted changes atomically or leave
persisted business state unchanged. Concurrent writes MUST NOT silently overwrite one
another: the design MUST use an explicit concurrency strategy that detects or prevents
conflicting updates and defines an observable conflict outcome. Approved requests MUST
remain immutable across every write path, including edits, transitions, and deletion.
Authorization, state checks, and persistence MUST participate in the concurrency strategy
so competing operations cannot bypass invariants.

### VI. Evidence Through Critical Tests

Tests MUST cover critical business rules, invalid input and state transitions, failure
and rollback paths, authorization, and concurrent operations. Domain and Application
rules MUST be testable without HTTP or a database. Sensitive integration tests MUST
inspect persisted state as well as HTTP responses, including evidence that denied or
failed operations produce no partial changes. Tests MUST include self-approval rejection,
untrusted identity and role input, approved-request immutability, and competing writes.
Concurrency verification MUST exercise the chosen persistence and transaction behavior;
mock-only results are insufficient evidence for those guarantees.

### VII. Secrets and Honest Evidence

Secrets, credentials, and live access tokens MUST NOT be committed in source, configuration,
fixtures, documentation, or evidence. Configuration MUST obtain secrets externally.
Agents and contributors MUST report only tests, reviews, and acceptance checks actually
performed, with their real outcomes. Evidence MUST identify the command or procedure,
relevant revision or change, and result; skipped, blocked, and failing checks MUST be
explicit. Invented test, review, or acceptance results are forbidden.

### VIII. Simple First Version

The initial version MUST use Java 17 and Spring Boot with a simple, maintainable
architecture that can be extended in later sessions. It MUST remain a single backend
application. Frontend, AI, RAG, MCP, vector database, microservices, and multi-agent
functionality are outside the initial version and MUST NOT be implemented or introduced
as dependencies for anticipated future needs. Extension points MUST arise from current
use cases rather than speculative functionality.

### IX. Versioned and Accountable Governance

The constitution MUST record its semantic version, original ratification date, last
amendment date, amendment history, and unresolved governance issues. Every amendment
MUST record its rationale, affected principles, and compatibility impact. Unresolved
questions MUST have an identifier, impact, and resolution stage; resolved questions MUST
retain a reference to the decision. Later sessions MUST consult this record before
changing project behavior or architecture.

## Technology and Scope Constraints

ChangeFlow manages change requests for a software company. The first release MUST use
Java 17 and Spring Boot and implement only behavior agreed in its feature specification.
Domain, Application, Infrastructure, and API are responsibility and dependency boundaries;
they do not require separate deployables or a separate build module for every boundary.
The implementation plan MUST select the smallest structure that enforces these rules.

The plan MUST document authentication integration, storage and transaction choices,
concurrency control, and error mapping before implementing affected behavior. Framework
configuration, HTTP types, and persistence entities MUST NOT leak into the independent
Domain. Application contracts MUST express use-case needs without requiring HTTP or a
specific persistence technology. A future scope expansion MUST follow the amendment
procedure before work that conflicts with this constitution begins.

## Development Workflow and Quality Gates

1. Use `$speckit-specify` to define observable behavior and acceptance scenarios. Resolve
   ambiguities that affect business rules or security before implementing those behaviors.
2. Use `$speckit-plan` to record design decisions, dependency direction, transaction and
   concurrency boundaries, and a constitution compliance assessment.
3. Use `$speckit-tasks` to produce ordered tasks linked to requirements and verification.
   Use `$speckit-implement` only after specification, design, and tasks have been reviewed.
   The review record MUST identify the reviewer, reviewed artifacts, and disposition.
4. Verify critical rules and integration behavior with the tests required by Principle VI.
   Record actual outcomes and remaining gaps; a coding agent MUST NOT claim acceptance
   on the basis of unexecuted checks.
5. Before accepting a change, review traceability, dependency direction, authorization,
   atomicity, immutability, concurrency, scope, and secret handling. A known constitutional
   violation MUST be corrected or addressed by an approved amendment before acceptance.
6. Keep specification, plan, tasks, decision records, and evidence consistent with the
   delivered change so work can continue in a later session without hidden assumptions.

## Governance

This constitution governs ChangeFlow specifications, plans, tasks, code, and reviews.
Conflicting project artifacts MUST be brought into compliance. Contributors and coding
agents MUST identify conflicts explicitly rather than silently bypassing a principle.

Amendments MUST propose the exact rule changes, rationale, compatibility impact, affected
artifacts, and any migration work. The project owner MUST approve the amendment before
adoption. Adoption MUST update this document's version, last-amended date, amendment
history, and unresolved issues. Affected artifacts MUST then be reviewed for consistency;
this constitution command itself MUST NOT modify dependent templates or application code.

Versioning MUST follow semantic versioning: MAJOR for incompatible removals or
redefinitions of governance or principles; MINOR for new principles, sections, or materially
expanded guidance; PATCH for clarifications and other non-semantic corrections. The
original ratification date MUST remain unchanged after initial adoption.

Amendment history:

- 1.0.0 (2026-11-16): Initial adoption of nine principles, Java 17 and Spring Boot constraints,
  SDD review gates, and governance rules. No earlier ratified constitution existed.

Unresolved issues:

- Governance: none at initial adoption.
- CF-OPEN-001: The lifecycle, editable fields, permission and ownership matrix, and observable
  failure/conflict outcomes remain undefined. These determine acceptance and authorization
  behavior and MUST be resolved in the feature specification before affected implementation.
- CF-OPEN-002: Authentication integration, persistence technology, transaction mechanisms,
  and concurrency strategy remain unselected. These determine enforceability of Principles
  IV-VI and MUST be resolved in the implementation plan before affected implementation.

**Version**: 1.0.0 | **Ratified**: 2026-11-16 | **Last Amended**: 2026-11-16
