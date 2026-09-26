# Implement Issue — Continuous Improvement

The `implement-issue` workflow is intentionally evolvable. Coding agents may notice recurring patterns, missing checks, ambiguous instructions, inefficient steps or failure modes while using it. Those observations may become workflow improvements, but **workflow changes are always human-governed**.

## Core rule

```text
OBSERVE -> VALIDATE -> PROPOSE -> HUMAN APPROVAL -> CHANGE -> VERIFY
```

An agent may identify and propose an improvement on its own. It must **never edit the canonical workflow, its references, adapters or governing agent rules without explicit human approval for that exact proposal**.

Silently "improving" the workflow is prohibited, even when the proposed change appears obviously beneficial.

## What is worth proposing

A proposal should be based on evidence from real use, for example:

- a recurring implementation pattern that should become an explicit rule;
- a verification gap that allowed a defect or regression through;
- a workflow step that repeatedly causes confusion, duplicate work or unnecessary context loading;
- a recovery rule that fails to help with a repeated class of errors;
- a security, tenant-isolation or data-integrity check that should be strengthened;
- a performance validation that is missing for a demonstrated risk;
- a cross-tool compatibility problem between Codex, Antigravity and Claude Code;
- an output rule that makes progress or blockers harder to understand;
- an obsolete rule that no longer matches the repository or supported tooling.

Do not propose changes merely for personal style, novelty or speculative future needs. A workflow improvement must make execution safer, clearer, more reliable, more autonomous, more efficient or easier to verify.

## When to evaluate

At the end of every `implement-issue` run, perform a short workflow retrospective:

1. Did the workflow miss a useful recurring pattern?
2. Did any instruction cause avoidable confusion, rework or repeated failure?
3. Did an important validation/security/performance check need to be invented ad hoc?
4. Did the agent discover a simpler way to achieve the same guarantees?
5. Did a tool-specific compatibility issue reveal drift or ambiguity?

If the answer is no, do not mention workflow improvement.

If the answer is yes, record one concise **workflow improvement candidate**. Continue the issue normally when safe. Prefer presenting the proposal at the end so routine implementation remains autonomous.

A pending workflow-improvement proposal does **not** by itself downgrade a correctly implemented issue from `In Review`; it is a separate governance decision.

If the workflow defect itself makes continued execution unsafe or invalid, stop and request approval before proceeding.

## Validate before asking the human

Before proposing a change, the agent must check that:

- the observation is supported by actual evidence from this repository/run;
- the improvement is reusable beyond a one-off incident;
- an existing rule/reference does not already cover it;
- the change does not weaken security, testing, documentation, frontend independence or scalability guarantees;
- the change does not add unnecessary process, infrastructure or agent-framework complexity;
- the smallest possible workflow change can solve the problem;
- the proposal can work across the supported agents, or its tool-specific scope is clearly stated.

## Required human proposal format

Use this exact information, kept concise:

```text
WORKFLOW IMPROVEMENT PROPOSAL

Observed:
- <concrete pattern/problem seen during real use>

Evidence:
- <issue/run/file/test/error or repeated behavior that proves it>

Proposed change:
- <smallest specific workflow/rule change>

Why change it:
- <why the current workflow is insufficient>

Expected improvement:
- <security/reliability/performance/autonomy/clarity benefit>

Trade-offs / risks:
- <cost, extra checks, possible downside; "none material" only if justified>

Files that would change:
- <canonical workflow/reference/docs/adapters>

Approval required:
- Approve / Reject / Modify
```

The human must be able to understand both **why the workflow should change** and **what will become better** before approving it.

## Approval rules

- Silence is not approval.
- A prior approval for another workflow change is not reusable.
- "Looks good" may count only when it clearly refers to the exact proposal shown immediately before it.
- If the human requests modifications, update the proposal and ask again.
- Do not bundle multiple unrelated workflow changes into one approval request.
- Never lower a security/test/quality gate simply because it is inconvenient; proposals to change a gate require evidence that the new rule preserves or improves the intended guarantee.

## After approval

Apply the smallest approved change and keep all workflow surfaces synchronized:

1. update the canonical `.agents/skills/implement-issue/SKILL.md` or its relevant reference first;
2. update `AGENTS.md`, `README.md`, `docs/ENGINEERING-WORKFLOW.md` or adapters only when the approved behavior affects them;
3. do not duplicate canonical process text into tool-specific adapters;
4. validate links/paths and tool compatibility;
5. review the diff to ensure the implementation matches the approved proposal and nothing more;
6. record the notable workflow change in `CHANGELOG.md` when appropriate.

Git history is the audit trail. Do not add a separate learning database or workflow-memory system unless a future documented need justifies it.

## Anti-rationalization

| Temptation | Required behavior |
|---|---|
| "This change is obviously better." | Explain the evidence and ask the human first. |
| "It is only documentation." | Workflow docs control agent behavior; approval is still required. |
| "I will fix it now and mention it later." | Never change workflow behavior retroactively without approval. |
| "One run exposed the issue, so it must become a rule." | Confirm it is reusable and not a one-off before proposing. |
| "More checks are always safer." | Extra process has cost; add only checks that protect a real guarantee. |
| "This tool needs its own full copy." | Keep one canonical workflow and the thinnest possible adapter. |

## Verification

A workflow change is complete only when:

- [ ] the exact proposal was explicitly approved by a human;
- [ ] the implemented change matches that proposal;
- [ ] canonical behavior remains single-source;
- [ ] relevant README/docs/adapters are synchronized;
- [ ] no security, testing, performance, frontend-agnostic or scalability guarantee was accidentally weakened;
- [ ] the workflow remains simpler than the problem it solves.
