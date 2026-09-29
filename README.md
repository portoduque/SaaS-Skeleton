# SaaS-Skeleton

Open-source SaaS starter focused on **security, performance, modularity, frontend independence and easy future scalability** — without overengineering.

> Current state: **Phase 1 / developer bootstrap**. Spring Boot and Next.js are runnable, the first application CI gates are active, and later phases add PostgreSQL, authentication, organizations, tenant isolation, generated API clients, deployment and final hardening.

## What is already working

The current repository contains:

- Java 25 + Spring Boot 4.1.1 backend shell;
- Next.js 16.3.6 + React 19.3.0 + TypeScript strict frontend shell;
- Maven Wrapper 3.3.4 using Maven 3.9.16;
- reproducible npm install through `package-lock.json`;
- JUnit/Spring context test;
- Vitest + React Testing Library frontend tests;
- JaCoCo XML + LCOV coverage reports;
- backend and frontend GitHub Actions CI;
- Gitleaks, Semgrep CE and Trivy security checks;
- CodeQL for Java/Kotlin and JavaScript/TypeScript;
- SonarQube Cloud Quality Gate integration;
- Dependabot for GitHub Actions, Maven and npm.

PostgreSQL, PgBouncer, Flyway and Docker Compose runtime services are intentionally introduced in the next implementation phase. Do not expect `docker compose up` to run the application yet.

## Principles

- **API-first and frontend-agnostic**: the backend must work with web, mobile, desktop or other clients.
- **Simple now, scalable later**: avoid decisions that block future horizontal scaling, but do not introduce infrastructure before it solves a real problem.
- **Security by default**: authorization, tenant isolation, authentication, secrets handling and dependency hygiene are mandatory.
- **Modular monolith first**: organize by feature; no microservices without a concrete need.
- **No overengineering**: every abstraction or infrastructure component must solve a demonstrated problem.
- **Open-source friendly**: cloning, running, testing and contributing must be straightforward.
- **Documentation is part of the product**: setup/tooling changes without synchronized onboarding are incomplete.

## Architecture direction

The planned production topology is:

```text
Internet
   |
 Caddy
   |
   +--> Next.js
   |
   +--> Spring Boot API
            |
        PgBouncer
            |
        PostgreSQL
```

The backend remains the business/security boundary and is designed to serve multiple clients later:

```text
Next.js Web -------+
React Native ------+
Desktop -----------+--> Spring Boot API --> PostgreSQL
Other integrations +
```

## Stack

### Implemented now

| Area | Technology |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1.1, Maven Wrapper |
| Frontend | Next.js 16.3.6, React 19.3.0, TypeScript 6.0.3 |
| Frontend tests | Vitest 5.0.2, Testing Library, jsdom |
| Lint | ESLint 9.39.5 + eslint-config-next 16.3.6 |
| Coverage | JaCoCo 0.8.15 + LCOV |
| CI | GitHub Actions |
| Security | Gitleaks, Semgrep CE, Trivy, CodeQL |
| Quality | SonarQube Cloud |
| Dependency updates | Dependabot |

> ESLint 9.39.5 is intentionally pinned for now. ESLint 10 was tested during POR-6, but transitive plugins used by `eslint-config-next 16.3.6` are not yet compatible with it. Dependabot will surface the upgrade again when the upstream chain changes.

### Planned by later phases

- PostgreSQL 18;
- PgBouncer;
- Flyway;
- Spring Security;
- Spring Data JPA / Hibernate;
- OpenAPI + generated TypeScript client;
- Tailwind CSS + shadcn/ui + Lucide;
- TanStack Query + React Hook Form + Zod;
- Testcontainers;
- Playwright;
- Docker Compose;
- Caddy;
- external database backups;
- OWASP ZAP final dynamic validation.

## Repository layout

```text
SaaS-Skeleton/
├── .agents/
│   └── skills/
│       └── implement-issue/
├── .claude/
│   └── commands/
│       └── implement-issue.md
├── .github/
│   ├── workflows/
│   │   ├── ci.yml
│   │   ├── codeql.yml
│   │   ├── security-baseline.yml
│   │   └── sonar.yml
│   ├── dependabot.yml
│   └── pull_request_template.md
├── apps/
│   ├── api/
│   │   ├── .mvn/wrapper/
│   │   ├── src/
│   │   ├── mvnw
│   │   ├── mvnw.cmd
│   │   └── pom.xml
│   └── web/
│       ├── src/
│       ├── eslint.config.mjs
│       ├── package.json
│       ├── package-lock.json
│       ├── tsconfig.json
│       └── vitest.config.mts
├── docs/
├── .env.example
├── .semgrep.yml
├── sonar-project.properties
├── AGENTS.md
├── CHANGELOG.md
├── CONTRIBUTING.md
├── LICENSE
├── README.md
└── SECURITY.md
```

# Quick start

This section is intentionally sequential. A new contributor should be able to start with a clean machine and follow it from top to bottom.

## 1. Install prerequisites

Required for the current Phase 1 repository:

- **Git**;
- **Java 25** — Eclipse Temurin is the CI distribution;
- **Node.js 24.15.0 or newer within the 24.x line**;
- **npm 11.19.0** for the canonical tested setup.

Optional but recommended:

- **Docker** — needed only to run Gitleaks/Semgrep/Trivy locally using the same simple containerized approach documented below;
- a GitHub account — needed only if you intend to push a fork/repository and use CI;
- a SonarQube Cloud account — needed only to reproduce the canonical Sonar integration in your own repository.

The current GitHub Actions environment is tested with:

```text
Java: 25
Node: 24.21.0
npm: 11.19.0
Maven: 3.9.16 via Maven Wrapper
```

Check your machine:

```bash
git --version
java --version
node --version
npm --version
```

Expected minimums:

```text
Java 25
Node >= 24.15.0 and < 25
npm 11.x
```

You do **not** need a global Maven installation. Always use the committed Maven Wrapper.

## 2. Clone the repository

```bash
git clone https://github.com/portoduque/SaaS-Skeleton.git
cd SaaS-Skeleton
```

## 3. Environment variables

Phase 1 does not require runtime environment variables yet.

`.env.example` exists as the canonical future template, but there is nothing you need to copy or edit to run the current backend/frontend shells.

When later phases add PostgreSQL, authentication or other runtime configuration, this section and `.env.example` must be updated in the same change.

## 4. Verify the backend

From the repository root:

```bash
cd apps/api
./mvnw --version
./mvnw --batch-mode --no-transfer-progress verify
```

Windows Command Prompt / PowerShell:

```powershell
cd apps/api
.\mvnw.cmd --version
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Expected result:

- Maven downloads the pinned distribution automatically on first use;
- Java 25 is detected;
- the Spring Boot context test passes;
- the command exits with code `0`;
- JaCoCo generates `apps/api/target/site/jacoco/jacoco.xml`.

To start the API shell:

```bash
./mvnw spring-boot:run
```

Expected log:

```text
Started SaasSkeletonApplication
```

The application listens on Spring Boot's default port `8080`. There are intentionally no product API endpoints yet, so receiving `404` at `/` is expected in Phase 1.

Stop it with `Ctrl+C`.

## 5. Verify the frontend

Open a second terminal from the repository root:

```bash
cd apps/web
npm ci --no-audit --no-fund
npm run lint
npm run typecheck
npm test
npm run build
```

Expected result:

- `npm ci` installs exactly the committed lockfile;
- ESLint exits with code `0`;
- TypeScript strict typecheck exits with code `0`;
- Vitest tests pass and create `coverage/lcov.info`;
- Next.js production build succeeds.

To start the development server:

```bash
npm run dev
```

Open:

```text
http://localhost:3000
```

Expected page:

- heading `SaaS-Skeleton`;
- Java/Spring Boot, Next.js/React/TypeScript and PostgreSQL foundation cards.

Stop it with `Ctrl+C`.

## 6. Run backend and frontend together

Terminal 1:

```bash
cd apps/api
./mvnw spring-boot:run
```

Terminal 2:

```bash
cd apps/web
npm run dev
```

Current Phase 1 behavior:

```text
Next.js:    http://localhost:3000
Spring:     http://localhost:8080
Database:   not introduced yet
API bridge: not introduced yet
```

The frontend must not access PostgreSQL directly. Later integration is always through the Spring Boot API.

# Local verification

Before opening a pull request, run the checks affected by your change.

## Backend

```bash
cd apps/api
./mvnw --batch-mode --no-transfer-progress verify
```

## Frontend

```bash
cd apps/web
npm ci --no-audit --no-fund
npm run lint
npm run typecheck
npm test
npm run build
```

No arbitrary global coverage percentage is required by the repository workflow. Tests are risk-based and should cover changed meaningful behavior. SonarQube separately applies its Quality Gate to new code.

# Security baseline locally

GitHub Actions is the authoritative merge-time gate. For quick local parity, Docker avoids installing additional security CLIs globally.

From the repository root:

```bash
# Secrets
docker run --rm -v "$PWD:/repo" -w /repo zricethezav/gitleaks:v8.30.1 git --redact --verbose .

# Project guardrails
docker run --rm -v "$PWD:/src" semgrep/semgrep:1.178.0 semgrep scan --config /src/.semgrep.yml --error --metrics=off /src

# Dependencies / configuration
docker run --rm -v "$PWD:/repo" -w /repo aquasec/trivy:0.70.0 fs --scanners vuln,misconfig --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 .
```

Expected result: every command exits with status `0` and reports no blocking finding.

If Docker cannot expand `$PWD` on your shell, replace it with the absolute path to the cloned repository.

## Tool responsibilities

| Tool | Responsibility |
| --- | --- |
| Gitleaks | committed/current secret detection |
| Semgrep CE | fast project-specific guardrails |
| Trivy | dependency/configuration vulnerability scanning |
| CodeQL | deeper code/data-flow security analysis |
| SonarQube Cloud | New Code quality gate + imported coverage |
| Dependabot | dependency/GitHub Actions update visibility |

The overlap is intentional only where the tools provide materially different signal. A scanner never replaces behavior/security tests.

# GitHub Actions setup for a fork or copy

If you are only developing locally, you can skip this section.

If you maintain your own GitHub repository based on SaaS-Skeleton:

## 1. Push the repository to GitHub

Make sure GitHub Actions is enabled for the repository.

The committed workflows automatically run on pull requests to `main` and pushes to `main`.

## 2. Application CI

No secret is required.

The `Application CI` workflow runs:

```text
CI / Backend
  -> Java 25
  -> Maven Wrapper verify
  -> JaCoCo report check

CI / Frontend
  -> Node 24
  -> npm ci
  -> lint
  -> typecheck
  -> tests/LCOV
  -> Next.js production build
```

## 3. Security Baseline

No external account is required.

The workflow runs:

- `Security / Secrets` — Gitleaks;
- `Security / Static Guardrails` — Semgrep CE;
- `Security / Dependencies and Config` — Trivy.

## 4. CodeQL

No additional secret is required for a public GitHub repository.

The committed workflow analyzes:

- `java-kotlin`;
- `javascript-typescript`.

It also runs weekly to detect security findings even when no pull request is active.

## 5. Dependabot

No manual token is required.

`.github/dependabot.yml` currently monitors weekly:

- GitHub Actions;
- Maven in `/apps/api`;
- npm in `/apps/web`.

Dependency PRs are not auto-merged.

# SonarQube Cloud setup

The canonical repository uses CI-based SonarQube Cloud analysis so JaCoCo and LCOV coverage can be imported.

Do **not** commit a Sonar token.

## 1. Create/connect the SonarQube organization

1. Sign in to SonarQube Cloud with GitHub.
2. Use `+` → **Create new organization**.
3. Connect the GitHub account/organization that owns your repository.
4. Grant the SonarQube GitHub App access only to the repositories you want analyzed when practical.
5. For a public open-source repository, choose **Get SonarQube for OSS** during organization creation when eligible. The OSS choice is made at organization creation; do not assume a normal Free organization can be converted later.
6. During repository selection, select only the intended project and disable **Auto-import new GitHub repositories** unless you explicitly want that behavior.

If you accidentally created the organization under the normal **Free** plan and your goal is the dedicated OSS plan, correct that before accumulating project history. SonarQube currently treats the plan selection as an organization-creation decision.

## 2. Import the repository

Inside the SonarQube organization:

1. choose **Analyze new project**;
2. select your SaaS-Skeleton repository;
3. create the project;
4. choose **With GitHub Actions** as the analysis method;
5. do not copy Sonar's generic single-stack workflow — this repository already contains the correct monorepo workflow.

## 3. Record the public keys

Open **Project → Project Information** and note:

```text
Organization Key
Project Key
```

For the canonical repository they are:

```text
Organization Key: portoduque
Project Key: portoduque_SaaS-Skeleton
```

If you fork/copy the repository under a different Sonar organization/project, update these two properties in `sonar-project.properties`.

## 4. Create the token

In SonarQube Cloud:

1. open your profile;
2. go to **My Account → Security**;
3. generate a token, for example `saas-skeleton-github-actions`;
4. copy it immediately.

Treat the token as a secret. Never paste it in an issue, pull request, screenshot, log or committed file.

If a token is accidentally exposed, revoke it immediately and create a replacement.

## 5. Save the GitHub secret

GitHub repository:

```text
Settings
→ Secrets and variables
→ Actions
→ Repository secrets
→ New repository secret
```

Create:

```text
Name:  SONAR_TOKEN
Value: <token generated by SonarQube Cloud>
```

No other Sonar secret is required by the current workflow.

## 6. Analysis method

Use CI-based analysis.

If Automatic Analysis is enabled in SonarQube Cloud, disable it before relying on the GitHub Actions workflow. Do not run Automatic Analysis and the committed CI-based scan as competing sources.

## 7. Quality Gate

Keep the built-in **Sonar way** Quality Gate unless a concrete future requirement justifies something different.

The current policy intentionally focuses on New Code. The canonical setup also keeps **Ignore duplication and coverage on small changes** enabled.

Do not weaken the gate merely to make a pull request pass.

## 8. Verify Sonar

Push a branch and open a pull request to `main`.

Expected GitHub check:

```text
Quality / SonarQube
```

Expected Sonar behavior:

- Java bytecode and dependency classpaths are available to the analyzer;
- JaCoCo XML is imported from the backend;
- LCOV is imported from the frontend;
- the workflow waits for the Quality Gate;
- the job fails when the Quality Gate fails;
- the job passes when the Quality Gate passes.

The Sonar workflow intentionally does not expose `SONAR_TOKEN` to untrusted fork pull requests.

# Expected pull-request checks

A normal trusted pull request currently produces:

```text
CI / Backend
CI / Frontend

Security / Secrets
Security / Static Guardrails
Security / Dependencies and Config

CodeQL / java-kotlin
CodeQL / javascript-typescript

Quality / SonarQube
```

A green CI result does not automatically authorize merge or deployment. Human validation remains a separate gate.

# Troubleshooting

## `./mvnw: Permission denied`

On Unix-like systems:

```bash
chmod +x apps/api/mvnw
```

The repository stores `mvnw` as executable; this usually only appears after unusual file transfer/copy behavior.

## Java version error

Check:

```bash
java --version
```

The backend requires Java 25.

## `npm ci` rejects your Node version

Check:

```bash
node --version
npm --version
```

Use Node `>=24.15.0 <25`. The canonical CI currently uses Node 24.21.0 with npm 11.19.0.

## ESLint 10 fails with Next.js plugins

This is currently an upstream compatibility limitation of the dependency chain used by `eslint-config-next 16.3.6`.

Do not force ESLint 10 with peer-dependency overrides. Keep the committed ESLint 9.39.5 until the upstream plugins support ESLint 10; Dependabot will surface future updates.

## SonarQube says project not found / not authorized

Verify:

- `SONAR_TOKEN` exists as a GitHub Actions repository secret;
- `sonar.organization` matches your Sonar organization key;
- `sonar.projectKey` matches your project key;
- the token belongs to a user allowed to analyze the project;
- Automatic Analysis is not competing with the CI-based workflow.

## Sonar Quality Gate fails on coverage

Do not lower the threshold reflexively.

Check first that:

- `apps/api/target/site/jacoco/jacoco.xml` exists;
- `apps/web/coverage/lcov.info` exists;
- meaningful changed code is actually exercised by tests;
- framework/bootstrap-only code is excluded only when testing it would be artificial and the exclusion is narrow/documented.

## A security scanner fails

Use this sequence:

```text
read the exact finding
→ determine whether it is real
→ fix the root cause
→ rerun the focused check
→ rerun the broader applicable gate
```

A false-positive suppression must be narrow and justified. Never disable a scanner simply to make CI green.

# Development workflow

The project uses a lightweight agent-friendly engineering loop:

```text
UNDERSTAND
   ↓
RESEARCH
   ↓
PLAN
   ↓
TEST (RED)
   ↓
IMPLEMENT (GREEN)
   ↓
REFACTOR
   ↓
REVIEW
   ↓
VERIFY
   ↓
DOCUMENT
   ↓
IN REVIEW
   ↓
HUMAN MANUAL VALIDATION
   ↓
DONE
```

Testing is risk-based rather than driven by a blanket global repository percentage. Critical business/security logic should have strong branch/invariant coverage where practical.

A task is not complete while any required build, test, migration validation, security check or affected documentation is outdated.

**Code updated + README stale = task incomplete.**

**Tooling changed + onboarding stale = task incomplete.**

## Autonomous issue workflow

The canonical issue implementation skill lives at:

```text
.agents/skills/implement-issue/SKILL.md
```

Invocation examples:

Codex:

```text
$implement-issue POR-9
```

Antigravity:

```text
/implement-issue POR-9
```

Claude Code:

```text
/implement-issue POR-9
```

Automated success ends at `In Review`; `Done` requires explicit human confirmation that the issue-specific manual validation guide passed. Tracker status changes require explicit authority for that run. Merge and deployment are separate actions.

Workflow improvements may be proposed from real evidence, but changes to the canonical workflow require explicit human approval.

# Documentation

- [Product requirements](docs/PRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Scope](docs/SCOPE.md)
- [Implementation plan](docs/IMPLEMENTATION_PLAN.md)
- [Testing strategy](docs/TESTING.md)
- [Security architecture](docs/SECURITY-ARCHITECTURE.md)
- [Engineering workflow](docs/ENGINEERING-WORKFLOW.md)
- [CI/CD and DevSecOps](docs/CI-CD-SECURITY.md)
- [Agent rules](AGENTS.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

# Roadmap note

Later phases add the one-command Docker Compose onboarding described by the architecture. Until PostgreSQL/PgBouncer/Flyway and runtime containers exist, the correct Phase 1 local flow is the explicit backend/frontend setup documented above.

# License

Apache License 2.0. See [LICENSE](LICENSE).
