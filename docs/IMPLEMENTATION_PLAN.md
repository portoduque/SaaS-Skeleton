# Implementation Plan

## Delivery rule

Build the starter incrementally. Each phase has a narrow goal and must leave the repository in a valid state before the next phase begins.

Do not implement later phases early unless a small prerequisite is unavoidable.

## Global engineering gate

Every implementation phase follows:

```text
UNDERSTAND -> RESEARCH -> PLAN -> TEST -> IMPLEMENT -> REVIEW -> VERIFY -> DOCUMENT -> IN REVIEW -> HUMAN ACCEPTANCE
```

Before a phase can be completed, all applicable checks must pass:

- backend build/tests/Testcontainers;
- frontend lint/typecheck/tests/build;
- Flyway validation when affected;
- OpenAPI/generated client synchronization when affected;
- Playwright critical flow when affected;
- security review for auth/RBAC/tenant/input/secrets/dependency changes;
- diff review for accidental/dead/unrelated changes;
- README, `.env.example` and affected docs synchronization.

Review findings must be evidence-based. Performance changes must be backed by measurement or a documented bottleneck. Do not add infrastructure or abstractions merely because a pattern is common elsewhere.

**Code updated + README stale = phase incomplete.**

## Phase 0 — Repository foundation

**Goal:** establish the open-source project contract before application code.

Deliverables:

- README;
- AGENTS rules;
- PRD, architecture, scope, testing and security documents;
- Apache-2.0 license;
- CONTRIBUTING and SECURITY policies;
- `.env.example` placeholder;
- basic `.gitignore`;
- changelog;
- `docs/ENGINEERING-WORKFLOW.md`;
- canonical `.agents/skills/implement-issue/` workflow and focused references;
- thin Claude Code `/implement-issue` adapter;
- pull request template;
- documented human-governed workflow evolution;
- documented `In Review` -> manual validation -> `Done` acceptance gate.

Gate:

- documentation is internally consistent;
- Codex, Antigravity and Claude Code usage is documented;
- one canonical workflow is shared without duplicating orchestration logic;
- successful agent implementation ends in `In Review`, with `Done` requiring explicit human validation;
- a new contributor can understand intended stack, scope and workflow.

## Phase 1 — Monorepo and developer bootstrap

**Goal:** create minimal web/API project shells and one documented developer command path.

Deliverables:

- `apps/api` Spring Boot project using Java 25 and Maven Wrapper;
- `apps/web` Next.js/TypeScript strict project;
- root scripts or documented commands;
- initial formatting/lint configuration;
- Dockerfiles where required.

Tests/gate:

- backend compiles and baseline test passes;
- frontend typecheck/lint/build passes;
- README setup commands match reality.

## Phase 2 — PostgreSQL, PgBouncer and Flyway

**Goal:** establish production-like data access locally.

Deliverables:

- PostgreSQL 18 service;
- PgBouncer service;
- application connection through PgBouncer;
- Flyway configured;
- initial migration;
- Testcontainers PostgreSQL setup.

Tests/gate:

- clean database starts from migrations;
- backend integration test connects successfully;
- data survives container recreation through documented volume behavior;
- database/PgBouncer are not exposed publicly by default.

## Phase 3 — API conventions and observability baseline

**Goal:** establish API and operational conventions before business modules.

Deliverables:

- `/api/v1` convention;
- DTO validation;
- standard error response;
- request/correlation ID;
- structured logging baseline;
- Spring Boot health endpoint;
- OpenAPI generation;
- resource-oriented URL/status/error/pagination conventions.

Gate:

- contract is generated;
- error/validation integration tests pass;
- health check works through local stack.

## Phase 4 — Users and authentication core

**Goal:** secure user lifecycle for web without coupling domain logic to Next.js.

Deliverables:

- user persistence;
- UUIDv7 identifiers;
- Argon2id password hashing;
- registration;
- login/logout;
- Spring Session JDBC;
- e-mail verification token model;
- password reset token model;
- abuse-sensitive endpoint rate-limiting baseline.

TDD focus:

- password handling;
- duplicate identity rules;
- token expiration/single use;
- authentication failures;
- session lifecycle.

Gate:

- security tests pass;
- sensitive data is absent from logs/API responses.

## Phase 5 — Organizations and memberships

**Goal:** introduce the reusable multi-tenant core.

Deliverables:

- organizations;
- organization memberships;
- roles `OWNER`, `ADMIN`, `MEMBER`;
- create/select organization;
- membership authorization rules.

TDD focus:

- ownership rules;
- role restrictions;
- user belonging to multiple organizations.

Gate:

- organization tests pass;
- data model supports future tenant-owned modules through `organization_id`.

## Phase 6 — Tenant isolation hardening

**Goal:** make cross-tenant leakage a tested invariant.

Deliverables:

- tenant-aware access convention for organization-owned resources;
- explicit negative tests for cross-tenant reads/writes;
- review of repository/service APIs for accidental unscoped access.

Gate:

- tenant A cannot read or mutate tenant B resources through supported API paths;
- tests fail if tenant scoping is deliberately removed.

## Phase 7 — Generated TypeScript API client

**Goal:** connect clients to backend through a typed contract rather than duplicated manual types.

Deliverables:

- OpenAPI artifact generation;
- generated TypeScript client package/workflow;
- frontend consumes generated client;
- documented regeneration command.

Gate:

- backend contract change can be reflected deterministically in client generation;
- frontend build detects incompatible contract usage.

## Phase 8 — Frontend shell and design system

**Goal:** provide a polished but neutral SaaS UI foundation.

Deliverables:

- Tailwind;
- shadcn/ui;
- typography/color/spacing/radius tokens;
- responsive application shell;
- navigation/sidebar/header patterns;
- dark/light mode;
- loading/error/empty state patterns;
- accessibility baseline.

Avoid product-specific dashboard widgets.

Gate:

- responsive layouts work on mobile/tablet/desktop;
- typecheck/lint/build pass;
- design primitives are reused rather than duplicated.

## Phase 9 — Auth and organization web flows

**Goal:** expose the core starter capabilities through the initial web client.

Deliverables:

- registration/login/logout UI;
- verification/reset flows;
- profile basics;
- create/select organization;
- members/roles UI required by the starter.

Gate:

- frontend uses API only;
- no business authorization exists solely in frontend;
- critical Playwright flows pass.

## Phase 10 — CI and open-source contribution workflow

**Goal:** prevent invalid agent/human changes from merging.

Deliverables:

- GitHub Actions backend build/test;
- frontend lint/typecheck/build;
- integration tests;
- critical Playwright smoke tests when practical in CI;
- container build validation;
- dependency/security automation supported by GitHub;
- pull request template with verification/security/documentation checklist;
- evidence-based diff review expectations.

Gate:

- intentionally broken backend/frontend checks block the pipeline.

## Phase 11 — Production reference deployment

**Goal:** document a simple one-VM reference deployment without coupling the software to it.

Deliverables:

- Caddy HTTPS/reverse proxy configuration;
- production Compose configuration/pattern;
- internal-only service networking;
- health/restart configuration;
- resource/configuration guidance;
- environment variable documentation.

Gate:

- reference deployment starts from a clean host following README/docs;
- only intended public ports are exposed.

## Phase 12 — Backup, restore and final hardening

**Goal:** complete the reusable production baseline.

Deliverables:

- automated external PostgreSQL backup example/reference;
- documented restore procedure;
- restore validation in a disposable environment;
- security/configuration review;
- dependency review;
- JPA/PostgreSQL performance review using real query/runtime evidence where available;
- repository cleanup;
- README end-to-end verification from clone to running system.

Gate:

- a fresh user can follow the README successfully;
- backup can be restored using the documented procedure;
- no obsolete/dead setup files remain;
- all required CI gates are green.

## After the starter

Product-specific SaaS features begin only after the starter baseline is stable.

Examples of future opt-in capabilities:

- billing;
- Redis/cache;
- background jobs;
- object storage;
- webhooks;
- notifications;
- mobile/desktop clients;
- horizontal database/read scaling.

Each is added only when a concrete product requirement justifies it.
