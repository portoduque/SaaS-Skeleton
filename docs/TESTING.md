# Testing Strategy

## Goal

Tests are the primary safety mechanism for agent-driven development. They protect behavior, security boundaries and integration assumptions without turning the project into a test-maintenance exercise.

## Approach: pragmatic, risk-based TDD

For business rules, authorization, tenant isolation, meaningful data/API behavior and bug fixes:

```text
RED -> GREEN -> REFACTOR -> REVIEW -> VERIFY
```

1. Write/update a test describing the desired behavior.
2. Confirm it fails for the expected reason when practical.
3. Implement the smallest correct solution.
4. Refactor without changing behavior.
5. Review the diff for correctness/security/performance risks.
6. Run relevant and full validation gates.

There is **no blanket global coverage percentage target**. Coverage is a signal, not the goal, but the repository expects an aggressive changed-code standard:

- attempt to cover every new or changed meaningful behavior/path when a stable automated test is practical;
- aim for complete branch/invariant coverage of changed security-critical, authorization, tenant and domain logic;
- measure changed-code coverage when the project's tooling supports it;
- explicitly justify any meaningful changed path that remains uncovered.

Generated code, static configuration, framework internals, trivial markup and CSS should not receive low-value tests merely to inflate a percentage.

## Test classes expected during issue implementation

The `implement-issue` workflow considers all of the following and runs every applicable class:

1. **Unit** — isolated domain/business behavior.
2. **Regression** — reproduces a bug or previously failing behavior before the fix.
3. **Integration** — Spring Boot, security, repositories, transactions and real PostgreSQL/Testcontainers behavior.
4. **Security/tenant** — allow/deny behavior, RBAC, cross-tenant negative tests, session/CSRF/token lifecycle.
5. **Contract/API** — endpoint semantics, validation, OpenAPI and generated TypeScript client compatibility.
6. **Migration** — clean migration, upgrade path where practical, constraints/index behavior and lock/downtime risk.
7. **Frontend behavior** — meaningful forms/states/client integration/accessibility behavior.
8. **E2E** — critical Playwright cross-system journeys.
9. **Static/build** — compile, lint, typecheck and production builds.
10. **Container/security/dependency** — when Docker, runtime topology or dependencies are affected.
11. **Performance/load** — only when the change affects a hot path, large query/list, concurrency behavior, bundle size or migration cost.

The detailed matrix lives in `.agents/skills/implement-issue/references/verification-matrix.md`.

## Independent CI and quality gates

Local or agent-run verification is necessary but not sufficient once a GitHub Actions gate exists. The same relevant build/test/security checks must run independently in CI before merge readiness.

Tool responsibilities are intentionally separated:

- Gitleaks: secrets;
- CodeQL: deeper static/data-flow security analysis;
- Semgrep CE: fast static/project-specific guardrails;
- Trivy: dependency, configuration and container vulnerability scanning;
- SonarQube Cloud OSS: quality/New Code analysis and imported JaCoCo/LCOV coverage;
- Dependabot: update visibility;
- Playwright: critical browser journeys;
- OWASP ZAP: final dynamic web validation once a runnable target exists.

A scanner does not replace behavioral tests. Passing Sonar/CodeQL/Semgrep cannot prove RBAC, tenant isolation, session lifecycle or domain behavior.

CI is introduced incrementally. A check must not report success merely because the application component it is supposed to test does not exist; add the check when its target exists.

See `CI-CD-SECURITY.md`.

## Backend tests

### Unit tests

Use for business rules that do not need Spring or PostgreSQL.

Prefer descriptive behavior names such as:

```text
should_reject_member_when_owner_role_is_required
should_expire_password_reset_token_after_success
```

### Integration tests

Use Spring Boot + Testcontainers + PostgreSQL for:

- repository behavior;
- Flyway migrations;
- transaction behavior;
- authentication/authorization flows;
- tenant isolation;
- API behavior where database integration matters;
- constraints/index-dependent behavior when relevant.

Prefer real PostgreSQL over H2 for database-dependent behavior.

The persistence foundation test starts PostgreSQL 18 and PgBouncer with Testcontainers, boots the application through the proxy, applies Flyway to a clean database and verifies the PostgreSQL version, migration history and `pg_stat_statements` extension. Docker is therefore required for backend `verify`.

### Data-access performance correctness

Tests/reviews should explicitly watch for:

- accidental unbounded queries;
- N+1 query patterns on important paths;
- unexpected eager loading;
- broken pagination/filtering;
- cross-tenant repository methods without tenant scope.

Do not encode brittle SQL-count assertions everywhere. Add query-count/performance assertions only where regression risk justifies them.

## Security-critical mandatory tests

At minimum, automated tests must prove:

- unauthenticated access is rejected where required;
- role restrictions are enforced;
- tenant A cannot read tenant B resources;
- tenant A cannot mutate tenant B resources;
- client-supplied tenant identifiers do not bypass membership checks;
- password-reset tokens expire and cannot be reused after success;
- e-mail verification tokens follow their intended lifecycle;
- sensitive endpoints respect their abuse/rate-limiting strategy when implemented;
- cookie/session flows preserve required CSRF protections.

Security-critical fixes should include a regression test whenever practical.

## Frontend tests

Use tests for meaningful behavior, not CSS implementation details.

Validate:

- TypeScript strict typecheck;
- lint;
- production build;
- critical component/form behavior where useful;
- loading/error/empty states for important flows;
- generated API-client integration.

Do not create unit tests for trivial markup, static styling or framework internals.

Do not test authorization only in the frontend; backend integration tests must prove enforcement.

## E2E tests

Playwright covers only critical cross-system flows initially:

- registration/login/logout;
- password reset when test infrastructure supports e-mail capture;
- create/select organization;
- role-protected operation;
- tenant isolation smoke scenario.

Keep the E2E suite small and reliable.

Prefer condition-based waits/locators over arbitrary sleep/timeouts. Preserve traces/screenshots only when useful for diagnosing failures.

## Migration validation

Every schema change requires a Flyway migration and integration validation against a clean PostgreSQL database.

For the local Compose stack, run `scripts/verify-database-persistence.sh` after the API has applied migrations. It recreates the database containers without deleting the named volume and confirms that Flyway history remains available through PgBouncer.

Already-applied versioned migrations must not be edited casually. Fix forward with a new migration.

For risky data transformations, prefer staged migrations:

```text
add compatible structure -> backfill -> switch application -> remove old structure later
```

## Bug policy

When practical:

1. reproduce the bug in an automated test;
2. confirm failure;
3. implement the smallest fix;
4. confirm the regression test and relevant suite pass.

Do not "fix" a bug by weakening a valid test.

## Flaky tests

Flakiness is a defect.

When a test is flaky:

- identify the race/environment cause;
- replace arbitrary waits with real readiness conditions;
- isolate state/test data;
- do not normalize permanent retries as a substitute for fixing the cause.

Temporary quarantine must reference a tracked issue and remain exceptional.

## Changed-code coverage completion

Before an issue can be moved to `In Review`:

1. inspect the changed meaningful code and its branches;
2. use coverage tooling when available;
3. add tests for reachable changed behavior that remains untested and is stable to automate;
4. list any meaningful uncovered changed path and explain why automation would not be practical or valuable.

A critical business/security/tenant behavior cannot be accepted through an unexplained coverage exception.

## Verification gate

A pull request cannot be considered ready when an applicable check is failing.

Planned gates:

```text
Backend: compile -> unit/integration tests -> verify -> migration validation
Frontend: lint -> typecheck -> relevant tests -> build
Contract: OpenAPI -> generated client -> frontend typecheck/build
E2E: critical smoke tests
Containers: image build when affected
Security: Gitleaks/CodeQL/Semgrep/Trivy plus relevant auth/tenant review when applicable
Quality: SonarQube Cloud quality/New Code analysis when configured
Docs: README/.env/docs synchronization
Diff: accidental/dead/unrelated changes review
```

Passing this gate means the implementation is ready for **human review**, not automatically `Done`. The final manual acceptance procedure lives in `.agents/skills/implement-issue/references/manual-validation.md`.

See `ENGINEERING-WORKFLOW.md` for the complete verification loop.
