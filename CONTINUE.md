# Continue

Working handoff for the next session. Keep it in step with the actual branches, pull requests, CI results and next actions. Detailed task prompts live in [docs/CHATGPT_HANDOFF.md](docs/CHATGPT_HANDOFF.md); Haiku-tier tasks live here.

## Read first

1. [Agent instructions](AGENTS.md), [project rules](docs/RULES.md) and the [AI usage policy](docs/AI_USAGE_POLICY.md).
2. [MOT0.3B toolbar contract](docs/MOT0.3B_TOOLBAR_PLAN.md) and the [Opus review, 2026-11-28](docs/reviews/OPUS_REVIEW_2026-11-28.md).
3. [Task queue and session prompts](docs/CHATGPT_HANDOFF.md).
4. When needed: [framework overview](docs/FRAMEWORK_OVERVIEW.md), [platform support](docs/PLATFORM_SUPPORT.md), [Motion UI API](motion-ui/README.md), [Core API](motion/README.md).

## Guardrails

- Motion is an independent Android / Jetpack Compose framework. `:motion-ui` is optional and depends on `:motion`; Core never depends on UI.
- Applications own branding, domain, screens, routing and product behavior. Never name or copy a consuming app, or copy third-party source or assets.
- Material 3 is the source for standard controls, typography and accessibility semantics. No Haze until it is separately reviewed.
- Android API 29+, compile SDK 37, JDK 21. iOS and desktop are not supported.
- Focused checks while iterating; one full CI run on the final PR head. Do not merge without explicit owner approval.
- Cloud sessions use the personal **Motion-UI** environment (Android SDK preinstalled; Gradle limited to one worker to avoid proxy rate limits).

## State (2026-11-28)

`main` is at `51420fd` (CI run #202 green). MOT0.1, MOT0.2 and MOT0.3A are delivered; the MOT0.3B toolbar contract is accepted, with no public toolbar API yet.

| PR | What | State |
|---|---|---|
| #28 | Status sync, Opus review, tiered AI routing, attribution rule, Sonnet session plan (docs only) | Draft, CI green on every commit. Waiting on owner review and Q1–Q6. |
| #29 | S2: Core `AmbientClock` pauses frame requests while inactive and holds the requested cadence | Draft, CI run #226 green. Opus 5.5 review (O2) approved with three optional nits. **Needs owner approval** (user-visible motion change). |
| #26 | Motion Lab toolbar prototype | Draft, CI run #222 green (both jobs). Review findings B1–B7 open; S5 next. |

Closed on 2026-11-28: #15 (superseded by #28). `fix/mot03b-pointer-injection-scope` is now split: the test fix went to #26, the Core change to #29, and the research docs are task S3.

## Next steps

The full plan to 1.0 is in [docs/MOTION_ROADMAP.md](docs/MOTION_ROADMAP.md): the definition of done, the audit findings, milestones M0–M7 with tiers and dependencies, and the owner decisions in order. Work through it one task row per session.

Start now, in parallel:
1. **Owner:** review and merge #28 and #29 (roadmap M0-1). Q1 and Q6 are still open.
2. **S5** (Sonnet 5.5, Motion-UI environment): #26 prototype fixes (M0-2). Prompt: "Session B prompt (S5)" in the [handoff](docs/CHATGPT_HANDOFF.md).
3. **M2-4 and M2-5** (Sonnet 5.5): Core finite-validation gaps and tests for untested public APIs. Neither needs owner approval.
4. **H tasks** below (Haiku 5.5 or ChatGPT Free).

## Haiku tasks (tier H)

Mechanical and easy to verify. Run in Claude Code with Haiku 5.5. Tasks marked **F-eligible** may go to ChatGPT Free when Haiku is not used ([starter](docs/agent-guides/CHATGPT_FREE_STARTER.md)). Edit only facts you have checked on GitHub.

| ID | Task | F-eligible | Done when |
|---|---|---|---|
| H1 | Update PR #26's "Verification status": `f1ed520` cherry-picks only `956f044`; run #222 passed both jobs on `6812d7d`; #216/#217 are superseded history; link the Opus review (B1–B7). Keep the manual-validation and NOT TESTED text. Description only. | Yes | The description matches the latest run, with no unverified claim. |
| H3 | After each merge or close, refresh the State table and Next steps here (PR, SHA, CI run). | Yes | This file matches GitHub. |
| H6 | Q2 decided (toolbar first): align README line 7's component list with `docs/FRAMEWORK_OVERVIEW.md`. | Yes | One-line edit matching the owner's answer. |
| H7 | On #29 (owner-authorized push to `claude/dazzling-galileo-v4bahp`): fix O2 nit 1 (the "Skip the first tick" comment in `AmbientClockTickerTest.ticksPerVirtualSecond` doesn't match the code; reword it) and nit 3 (the design-notes status line should say CI run #226 passed). Run `spotlessCheck` and `:motion:testDebugUnitTest`; push once. | No (Kotlin, needs Spotless) | Both nits fixed, local checks pass, one green CI run. |
| H8 | Branch inventory: list every remote branch with its PR number and state (open, merged, closed, none) and last commit date, as a table for the owner. **Delete nothing.** | Yes | Table posted to the owner; the owner decides what to delete. |
| H9 | Mechanical doc check on #28's changed files: relative links resolve, policy section references match the renumbered sections, and task IDs cited exist in the handoff. Report only. | Yes | Table of problems, or "None found". |

## Deferred validation

Emulator and physical-device behavior, TalkBack traversal, contrast inspection, real-display cadence and hardware haptics remain NOT TESTED until run on real supported environments. CI emulator tests run with animations disabled.
