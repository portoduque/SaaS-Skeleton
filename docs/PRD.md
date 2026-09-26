# Product Requirements Document — SaaS-Skeleton

## 1. Product

SaaS-Skeleton is an open-source starter repository for building secure, performant and maintainable SaaS products without rebuilding common foundations for every project.

It is a **starter**, not a finished SaaS product.

## 2. Problem

New SaaS projects repeatedly spend time setting up the same cross-cutting concerns: authentication, users, tenant boundaries, database migrations, API contracts, testing, local infrastructure and deployment basics.

Existing starters often fall into one of two extremes:

- too minimal to be a safe production foundation; or
- too complex, with infrastructure and patterns that most early products do not need.

SaaS-Skeleton aims for the middle: **production-conscious fundamentals without overengineering**.

## 3. Primary user

Developers and coding agents who want to clone a repository, run it locally and add product-specific modules on top of a stable SaaS foundation.

## 4. Product principles

- Secure by default.
- Frontend-agnostic and API-first.
- Easy horizontal scalability in the future.
- High performance without premature optimization.
- Modular and easy to navigate.
- Simple local setup.
- Documentation is part of the product.
- Coding-agent work must follow evidence-based review and verification gates.
- Open source and commercially reusable under Apache-2.0.

## 5. MVP foundation

The starter must provide:

- users;
- organizations;
- organization memberships;
- roles `OWNER`, `ADMIN`, `MEMBER`;
- authentication and logout;
- e-mail verification;
- password recovery/reset;
- tenant isolation enforced in the backend;
- REST API under `/api/v1`;
- OpenAPI documentation/contract;
- generated TypeScript API client;
- PostgreSQL with Flyway migrations;
- PgBouncer connection pooling;
- responsive Next.js application shell;
- basic design system;
- health checks and structured application logging;
- automated backend and critical E2E tests;
- Docker Compose local environment;
- CI validation;
- external backup/restore documentation;
- a documented engineering workflow for coding agents: research, pragmatic TDD, evidence-based review and verification.

## 6. Architecture requirements

- Spring Boot backend must not depend on any frontend implementation.
- Business rules and authorization must live in the backend.
- Backend must be stateless-ready so multiple instances can run later.
- Start as a modular monolith organized by feature.
- PostgreSQL is the system of record.
- Schema evolution must be versioned with Flyway.
- The first deployment target is one VM, but the design must permit separating and scaling components later.

## 7. Multi-tenancy

Initial model:

- shared application;
- shared PostgreSQL database/schema;
- tenant represented by `organization`;
- membership represented by `organization_membership`;
- product data belonging to a tenant carries `organization_id`.

A user may belong to multiple organizations.

Tenant context received from a client must never be trusted as authorization. The backend validates membership and permission before access.

PostgreSQL Row-Level Security is not required in the initial implementation. It may be evaluated later if the concrete product needs an additional database-level isolation layer.

## 8. Clients

Initial client:

- Next.js web application.

Future clients must be possible without replacing the backend:

- React Native / Expo;
- Android/iOS;
- desktop client such as Tauri;
- external integrations.

## 9. Non-goals for the starter

Not included unless a future requirement justifies them:

- billing/Stripe;
- Redis;
- queues;
- microservices;
- Kubernetes;
- Kafka/RabbitMQ;
- Elasticsearch/ClickHouse;
- product-specific workflows;
- complex dynamic permission engines.

## 10. Success criteria

The starter is considered usable when a new developer can:

1. clone the repository;
2. copy `.env.example`;
3. start the complete local stack using documented commands;
4. create an account and authenticate;
5. create/use an organization with correct role enforcement;
6. verify tenant A cannot access tenant B data;
7. run migrations and tests successfully;
8. understand the architecture and contribution flow through repository documentation;
9. build a new product-specific module without changing the core architecture;
10. complete an issue only after applicable build, test, security, contract and documentation gates pass.
