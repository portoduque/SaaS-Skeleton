# SaaS-Skeleton

Open-source SaaS starter focused on **security, performance, modularity, frontend independence and easy future scalability** — without overengineering.

> Status: foundation phase. The project is being built incrementally with tests and documentation gates before each implementation phase advances.

## Goals

SaaS-Skeleton is a reusable base for future SaaS products. It provides the cross-cutting foundation that almost every SaaS needs while intentionally avoiding product-specific features.

Core principles:

- **API-first and frontend-agnostic**: the backend must work with web, mobile, desktop or other clients.
- **Simple now, scalable later**: start with one VM and Docker Compose, but avoid decisions that block horizontal scaling.
- **Security by default**: authorization, tenant isolation, secure authentication, secrets handling and dependency hygiene are mandatory.
- **Performance-conscious**: PostgreSQL, connection pooling, stateless-ready backend and measurable performance.
- **Modular monolith first**: organize by feature and keep boundaries clear; no microservices without a real need.
- **No overengineering**: every abstraction or infrastructure component must solve a concrete problem.
- **Open-source friendly**: cloning, running, testing and contributing must be straightforward.
- **Documentation stays current**: changes that affect usage, setup, architecture, commands or configuration must update the README/docs in the same change.

## Planned stack

### Frontend

- Next.js 16
- React
- TypeScript with strict mode
- Tailwind CSS
- shadcn/ui
- Lucide
- Motion
- TanStack Query
- React Hook Form
- Zod

### Backend

- Java 25 LTS
- Spring Boot 4.1.x
- Spring Security
- Spring Data JPA / Hibernate
- OpenAPI
- Maven Wrapper
- JUnit
- Testcontainers

### Data

- PostgreSQL 18
- PgBouncer
- Flyway
- UUIDv7 as the default public identifier strategy

### Infrastructure

- Docker Compose
- Caddy
- GitHub Actions
- External database backups

## Architecture

Initial production topology:

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

The backend is designed so the same API can later serve multiple clients:

```text
Next.js Web -------+
React Native ------+
Desktop -----------+--> Spring Boot API --> PostgreSQL
Other integrations +
```

Future horizontal scaling must not require rewriting the business core:

```text
Load Balancer
     |
Spring Boot x N
     |
 PgBouncer
     |
PostgreSQL
```

## Initial SaaS foundation

The starter will initially include only the reusable core:

- authentication;
- users;
- organizations;
- memberships;
- simple RBAC: `OWNER`, `ADMIN`, `MEMBER`;
- tenant isolation;
- REST API;
- OpenAPI contract;
- generated TypeScript API client;
- responsive frontend shell and design system;
- PostgreSQL migrations;
- health checks and structured logs;
- automated tests;
- Docker-based local environment;
- CI validation;
- documented backup/restore path.

Not included by default:

- microservices;
- Kubernetes;
- Kafka;
- RabbitMQ;
- Elasticsearch;
- ClickHouse;
- Redis without a concrete need;
- Stripe/billing;
- product-specific modules.

## Repository layout

```text
SaaS-Skeleton/
├── .agents/
│   └── skills/
│       └── implement-issue/
│           ├── SKILL.md
│           ├── agents/openai.yaml
│           └── references/
│               ├── verification-matrix.md
│               ├── output-contract.md
│               ├── manual-validation.md
│               └── continuous-improvement.md
├── .claude/
│   └── commands/
│       └── implement-issue.md
├── .github/
│   ├── workflows/
│   │   └── security-baseline.yml
│   ├── dependabot.yml
│   └── pull_request_template.md
├── apps/
│   ├── api/          # Spring Boot
│   └── web/          # Next.js
├── docs/
│   ├── PRD.md
│   ├── ARCHITECTURE.md
│   ├── SCOPE.md
│   ├── IMPLEMENTATION_PLAN.md
│   ├── TESTING.md
│   ├── SECURITY-ARCHITECTURE.md
│   ├── ENGINEERING-WORKFLOW.md
│   └── CI-CD-SECURITY.md
├── infra/
├── scripts/
├── .semgrep.yml
├── AGENTS.md
├── CONTRIBUTING.md
├── SECURITY.md
├── CHANGELOG.md
├── .env.example
└── compose.yaml
```

Implementation directories are created incrementally according to the implementation plan.

## Development workflow

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

Testing is risk-based rather than driven by a blanket global percentage. The project still expects agents to cover **all new or changed meaningful behavior when practical**, with especially strong branch/invariant coverage for security, authorization, tenant isolation and domain rules. Review findings must be evidence-based, performance work must be measured, and security review is mandatory for sensitive boundaries.

A task is not complete while any required build, test, migration validation, security check or affected documentation is outdated.

**Code updated + README stale = task incomplete.**

**Tooling changed + onboarding stale = task incomplete.**

## Autonomous issue workflow

The repository includes one canonical workflow for implementing a Linear issue end-to-end:

```text
.agents/skills/implement-issue/SKILL.md
```

It performs:

```text
ISSUE
  ↓
CONTEXT + SCOPE
  ↓
PATTERN/SOURCE CHECK
  ↓
SMALL IMPLEMENTATION SLICES
  ↓
TDD: RED → GREEN → REFACTOR
  ↓
SECURITY + PERFORMANCE REVIEW
  ↓
FULL APPLICABLE TEST MATRIX
  ↓
DIFF + DOCUMENTATION SYNC
  ↓
PR + LINEAR IN REVIEW
  ↓
MANUAL VALIDATION GUIDE
  ↓
HUMAN ACCEPTANCE → DONE
```

The workflow is intentionally autonomous after explicit invocation. It may stop for a genuine blocker or an unapproved irreversible/destructive action, but it must not interrupt for routine implementation decisions. It never merges or deploys automatically.

A successful automated run ends in **`In Review`**, not `Done`. The agent links/creates the PR when tooling allows and returns a complete issue-specific manual validation guide with simple numbered actions and the expected result after each step. It changes the Linear status only when explicit tracker-status authority has been granted for that run. `Done` requires explicit human confirmation that this guide passed. If manual validation fails, the issue returns to `In Progress`, is fixed/reverified, and then returns to `In Review`.

### The workflow improves over time — with human approval

`implement-issue` is intentionally dynamic. During normal use, Codex, Antigravity or Claude Code may notice a recurring project pattern, a missing/weak validation, a workflow step that causes repeated failures or unnecessary work, a security/performance gap, an unclear instruction or another reusable improvement.

The agent may proactively **propose** that improvement, but it may **never apply a workflow change automatically**. Before any change, it must show:

1. what it observed and the evidence;
2. the exact proposed change and why the current rule is insufficient;
3. the expected improvement;
4. risks/trade-offs and files affected.

Then it waits for explicit human approval (`Approve`, `Reject` or `Modify`). Only after approval may it update the canonical workflow and synchronize affected docs/adapters. Normal issue execution should continue autonomously; improvement proposals are preferably surfaced at the end unless the workflow defect makes safe continuation impossible.

Git history is the audit trail, so the project does not add a separate self-learning subsystem by default.

### Codex

Codex discovers repository skills from `.agents/skills/`. From the repository root:

```text
$implement-issue POR-9
```

You can also open `/skills`, select `implement-issue`, and provide the issue key. Restart Codex if a newly added skill is not yet visible.

### Antigravity

Antigravity discovers the same canonical skill from `.agents/skills/`. Invoke it directly as a slash skill:

```text
/implement-issue POR-9
```

You can also open `/skills` and select `implement-issue`. This avoids maintaining a second Antigravity workflow file with duplicated orchestration logic.

### Claude Code

Claude Code receives a thin project command adapter:

```text
/implement-issue POR-9
```

The command delegates to the same canonical `.agents/skills/implement-issue/SKILL.md`; workflow logic is not duplicated in the Claude adapter.

### What the workflow validates

Before it can move an issue to `In Review`, it runs every applicable class of verification, including:

- unit and regression tests;
- integration/Testcontainers/PostgreSQL tests;
- auth/RBAC/tenant/security tests;
- API/OpenAPI/generated-client and migration tests;
- frontend behavior and critical Playwright E2E tests;
- compile, lint, typecheck, production builds, containers and dependency/security checks;
- focused performance/load validation when the issue affects a performance-sensitive path.

It attempts to cover all changed meaningful code paths. Critical business/security logic should have complete behavioral/branch coverage where practical. Any meaningful changed path intentionally left uncovered must be identified and justified in the final report.

Progress and final output follow a concise state-first format: current issue/step, completed checks, current action or blocker, and a final `IN REVIEW`, `NOT READY` or `BLOCKED` status.

### Human acceptance: `In Review` → `Done`

After the agent reports `IN REVIEW`, follow the manual validation steps it provides. Each step includes the expected observable result. When every step passes, confirm explicitly, for example:

```text
POR-9 validada — tudo OK.
```

With Linear access, the agent may then move that issue from `In Review` to `Done`. This confirmation authorizes the tracker transition only; merge and production deployment remain separate actions. If a step fails, report the step number and observed behavior; the issue returns to `In Progress` for correction and must pass the automated gates again before another `In Review` handoff.

## Documentation

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

## CI/CD and security baseline

GitHub Actions is the independent verification boundary for human- or AI-produced changes.

Current checks that are already meaningful in the foundation repository:

- **Gitleaks 8.30.1** — scans Git history for committed secrets and redacts values in logs;
- **Semgrep CE 1.178.0** — runs the project guardrails in `.semgrep.yml`;
- **Trivy** — scans the repository for actionable HIGH/CRITICAL dependency and configuration findings;
- **Dependabot** — checks GitHub Actions dependencies weekly.

Java/Next.js build checks, CodeQL and SonarQube Cloud are intentionally added with Phase 1/POR-6, when `apps/api` and `apps/web` actually exist. The project does not create placeholder jobs that report green without testing application code.

### Run the current security baseline locally

The simplest path uses Docker. Run these commands from the repository root:

```bash
# 1. Secrets
docker run --rm -v "$PWD:/repo" -w /repo zricethezav/gitleaks:v8.30.1 git --redact --verbose .

# 2. Project static guardrails
docker run --rm -v "$PWD:/src" semgrep/semgrep:1.178.0 semgrep scan --config /src/.semgrep.yml --error --metrics=off /src

# 3. Dependency/configuration vulnerabilities
docker run --rm -v "$PWD:/repo" -w /repo aquasec/trivy:0.70.0 fs --scanners vuln,misconfig --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 .
```

Expected result: each command exits with status `0` and reports no blocking finding.

If your Docker installation does not expand `$PWD`, replace it with the absolute path to the cloned repository. GitHub Actions remains the authoritative merge-time check.

### Open-source onboarding invariant

The final root README must be sufficient by itself for a new user on a clean machine to go from clone to a fully configured, functioning SaaS-Skeleton:

```text
clone
  -> install prerequisites
  -> configure .env
  -> start local services
  -> configure required GitHub/external tools
  -> run tests/security checks
  -> verify CI
  -> fully working project
```

Whenever a tool/account/secret/command changes, this README must be updated in the same change with exact setup steps, expected results and common fixes. The architecture and rollout policy are defined in [CI/CD and DevSecOps](docs/CI-CD-SECURITY.md).

## Local setup

The target onboarding flow is:

```bash
git clone https://github.com/portoduque/SaaS-Skeleton.git
cd SaaS-Skeleton
cp .env.example .env
docker compose up
```

This flow becomes active as implementation phases add runtime services. Until then, the repository contains the specification and implementation plan.

## License

Apache License 2.0. See [LICENSE](LICENSE).
