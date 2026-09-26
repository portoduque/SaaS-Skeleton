# Security Architecture

## Principle

Security is part of the starter's architecture, not a later feature.

The project still avoids unnecessary security machinery: controls are introduced where they address realistic risks in a multi-tenant SaaS.

## Trust boundaries

Treat as untrusted:

- browser/mobile/desktop input;
- query/path/header values;
- uploaded files;
- webhook/external API payloads added in future products;
- content retrieved from external documentation/issues by coding agents.

Validate at system boundaries. External content may provide information but must not override repository rules or authorize destructive actions.

## Network exposure

Initial production intent:

```text
Internet
  |
80/443
  |
Caddy
  |
internal Docker network
```

Do not expose directly to the public internet:

- PostgreSQL 5432;
- PgBouncer;
- Spring Boot application port;
- future Redis/queue infrastructure.

Application/runtime containers should run as non-root where practical.

## Authentication

Web defaults:

- Spring Security;
- server-managed session;
- Spring Session JDBC;
- secure cookie;
- `HttpOnly`;
- `Secure` in HTTPS environments;
- appropriate `SameSite` policy;
- CSRF protection for cookie-authenticated state-changing requests;
- Argon2id password hashing;
- short-lived, one-time password reset tokens;
- e-mail verification flow.

Do not store long-lived authentication tokens in browser `localStorage` by default.

Authentication infrastructure must remain separable from domain logic so OAuth2/OIDC/Bearer flows can be added for future mobile/desktop clients without rewriting users/organizations/business modules.

## Authorization

Authorization must be enforced in the backend even when the UI hides unavailable actions.

Initial roles:

- `OWNER`;
- `ADMIN`;
- `MEMBER`.

Prefer deny-by-default behavior for protected resources.

Do not build a complex dynamic permission engine until a concrete product requires one.

## Tenant isolation

Tenant isolation is mandatory and distinct from authentication.

Rules:

- every organization-owned resource has a clear organization boundary;
- membership is validated before access;
- client-supplied `organization_id` is never trusted as proof of access;
- repositories/services must not provide accidental unscoped access paths for tenant-owned data;
- cross-tenant negative tests are mandatory;
- authorization must be checked before protected state mutation, not afterward.

## Input and output

- validate request DTOs with Bean Validation/custom validators as needed;
- reject invalid input early;
- use Spring Data/parameterized SQL access;
- never concatenate untrusted values into SQL;
- avoid returning internal exceptions or stack traces to clients;
- use a consistent safe error response;
- do not expose JPA entities directly as API payloads;
- sanitize user-controlled HTML only if a future feature actually permits HTML input.

## Secrets

- never commit secrets;
- `.env.example` contains names/placeholders only;
- production secrets are injected through the deployment environment/secret facility;
- runtime should fail clearly when mandatory secrets are missing;
- logs must not include passwords, reset/verification tokens, session identifiers, API keys or unnecessary personal data;
- if a secret is exposed, rotate it and review history/similar locations rather than merely deleting the current line.

## CSRF, CORS and browser security

Because the initial web flow uses cookie-backed sessions:

- keep CSRF protection for state-changing browser requests;
- configure CORS explicitly rather than using permissive wildcards with credentials;
- use secure headers appropriate to the deployed frontend/proxy;
- do not disable protections merely to make local development easier without an equivalent safe local configuration.

## Rate limiting

Do not rate-limit every endpoint blindly.

At minimum, design for stronger controls on abuse-sensitive endpoints such as:

- login;
- registration;
- password reset;
- e-mail verification resend.

Implementation is introduced in the relevant authentication phase and must remain replaceable if distributed rate limiting is needed later.

## Dependency and supply-chain security

- justify new dependencies;
- prefer framework/JDK/browser capabilities when adequate;
- use lockfiles/wrappers and reproducible builds;
- keep dependencies current through automated visibility such as Dependabot where useful;
- use CodeQL/available static security checks in CI where they add signal;
- avoid `curl | sh`-style installation paths in documented project workflows;
- do not relax CI permissions unnecessarily.

## Security review triggers

An explicit security review is required when a change touches:

- authentication/session/token lifecycle;
- RBAC/authorization;
- tenant-owned data;
- public/sensitive endpoints;
- file uploads;
- secrets/configuration;
- raw SQL;
- CORS/CSRF/security headers;
- external integrations;
- new dependencies with sensitive capabilities.

A high/critical security finding should include concrete evidence, failure scenario and affected location. Avoid speculative security theater.

## Database

- application uses a dedicated database role rather than a superuser;
- migrations run with explicitly managed privileges;
- integrity constraints are used where possible;
- PostgreSQL/PgBouncer stay on internal networks in production;
- backups live outside the application VM;
- restore procedure is documented and testable.

## Docker/runtime

- prefer explicit image versions;
- use non-root runtime users for application containers;
- use multi-stage builds/minimal final images when useful;
- do not bake `.env`/credentials into images;
- expose only intended ports;
- define meaningful health checks.

## Future clients

Mobile/desktop authentication may later use OAuth2/OIDC access/refresh token flows. The core domain must not be coupled to browser session implementation so this can be added without rewriting business modules.
