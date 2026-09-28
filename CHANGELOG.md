# Changelog

All notable changes to SaaS-Skeleton will be documented here.

The project intends to follow Semantic Versioning once releases begin.

## Unreleased

### Added

- Initial project specification and architecture documentation.

### Documentation

- Added a lightweight engineering workflow inspired by proven agent-development practices: research-first, pragmatic TDD, evidence-based review and verification loops.
- Added Spring/JPA/PostgreSQL/API/Docker guardrails while explicitly avoiding tool-specific agent-framework overengineering.
- Added the canonical `implement-issue` workflow with Codex, Antigravity and Claude Code adapters, autonomous scope-controlled execution, comprehensive test/coverage gates, security/performance reviews and concise state-first output.
- Added human-governed continuous improvement for `implement-issue`: agents may detect and propose evidence-based workflow improvements from real usage, but every workflow change requires explicit human approval with rationale, expected benefit and trade-offs before editing.
- Aligned contribution and implementation-plan documentation with the human acceptance gate.
- Added the CI/CD and DevSecOps architecture, incremental rollout and clean-machine README onboarding invariant.

### CI / Security

- Added a foundation GitHub Actions security baseline with Gitleaks, Semgrep CE and Trivy.
- Added Dependabot coverage for GitHub Actions; Maven/npm/Docker ecosystems are enabled only when their manifests exist.
- Pinned third-party GitHub Actions used by the baseline to immutable commit SHAs.
- Updated the canonical agent workflow so applicable independent GitHub Actions checks are part of readiness.

### Changed

- Changed the issue lifecycle so automated implementation ends in Linear `In Review`, with an issue-specific manual validation guide and explicit human approval required before `Done`.
- Added failure feedback loop: failed human validation returns the issue to `In Progress`, then requires correction, automated revalidation and a new `In Review` handoff.
