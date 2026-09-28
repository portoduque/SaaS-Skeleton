# Scope

## In scope for the starter

### Platform foundation

- monorepo containing web and API applications;
- Docker Compose local environment;
- Caddy entry point;
- PostgreSQL;
- PgBouncer;
- Flyway;
- configuration through environment variables;
- health checks;
- basic structured logging.

### Identity and SaaS core

- user registration;
- login/logout;
- e-mail verification;
- password reset;
- profile basics;
- organizations;
- memberships;
- roles: `OWNER`, `ADMIN`, `MEMBER`;
- organization switching;
- backend tenant isolation.

### API

- REST/JSON;
- `/api/v1` namespace;
- explicit DTOs;
- validation;
- consistent error format;
- OpenAPI contract;
- generated TypeScript client.

### Frontend

- Next.js/React/TypeScript strict;
- responsive application shell;
- Tailwind CSS;
- shadcn/ui;
- reusable design tokens/components;
- dark/light theme support;
- accessible forms and navigation;
- authentication and organization UI required to exercise the starter.

### Quality

- JUnit;
- Testcontainers with real PostgreSQL integration tests;
- Playwright for critical flows;
- CI build/test gates;
- dependency/security scanning where available;
- README and developer documentation.

### Open source

- Apache-2.0 license;
- contributing guide;
- security reporting policy;
- changelog;
- clear local setup.

## Explicitly out of scope initially

- product-specific domain modules;
- payments/billing;
- Redis;
- background queue platform;
- Kafka;
- RabbitMQ;
- Kubernetes;
- microservices;
- CQRS;
- event sourcing;
- Elasticsearch/OpenSearch;
- ClickHouse;
- read replicas;
- sharding;
- complex dynamic RBAC/ABAC;
- database-per-tenant;
- mobile application;
- desktop application;
- large tool-specific agent frameworks, hundreds of repository-local skills/hooks or duplicated configs for every AI coding tool.

These are not rejected permanently. They are intentionally deferred until a concrete SaaS demonstrates the need.

## Extension rule

Deferred items are added only when a concrete product requirement or measured operational need justifies them. The starter must remain usable without optional future infrastructure.
