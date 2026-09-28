# Implement Issue — Verification Matrix

Run the smallest relevant checks during development, then the full applicable matrix before the issue can enter `In Review`.

Once GitHub Actions gates exist, local verification is not a substitute for the independent CI result.

Do not run tests that cannot provide useful signal for the changed surface, but do not omit a test class merely to save time when the issue affects it.

## 1. Baseline and static checks

When supported by the current implementation:

- backend compile;
- backend formatter/static analysis configured by the project;
- frontend lint;
- TypeScript strict typecheck;
- production builds;
- generated artifact drift checks.

## 2. Unit tests

Required for isolated domain/business behavior where useful.

Coverage expectation:

- happy path;
- edge/boundary conditions;
- invalid inputs;
- important state transitions;
- error behavior.

Do not mock away the behavior being proven.

## 3. Regression tests

Required for bug fixes and previously failing behaviors when practical.

A regression test should:

1. fail without the fix for the expected reason;
2. pass with the fix;
3. remain focused enough to diagnose future recurrence.

## 4. Integration tests

Use Spring Boot + Testcontainers + real PostgreSQL behavior where persistence, transactions, security or framework integration matters.

Examples:

- controller/API + security + service + database;
- repository queries;
- database constraints;
- transaction behavior;
- session persistence;
- Flyway startup/migration compatibility.

Do not replace PostgreSQL-dependent behavior with H2 simply for test convenience.

## 5. Security and tenant tests

Mandatory when relevant.

Prove both allow and deny behavior:

- unauthenticated request denied where required;
- wrong role denied;
- correct role allowed;
- tenant A cannot read tenant B;
- tenant A cannot update/delete tenant B;
- forged/client-supplied organization ID cannot bypass membership;
- reset/verification tokens expire and cannot be reused as designed;
- cookie/session/CSRF behavior remains correct;
- abuse-sensitive auth endpoints respect rate limiting when implemented;
- errors/logs do not reveal secrets/tokens/passwords/session IDs.

## 6. API and contract tests

When API behavior changes:

- endpoint behavior/status codes;
- request validation;
- stable error format;
- pagination/filter semantics when affected;
- OpenAPI generation;
- generated TypeScript client regeneration;
- frontend typecheck/build against the regenerated client.

A backend API change is incomplete if the published/generated client is stale.

## 7. Migration tests

When Flyway changes:

- clean PostgreSQL database migrates from zero to latest;
- previous expected schema state upgrades successfully when practical;
- constraints/defaults/indexes behave as intended;
- risky operations are evaluated for lock/downtime impact;
- already-applied migrations remain immutable;
- incompatible/destructive changes use staged/forward-safe rollout when needed.

For large backfills or data transforms, validate on representative volume when the issue justifies it.

## 8. Frontend behavior tests

Test behavior that can regress, not styling trivia.

Examples:

- form validation/submission;
- loading/error/empty states;
- organization selection;
- auth/session UI flow;
- role-dependent presentation where useful;
- API-client integration;
- accessibility behavior for changed interactive components.

Do not rely on frontend tests as proof of backend authorization.

## 9. End-to-end tests

Use Playwright for critical cross-system journeys affected by the issue.

Core starter candidates:

- register/login/logout;
- verification/reset when test e-mail infrastructure exists;
- create/select organization;
- membership/role-protected operation;
- cross-tenant denial smoke path.

Prefer deterministic conditions and locators. Avoid arbitrary sleeps.

## 10. Container/infrastructure validation

When Docker/Compose/Caddy/runtime changes:

- image builds;
- containers run as intended;
- healthchecks behave correctly;
- required internal connectivity works;
- database/pooler/backend are not accidentally published in production topology;
- runtime secrets are not baked into images.

## 11. Dependency/security/quality validation

Run configured independent tooling when applicable to the current repository state:

- Gitleaks for secret exposure;
- CodeQL for configured supported languages;
- Semgrep CE for approved static/project guardrails;
- Trivy for dependency/configuration vulnerabilities and later container images;
- SonarQube Cloud OSS for quality/New Code analysis and imported coverage when configured;
- Dependabot visibility for package ecosystems that exist;
- security review of auth/authz/input/SQL/CORS/CSRF/headers as applicable.

For final runnable-system hardening, include OWASP ZAP according to `docs/CI-CD-SECURITY.md`.

A CI/security check must not silently pass because its intended target is absent. Add/require the check when the target exists.

Never make a check pass by weakening the protection it validates. False-positive suppressions must be narrow and justified.

## 12. Performance validation

Required only when the issue changes a hot path, potentially large query/list, concurrency behavior, migration cost, bundle weight or known performance-sensitive component.

Use the smallest meaningful evidence:

- query plan / `EXPLAIN`;
- `pg_stat_statements` evidence;
- query-count/N+1 inspection;
- browser profiler/bundle analysis;
- focused benchmark;
- load/concurrency test.

Compare before/after when optimizing an existing bottleneck.

## Coverage completion rule

Before `In Review`:

1. inspect changed meaningful code and its branches;
2. use coverage tooling when available;
3. add tests for reachable changed behavior that remains untested and is stable to automate;
4. explicitly list any meaningful uncovered changed path and why it is not practical/valuable to automate now.

Critical business/security/tenant behavior must not rely on an unexplained coverage exception.
