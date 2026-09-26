# Contributing

Thank you for contributing to SaaS-Skeleton.

The project values small, testable, evidence-based changes and clear documentation over speculative rewrites.

## Before contributing

Read:

- `README.md`;
- `AGENTS.md`;
- `docs/ARCHITECTURE.md`;
- `docs/SCOPE.md`;
- `docs/ENGINEERING-WORKFLOW.md`;
- `docs/TESTING.md`;
- `docs/SECURITY-ARCHITECTURE.md`.

## Principles

Contributions must preserve:

- frontend independence;
- future horizontal scalability;
- security and tenant isolation;
- performance awareness based on measurement;
- modularity;
- simplicity/no overengineering.

Do not introduce large infrastructure, agent frameworks or architectural patterns unless the change solves a documented requirement.

## Development process

Use the project workflow:

```text
UNDERSTAND -> RESEARCH -> PLAN -> TEST -> IMPLEMENT -> REVIEW -> VERIFY -> DOCUMENT -> DONE
```

1. Create a focused branch.
2. Understand the issue and keep the scope narrow.
3. Check existing code and official docs before inventing a new pattern/dependency.
4. Add/update tests when behavior changes.
5. Implement the smallest coherent change.
6. Review the full diff with evidence-based findings.
7. Run all applicable verification gates.
8. Update README/docs/config examples when affected.
9. Open a focused pull request describing the reason and validation performed.

## Review expectations

High/critical findings should identify the exact location, concrete failure scenario, impact and recommended fix. Avoid speculative or stylistic review noise.

Security-sensitive changes require explicit review of authentication/authorization, tenant boundaries, input validation, secrets and relevant browser/API protections.

Performance recommendations should include evidence of a bottleneck rather than speculative infrastructure.

## Database changes

All schema changes use Flyway migrations.

Do not edit already-applied versioned migrations to change production history. Add a new forward migration.

Keep migrations compatible with safe staged deployments when a schema/data transition is risky.

## Pull request completion

A PR is not ready when applicable builds/tests fail, security/contract checks are unresolved or documentation is stale.

**Code updated + README stale = incomplete PR.**

Keep PRs small enough to review and revert safely.
