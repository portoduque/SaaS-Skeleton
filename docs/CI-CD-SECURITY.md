# CI/CD and DevSecOps

## Purpose

SaaS-Skeleton treats CI/CD as an independent safety layer for code produced by humans or coding agents. An agent may implement and verify a change locally, but GitHub is the enforcement boundary for merge readiness.

The target flow is:

```text
branch
  -> local verification
  -> pull request
  -> GitHub Actions
       -> build/tests
       -> static/security checks
       -> quality checks
       -> integration/E2E checks when available
  -> required checks green
  -> human review/acceptance
  -> merge
  -> delivery/deployment only through a separately authorized workflow
```

Passing CI never authorizes an automatic production deployment.

## Core rules

1. CI starts with the first application code, not near the end of the project.
2. A required check must prove a real property. Do not create placeholder checks that pass because the component does not exist.
3. Tooling is stack-specific. SaaS-Skeleton does not use generic stack auto-detection.
4. Security tools must have distinct responsibilities to reduce duplicate noise.
5. Required security controls may not be weakened merely to make a pipeline green.
6. Third-party GitHub Actions are pinned to immutable commit SHAs and updated deliberately, preferably through Dependabot.
7. GitHub Actions uses least-privilege permissions, job timeouts and concurrency cancellation where appropriate.
8. Pull requests from forks must never receive repository secrets. Do not use `pull_request_target` to execute untrusted fork code.
9. False positives are suppressed only with a narrow, documented reason.
10. Tool/configuration changes are incomplete until README onboarding is synchronized.

## Open-source onboarding invariant

The root `README.md` is the installation manual for the project.

A new user must be able to start from a clean machine, clone the repository, follow the README in order and reach a fully configured, working project without relying on undocumented knowledge.

Whenever an issue adds or changes any of the following, the README must be reviewed and updated in the same change:

- runtime prerequisites or supported versions;
- environment variables or secrets;
- build/test commands;
- Docker/Compose services or ports;
- GitHub Actions;
- SonarQube Cloud;
- Semgrep;
- CodeQL;
- Gitleaks;
- Trivy;
- Dependabot;
- Playwright;
- OWASP ZAP;
- branch/ruleset configuration;
- deployment or backup/restore steps.

Each external tool section must explain, in order:

1. what the tool does;
2. whether it is required or optional;
3. whether an account is required;
4. exact account/project setup steps when applicable;
5. exact GitHub Secret/Variable names;
6. local command when available;
7. how to trigger the CI check;
8. the expected successful result;
9. common setup failures and the smallest fix.

**Tooling changed + onboarding stale = task incomplete.**

## Tool responsibility map

| Tool | Primary responsibility |
| --- | --- |
| Compiler / TypeScript strict | structural/type errors |
| Automated tests | behavioral correctness |
| Gitleaks | committed and current secret detection |
| GitHub Secret Scanning | platform-side secret detection for the public repository |
| CodeQL | deeper code/data-flow security analysis |
| Semgrep Community Edition | fast static guardrails and project-specific patterns |
| Trivy | dependency, configuration and container vulnerability scanning |
| SonarQube Cloud OSS | quality gate, maintainability, duplication, bugs and imported coverage |
| Dependabot | dependency and GitHub Actions update visibility |
| Playwright | critical browser journeys |
| OWASP ZAP | dynamic web security validation once a runnable target exists |

## Phase rollout

### Phase 1 / POR-6 — application bootstrap + CI baseline

As soon as `apps/api` and `apps/web` exist, add required CI for:

Backend:
- Java 25 setup;
- Maven Wrapper only;
- compile/test/verify.

Frontend:
- pinned supported Node version;
- reproducible install using the committed lockfile;
- lint;
- TypeScript strict typecheck;
- behavior/unit tests when present;
- production build.

Security/quality:
- Gitleaks;
- CodeQL for `java-kotlin` and `javascript-typescript`;
- Semgrep CE;
- Trivy filesystem/dependency/misconfiguration scan;
- SonarQube Cloud OSS analysis with JaCoCo/LCOV coverage when test tooling supports it;
- Dependabot for Maven, npm, GitHub Actions and Docker ecosystems as they become present.

A minimal main-branch ruleset may be enabled only after the checks are stable and their status names are known.

### Phase 2 / POR-7 — PostgreSQL, PgBouncer and Flyway

Add:
- real PostgreSQL/Testcontainers integration;
- clean-database Flyway migration validation;
- prior-state upgrade validation when meaningful;
- PgBouncer integration where the issue affects it.

Do not substitute H2 for PostgreSQL behavior.

### Phase 3 / POR-8 — API and observability

Add:
- API validation/error integration tests;
- health endpoint validation;
- deterministic OpenAPI generation.

### Phase 4 / POR-9 — authentication

Add mandatory negative/positive tests for:
- password handling;
- login/logout;
- session persistence;
- cookie attributes;
- CSRF;
- fixation protection;
- verification/reset tokens;
- abuse-sensitive rate limiting.

### Phases 5-6 / POR-10 and POR-11 — organizations and tenant isolation

Required CI invariants include:
- correct role allowed;
- incorrect role denied;
- tenant A cannot read tenant B;
- tenant A cannot mutate/delete tenant B;
- client-supplied organization identifiers do not bypass membership checks.

### Phase 7 / POR-12 — generated TypeScript client

Add a deterministic contract-drift gate:

```text
generate OpenAPI
  -> regenerate TypeScript client
  -> verify repository diff is clean
```

### Phases 8-9 / POR-13 and POR-14 — frontend + E2E

Add:
- behavior-focused component tests;
- useful automated accessibility checks;
- Playwright critical journeys;
- stable deterministic fixtures/locators.

Do not create tests for static Tailwind classes or trivial framework markup.

### Phase 10 / POR-15 — CI/DevSecOps hardening

POR-15 hardens the CI created earlier rather than introducing CI for the first time.

Audit and finalize:
- workflow permissions;
- immutable action SHAs;
- cache safety;
- timeouts;
- concurrency;
- artifact retention;
- fork behavior and secret isolation;
- CodeQL configuration;
- Semgrep project-specific rules justified by observed recurring problems;
- Trivy image scans;
- SonarQube Quality Gate and New Code policy;
- Dependabot coverage;
- container build validation;
- final required-check set;
- main ruleset/branch protection.

The final main ruleset should, when supported:
- require pull requests;
- require the selected CI/security checks;
- block force pushes;
- block branch deletion;
- prevent normal direct pushes to `main`.

### Phase 11 / POR-16 — Continuous Delivery

Create a reproducible reference delivery path for the one-VM Docker/Caddy topology.

Default behavior:
- merge to `main` builds/verifies deliverable artifacts/images;
- production deployment remains separately authorized;
- use `workflow_dispatch` and a GitHub Environment or equivalent approval boundary for production reference deployment.

SaaS-Skeleton provides Continuous Delivery by default, not mandatory unattended Continuous Deployment.

### Phase 12 / POR-17 — final dynamic/security/clean-room validation

Add/validate:
- OWASP ZAP baseline scan against an ephemeral or staging target;
- manual/explicit full active scan only against an approved non-production target;
- real PostgreSQL backup -> disposable restore -> integrity/application validation;
- final Trivy image/config scans;
- full Gitleaks/CodeQL/Semgrep/Sonar suite;
- complete Playwright critical flow suite;
- clean-room/fresh-clone README validation;
- repository cleanup and dependency review.

POR-17 must validate existing controls. It must not be the first time a fundamental CI/security control is introduced.

## SonarQube Cloud OSS

The canonical project uses SonarQube Cloud's open-source plan when the repository remains public and eligible.

Rules:
- analysis is CI-based so coverage reports can be imported;
- backend coverage uses JaCoCo XML;
- JavaScript/TypeScript coverage uses LCOV;
- Quality Gate focuses on useful New Code signals rather than an arbitrary global coverage percentage;
- `SONAR_TOKEN` is stored only as a GitHub Actions secret;
- fork pull requests do not receive `SONAR_TOKEN`;
- do not use `pull_request_target` to work around fork-secret restrictions.

If Sonar cannot run for an untrusted fork, secret-independent required checks must still run. Sonar runs again on trusted `main`.

## Semgrep policy

Semgrep CE is complementary to CodeQL.

Initially use a small, high-signal ruleset. As real recurring agent mistakes are observed, project-specific rules may be proposed through the repository's human-governed workflow-improvement process.

A new Semgrep rule must:
- represent a reusable problem;
- have a concrete example/evidence;
- avoid duplicating an existing compiler/test/CodeQL control without benefit;
- include a positive/negative fixture when practical;
- receive explicit human approval when it changes workflow-governing behavior.

## Gitleaks policy

Gitleaks is the CI secret scanner. Prefer the standalone CLI/container over a configuration that requires an external license key for organization-owned forks/clones.

Scans must redact secret values in logs.

Do not normalize committed secrets through a broad baseline. A real exposed secret must be removed from the repository state as appropriate and rotated at the provider.

## Trivy policy

Use Trivy for:
- filesystem/dependency vulnerabilities;
- configuration/misconfiguration;
- container images when images exist.

Initially fail on actionable HIGH/CRITICAL findings. Unfixed upstream findings require explicit review rather than automatic suppression.

## Dependency update policy

Dependabot must cover only ecosystems that actually exist.

Expected evolution:
- GitHub Actions immediately after workflows exist;
- Maven after `apps/api/pom.xml` exists;
- npm after `apps/web/package.json` and lockfile exist;
- Docker after Dockerfiles/images exist.

Prefer grouped weekly updates to avoid PR noise. Do not auto-merge dependency PRs by default.

## Required-check design

Avoid dozens of tiny required checks.

Prefer a small stable surface such as:
- `CI / Backend`;
- `CI / Frontend`;
- `CI / Integration`;
- `Security / Secrets`;
- `Security / Static`;
- `Security / Dependencies`;
- `Quality / SonarQube`;
- `E2E / Playwright`;
- `Containers`.

Only checks that are guaranteed to report on every relevant pull request should be configured as required.

## Failure policy

When a check fails:

```text
observe exact failure
  -> establish whether it is real or false positive
  -> make the smallest correct fix
  -> rerun the focused check
  -> rerun the broader applicable gate
```

Forbidden shortcuts:
- deleting/disabling a valid test;
- weakening security configuration;
- broad ignore patterns;
- silently skipping scans;
- hiding flaky failures with permanent retries;
- changing a Quality Gate merely to make a PR pass.

## Final success condition

The DevSecOps baseline is complete only when:

1. meaningful code cannot merge while required checks fail;
2. repository secrets are not exposed to untrusted fork code;
3. every configured tool has a clear non-duplicated purpose;
4. the core OSS setup can be used without a paid requirement;
5. a clean user can clone the repository and configure the full supported stack by following only the root README.
