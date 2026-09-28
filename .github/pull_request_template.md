## What changed

<!-- Describe the smallest coherent change and why it is needed. -->

## Validation

- [ ] Backend build + applicable unit/regression/integration/Testcontainers tests pass
- [ ] Changed meaningful behavior is covered as fully as practical; uncovered paths are justified
- [ ] Frontend lint/typecheck/behavior tests/build pass when affected
- [ ] Flyway migration validated when affected
- [ ] OpenAPI/generated client synchronized when affected
- [ ] Critical Playwright flow passes when affected
- [ ] Docker/container validation passes when affected
- [ ] Applicable GitHub Actions checks are green

## Security and architecture

- [ ] Authorization/tenant isolation reviewed when affected
- [ ] No secrets or sensitive data were added to code/logs
- [ ] Gitleaks/CodeQL/Semgrep/Trivy/Sonar findings reviewed when the corresponding check applies
- [ ] No unnecessary dependency, infrastructure or abstraction was introduced
- [ ] Backend remains API-first and frontend-agnostic
- [ ] Change does not block future horizontal scaling

## Documentation

- [ ] README updated when setup, commands, config, API usage or developer workflow changed
- [ ] `.env.example` updated when configuration changed
- [ ] Relevant docs updated when architecture/security/testing behavior changed
- [ ] Tool/account/secret/setup changes are documented step-by-step in the root README

> Code updated + README stale = incomplete PR.
>
> Tooling changed + onboarding stale = incomplete PR.

## Human acceptance

- [ ] Issue moved to `In Review` after automated gates passed
- [ ] Issue-specific manual validation guide provided to the human reviewer
- [ ] `Done` will only be set after explicit human confirmation that manual validation passed
