# Security Policy

## Reporting a vulnerability

Please do not open a public issue containing exploit details, credentials, tokens, personal data or other sensitive vulnerability information.

Until a private security reporting channel is configured for the repository, contact the repository maintainer privately through the GitHub account associated with this project and provide:

- affected component/version or commit;
- concise reproduction steps;
- expected impact;
- suggested mitigation if known.

The project will add/clarify GitHub private vulnerability reporting as repository settings are finalized.

## Supported versions

Before the first stable release, security fixes target the current `main` branch and latest published pre-release/release.

After stable releases begin, this document will list supported release lines explicitly.

## Security principles

The project treats authentication, authorization, tenant isolation, secrets handling, dependency hygiene and database integrity as core architecture concerns. See `docs/SECURITY-ARCHITECTURE.md`.

## Development security rules

Engineering security requirements are defined in `docs/SECURITY-ARCHITECTURE.md` and enforced through `AGENTS.md`. Changes involving authentication, authorization, tenant data, secrets, raw SQL, sensitive endpoints or new dependencies require explicit security review before completion.
