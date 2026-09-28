# Implement Issue — Output Contract

User-visible output must make state obvious without narrating every tool call.

## Progress updates

Update at meaningful phase boundaries, after a concrete success, or when a real failure changes the plan.

Preferred shape:

```text
POR-9 — Step 3/7

✓ Migration validated
✓ Repository integration tests passing
→ Implementing session authentication

Blockers: none
```

Do not flood the user with low-level operational narration.

## Failure updates

State facts before hypotheses.

```text
POR-9 — Verification blocked

Observed:
- AuthIntegrationTest: expected 200, received 403

Evidence:
- Request reaches the CSRF filter without an accepted token

Working hypothesis:
- Login endpoint configuration does not match the intended browser-session CSRF flow

Next action:
- verify the intended CSRF contract, apply the smallest fix, rerun the focused test
```

Use `Root cause confirmed:` only when evidence actually confirms it.

No emotional filler such as “uh oh”, “unfortunately”, or “there seems to be a problem”.

## Out-of-scope findings

Do not implement tangents. Preserve useful observations briefly:

```text
Noticed — out of scope:
- `FooService` has unrelated duplication; no change made.
```

Do not turn this section into a backlog dump. Include only meaningful observations.

## Final IN REVIEW output

Keep the first screen useful.

```text
POR-9 — IN REVIEW

✓ Acceptance criteria implemented
✓ Unit/regression/integration tests passing
✓ Security/tenant checks passing
✓ Build and contract validation passing
✓ Applicable GitHub Actions checks passing
✓ README/docs synchronized
✓ Linear: In Review

Coverage:
- changed critical logic: covered
- uncovered meaningful changed paths: none

PR: #24

Manual validation

Prerequisite
1. <exact startup/setup action>
   Expected: <observable result>

1. <first user/operator action>
   Expected: <observable success>

2. <next action>
   Expected: <observable success>

3. <important negative/error path when relevant>
   Expected: <safe observable behavior>

If every step passes, reply:
"POR-9 validada — tudo OK."

If something fails, reply:
"POR-9 falhou no passo <N>: <what happened>."
```

The manual section is mandatory for successful completion and must be specific to the issue. Follow `manual-validation.md`; do not copy a generic checklist.

Then, only if useful, add compact evidence groups such as:

- key areas changed;
- exact validation commands/results;
- migration/API/env notes;
- out-of-scope observations.

## Final NOT READY output

```text
POR-9 — NOT READY

Passing:
- unit tests
- backend build

Failing:
- integration test `...`

Observed:
- ...

Next action:
- ...
```

Do not move the issue to `In Review` while a mandatory automated gate fails. This includes applicable independent GitHub Actions checks once they exist.

## Final BLOCKED output

```text
POR-9 — BLOCKED

Completed:
- ...

Blocker:
- exact missing credential/decision/context

Why it blocks safe progress:
- ...

Required next action:
- one concrete action
```

## Workflow improvement proposal

Only show this when the end-of-run retrospective found a validated, reusable improvement candidate. Do not manufacture one every run. Never apply it before the human approves it.

```text
WORKFLOW IMPROVEMENT PROPOSAL

Observed:
- <what happened>

Evidence:
- <concrete evidence from this run/repository>

Proposed change:
- <smallest specific change>

Why change it:
- <gap in the current workflow>

Expected improvement:
- <what becomes safer/faster/clearer/more reliable>

Trade-offs / risks:
- <downside or added cost>

Files that would change:
- <files>

Approval required:
- Approve / Reject / Modify
```

Do not mix unrelated improvement candidates into one approval request.

## Style rules

- answer/state first;
- concise headings;
- numbered steps only when the user must perform multiple actions;
- make completed work visible;
- group long lists rather than hiding important items;
- no generic preamble or closing pleasantries;
- no time estimates for autonomous agent work;
- successful runs end with a simple issue-specific manual validation path, not with "done";
- preserve uncertainty when evidence is incomplete;
- do not ask the user to do work the agent can perform with available tools.
