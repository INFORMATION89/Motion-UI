# ChatGPT Free: Session Starter

**Scope:** Contributors using the free ChatGPT plan with GitHub repository access to work on Motion.  
**Status:** Optional provider-specific supplement. It does not make ChatGPT or any model mandatory.  
**Last reviewed:** 2026-11-28  
**Authority:** [AGENTS.md](../../AGENTS.md), [docs/RULES.md](../RULES.md), [docs/AI_USAGE_POLICY.md](../AI_USAGE_POLICY.md), and the task queue in [docs/CHATGPT_HANDOFF.md](../CHATGPT_HANDOFF.md) remain authoritative. This guide cannot override them.

## Why this exists

ChatGPT Free can open branches, push commits, and open pull requests, but it cannot run Gradle, Spotless, or tests. Repository work on 2026-11-26/27 (by several agents; not all of it attributed) showed process problems these rules prevent:

- one fix branch grew from a one-line test fix into an unrelated Motion Core change plus five research docs;
- a docs-policy PR was left on a stale base that would have reverted later cleanup;
- eight CI runs in a row (#210-#217) came from small successive pushes to one PR, and most were cancelled or failed;
- many near-duplicate PRs (#14, #16-#24) were opened and closed for one docs contract;
- three of those CI runs (#210, #211, #214) existed only to fix Spotless formatting, which an agent that cannot run `./gradlew spotlessApply` has no way to check before pushing.

The rules below prevent those problems.

## Starter prompt

Paste this at the start of every ChatGPT Free session, filling in the task ID:

```text
You are working on the Motion repository (INFORMATION89/Motion-UI).
Before doing anything, read AGENTS.md, then docs/CHATGPT_HANDOFF.md.

Your task is: <TASK ID, e.g. H3 or F2>. Do only this task. If it is assigned to the O (Opus) tier,
stop and tell me instead of doing it.

Rules:
1. Work on one new branch created from the current main, unless the task names another base.
   Never push to main. Never merge a PR.
2. Plan the whole change first, then push it in as few commits as possible (normally 1-3).
   Do not push a commit per small fix, and do not push repeatedly to one PR to trigger CI.
3. Never use [skip ci] or any other CI-skip marker on a commit that changes anything other than
   documentation (*.md, docs/).
4. Never hand-edit generated files or build output. If they need to change, tell me which Gradle
   command regenerates them.
5. Do not change public API, Motion Core behavior, dependencies, Gradle files, or CI workflows
   unless the task explicitly says so.
6. Never name or reference a consuming application. Motion stays consumer-neutral.
7. Never paste, print, or commit secrets, tokens, or private data. Check the account's data-use
   settings before sharing anything that is not already public.
8. Open the PR using .github/pull_request_template.md and fill in every section, including
   "Tool and tier: ChatGPT Free, tier F". Do not add attribution trailers to commits.
9. If CI fails, find the cause before pushing again. After two failed fixes, stop and report.
10. Finish with a short handoff: branch, commit SHA, what changed, checks run, what is not verified.
11. You cannot run Gradle or Spotless. Do not edit Kotlin (*.kt) or Gradle (*.kts) files unless the
    task explicitly assigns it; if it does, say in the PR that formatting and builds were not run locally.
```

## Tasks it may take

Tier F tasks in [docs/CHATGPT_HANDOFF.md](../CHATGPT_HANDOFF.md), plus tier H tasks marked F-eligible when Haiku is not in use. Tier S tasks only when the owner assigns them explicitly and the guardrails above are followed. Never tier O tasks.
