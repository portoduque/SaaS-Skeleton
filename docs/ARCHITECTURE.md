# Architecture

## Decision summary

SaaS-Skeleton uses a **modular monolith, API-first, frontend-agnostic and stateless-ready architecture**.

The goal is not maximum infrastructure on day one. The goal is to avoid architectural dead ends while keeping the initial system easy to understand and operate.

## Initial topology

```text
Internet
   |
 Caddy
   |
   +--> Next.js web
   |
   +--> Spring Boot API
            |
        PgBouncer
            |
        PostgreSQL
```

All components may initially run in Docker Compose on one VM.

Only Caddy is intended to be publicly exposed. PostgreSQL, PgBouncer and the Spring Boot service remain on the internal network.

## Future topology

The same application core should support:

```text
CDN / Load Balancer
        |
   +----+----+
   |         |
 Web x N   API x N
             |
         PgBouncer
             |
         PostgreSQL
```

Database replicas, cache, workers or specialized infrastructure are added only when measured requirements justify them.

## Backend

Technology:

- Java 25 LTS;
- Spring Boot 4.1.x;
- Spring Security;
- Spring Data JPA/Hibernate;
- Maven Wrapper;
- Flyway;
- OpenAPI.

### Organization

Code is organized by feature:

```text
com.saas.skeleton/
├── auth/
├── users/
├── organizations/
├── memberships/
├── security/
└── shared/
```

Keep the internal feature structure simple. Default flow:

```text
Controller -> Service -> Repository -> Database
```

Do not create domain/application/ports/adapters layers for every feature by default. Add complexity only when a concrete module benefits from it.

### Rules

- Controllers handle HTTP concerns, not business rules.
- Services own business behavior and transaction boundaries.
- Repositories own persistence access.
- JPA entities are not the public API contract.
- API DTOs are explicit.
- Hibernate schema auto-update is not used as the production migration mechanism.
- Open Session in View should be disabled.

## Frontend independence

The backend exposes REST/JSON under `/api/v1` and an OpenAPI contract.

```text
Next.js --------+
React Native ---+
Desktop --------+--> REST/OpenAPI --> Spring Boot
Integrations ---+
```

No critical business rule may exist only in the frontend.

Next.js must never access PostgreSQL directly.

## API contract

OpenAPI is the canonical machine-readable API contract.

A TypeScript client is generated from the contract and consumed by the web application. Future mobile/desktop TypeScript clients can reuse the same generated package.

## Authentication

Initial web authentication uses secure browser cookies backed by Spring Security and server-side session storage using Spring Session JDBC.

This avoids storing sensitive bearer tokens in browser storage and permits horizontal scaling because sessions are not tied to one application process.

The authentication domain must not be coupled to browser-only behavior. OAuth2/OIDC token flows for future mobile/desktop clients can be introduced later without changing core users/organizations/business modules.

## Multi-tenancy

Initial model is shared database/shared schema.

Core entities:

```text
users
organizations
organization_memberships
```

Membership binds a user to an organization and role.

Future tenant-owned tables include `organization_id` and are always queried through authorized tenant context.

The client-provided organization identifier is not sufficient authorization.

## Identifiers

Use UUIDv7 for public/domain entity identifiers where practical. This avoids predictable sequential public identifiers while preserving better index locality than fully random UUIDv4.

## Database

- PostgreSQL 18;
- Flyway for all schema evolution;
- PgBouncer between application and PostgreSQL;
- constraints and foreign keys enforce integrity;
- indexes are added from query requirements, not speculation;
- PostgreSQL preloads `pg_stat_statements`, and Flyway enables the extension in each migrated database.
- PostgreSQL is not published to the host; the local PgBouncer port binds only to `127.0.0.1`.
- The application connects through PgBouncer, including migration startup.


## API conventions

The API is resource-oriented and consistent across clients.

- use nouns in resource paths;
- use HTTP status codes semantically;
- keep one stable structured error format;
- use consistent pagination/filtering conventions;
- return only fields needed by the contract;
- treat OpenAPI as the source for generated client types.

Do not add frontend-specific endpoints that embed Next.js assumptions into backend domain logic.

## Persistence and JPA performance rules

JPA is used as a persistence tool, not as a reason to hide database behavior.

Defaults:

- relationships are lazy unless a measured path requires otherwise;
- avoid `EAGER` collections;
- watch for N+1 query patterns;
- use projections/fetch joins for deliberate read paths;
- keep transactions short;
- paginate potentially large collections;
- avoid unbounded `findAll`-style application paths;
- add indexes from real filters/order/join patterns;
- inspect query plans and `pg_stat_statements` before speculative tuning.

Do not copy connection-pool numbers from examples blindly. Hikari/application concurrency, PgBouncer and PostgreSQL limits must be tuned together using measured load.

## Performance strategy

Performance work follows: measure -> identify bottleneck -> change -> compare.

Useful evidence may include:

- PostgreSQL `EXPLAIN`;
- `pg_stat_statements`;
- application timings/metrics;
- browser/runtime profiling;
- bundle analysis;
- concurrency/load tests when justified.

Do not add Redis, read replicas, queues, caching layers or client memoization merely in anticipation of scale.

## Container/runtime hardening

Application containers should:

- use explicit supported image versions;
- use multi-stage builds where they reduce runtime footprint;
- run as non-root;
- keep final images minimal;
- expose only intended ports;
- define meaningful health checks;
- receive secrets at runtime rather than embedding them in images.

## Infrastructure boundaries

Start with the minimum required components.

Do not add Redis, queues or object storage until a concrete feature needs them. When added, product modules should depend on a small application-facing contract rather than provider-specific details where substitution is reasonably expected.

## Architectural quality rule

Every new architectural component must answer both questions:

1. What concrete current problem does it solve?
2. Does it preserve or improve security, performance, frontend independence or future scalability?

If the first answer is unclear, do not add it.
