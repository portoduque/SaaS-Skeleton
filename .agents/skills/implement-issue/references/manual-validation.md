# Implement Issue — Manual Validation Handoff

This reference defines the mandatory human-acceptance handoff after automated implementation is complete.

## Core rule

Automated verification and human acceptance are separate gates.

```text
IMPLEMENT
  -> AUTOMATED VERIFY
  -> IN REVIEW
  -> HUMAN MANUAL VALIDATION
      -> fail: IN PROGRESS -> fix -> verify -> IN REVIEW
      -> pass: explicit human confirmation -> DONE
```

`implement-issue` may move an issue to **In Review** after all automated gates pass. It must never move that issue to **Done** in the same implementation run.

`Done` requires explicit human confirmation that the manual validation guide was completed successfully.

Human confirmation authorizes the Linear status transition only. It does **not** authorize merge, production deployment, destructive actions or any other separate privileged operation.

## Generate a guide specific to the issue

The final In Review response must include a complete but simple manual validation guide derived from:

- the issue objective and acceptance criteria;
- the implementation diff;
- the actual runtime/setup commands documented by the repository;
- user-visible or operator-visible behavior introduced by the change;
- important failure/error paths that a human can meaningfully verify.

Do not emit a generic checklist copied across every issue.

Every manually observable acceptance criterion should map to at least one validation step. If a criterion is not sensible to validate manually because it is already a purely internal automated invariant, state that it is covered automatically instead of inventing a manual ritual.

## Guide design rules

1. **Start with prerequisites.** State exactly what must be running, what account/data is needed and which command/page to open.
2. **Use numbered bounded actions.** One clear action per step where practical.
3. **Show the expected result immediately after each action.** The user should never need to infer what success looks like.
4. **Prefer user-visible behavior.** Do not make the human repeat unit/integration tests that the agent already ran.
5. **Include the critical happy path and meaningful negative/error behavior.** Keep it proportional to the issue.
6. **For UI changes, include responsive and important interaction/state checks.** Include accessibility checks a human can realistically observe when relevant.
7. **For auth/security flows, validate visible access/session behavior without exposing secrets.** Automated security invariants remain mandatory in addition to the manual check.
8. **For infrastructure/database work, validate the operator workflow that matters:** startup, persistence, restore, exposure, health, migration behavior, etc. Do not force GUI steps when CLI verification is the real user workflow.
9. **Use commands/URLs that actually exist in the repository.** Never invent setup commands.
10. **Keep the path linear.** Avoid tangents and optional explorations inside the main acceptance path.

## Required final structure

Use this general shape and adapt it to the issue:

```text
POR-9 — IN REVIEW

Automated validation
✓ Acceptance criteria implemented
✓ Applicable tests/build/security gates passing
✓ README/docs synchronized
✓ PR: #24

Manual validation

Prerequisite
1. <exact startup/setup action>
   Expected: <observable result>

1. <human action>
   Expected: <observable success>

2. <human action>
   Expected: <observable success>

3. <negative/error-path action when relevant>
   Expected: <observable safe behavior>

If every step passes, reply:
"POR-9 validada — tudo OK."

If something fails, reply:
"POR-9 falhou no passo <N>: <what happened>."
```

The exact wording may vary naturally, but the guide must preserve: prerequisites, numbered actions, expected results and an explicit confirmation/failure response.

## Linear state transitions

When tracker access is available:

### Automated implementation completes successfully

- attach/link the PR when available;
- move the issue from `In Progress` to **`In Review`**;
- provide the manual validation guide;
- stop. Do not mark `Done`.

### Human reports a failure

If the human reports that a manual validation step failed:

1. move the issue back to **`In Progress`** when Linear access is available;
2. reproduce the failure with automated coverage when practical;
3. apply the smallest correct fix;
4. rerun all affected regression/integration/security/build gates;
5. move the issue back to **`In Review`**;
6. provide a new manual validation guide.

The new guide may focus on the corrected path plus affected regressions only when that is sufficient to preserve confidence. If the fix can affect the broader flow, repeat the full relevant guide.

### Human explicitly confirms success

Only after explicit human confirmation that the guide passed:

1. optionally record a concise tracker comment stating that human acceptance was confirmed, if the tracker supports it;
2. move the issue from **`In Review`** to **`Done`**;
3. report the status change concisely.

Examples of sufficient confirmation:

- `POR-9 validada — tudo OK.`
- `Testei todos os passos, pode marcar como Done.`
- `Tudo certo, validação manual passou.`

Do not infer acceptance from silence, a vague acknowledgment, or approval of only one step.

## Missing `In Review` status

`In Review` is part of the SaaS-Skeleton operating model. If the Linear team does not expose that status, do not silently substitute `In Progress` or `Done`.

- complete the code/automated validation normally;
- keep the issue in `In Progress`;
- report that the tracker handoff is blocked because `In Review` is missing;
- tell the human to add/enable the `In Review` status once;
- after it exists, move the issue to `In Review` and deliver the normal validation handoff.

This tracker-configuration gap does not justify weakening the human-acceptance gate.
