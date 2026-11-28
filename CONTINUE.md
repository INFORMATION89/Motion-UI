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

`main` is at `d15025f` (CI run #232 green). MOT0.1, MOT0.2 and MOT0.3A are delivered; the MOT0.3B toolbar contract is accepted, with no public toolbar API yet. #28 and #29 are merged; **#26 is the only open PR**.

| PR | What | State |
|---|---|---|
| #29 | S2: Core `AmbientClock` pauses frame requests while inactive and holds the requested cadence | Merged 2026-11-28, merge commit `88afabb`. Owner-approved Core behavior change. H7 fixed O2 nits 1 and 3 (`7dcae66`); nit 2 (a test that frames are skipped) is still open and optional. |
| #28 | Status sync, Opus review, tiered AI routing, attribution rule, Sonnet session plan, roadmap (docs only) | Merged 2026-11-28, merge commit `d15025f`; CI run #232 green on `main`. Q2–Q5 are decided; Q1 and Q6 are open. |
| #26 | Motion Lab toolbar prototype | **Open (draft).** Head `a8d616d`, CI run #233 green (both jobs). S5 done: B4 and B6 fixed; `main` merged in, conflicts resolved. B1–B3 are still open. |

Closed on 2026-11-28: #15 (superseded by #28). `fix/mot03b-pointer-injection-scope` is now split: the test fix went to #26, the Core change to #29 (merged), and the research docs are task S3.

## Next steps

The full plan to 1.0 is in [docs/MOTION_ROADMAP.md](docs/MOTION_ROADMAP.md): the definition of done, the audit findings, milestones M0–M7 with tiers and dependencies, and the owner decisions in order. Work through it one task row per session.

Done: M0-1 (#28 and #29 merged) and M0-2 (S5 on #26). Next, in parallel:
1. **Owner:** answer Q1 and Q6 in the [handoff](docs/CHATGPT_HANDOFF.md); review #26 (roadmap M0-5 after M0-3 and M0-4).
2. **M0-3 / S6** (Sonnet 5.5): emulator job required for code PRs, docs-only skip (Q3 decided).
3. **M0-4 / S7** (Sonnet 5.5): the #26 doc conflicts with `main` were resolved by merging `main` (taking `main`'s `CONTINUE.md` and plan status); S7 only needs to confirm nothing else overlaps.
4. **M2-4 and M2-5** (Sonnet 5.5): Core finite-validation gaps and tests for untested public APIs. Neither needs owner approval.
5. **S3** (ChatGPT Free, owner-assigned): research docs in their own docs-only PR.
6. **O3** (Opus 5.5): smallest public toolbar API, after Q4 and the B1–B3 design.

## Haiku tasks (tier H)

Mechanical and easy to verify. Run in Claude Code with Haiku 5.5. Tasks marked **F-eligible** may go to ChatGPT Free when Haiku is not used ([starter](docs/agent-guides/CHATGPT_FREE_STARTER.md)). Edit only facts you have checked on GitHub.

| ID | Task | F-eligible | Done when |
|---|---|---|---|
| H1 | **Done 2026-11-28.** Update PR #26's "Verification status": `f1ed520` cherry-picks only `956f044`; run #222 passed both jobs on `6812d7d`; #216/#217 are superseded history; link the Opus review (B1–B7). Keep the manual-validation and NOT TESTED text. Description only. | Yes | The description matches the latest run, with no unverified claim. |
| H3 | Refreshed 2026-11-28 (recurring). After each merge or close, refresh the State table and Next steps here (PR, SHA, CI run). | Yes | This file matches GitHub. |
| H6 | **Done 2026-11-28** (README line 7 leads with the animated toolbar). Q2 decided (toolbar first): align README line 7's component list with `docs/FRAMEWORK_OVERVIEW.md`. | Yes | One-line edit matching the owner's answer. |
| H7 | **Done 2026-11-28** (`7dcae66`). On #29 (owner-authorized push to `claude/dazzling-galileo-v4bahp`): fix O2 nit 1 (the "Skip the first tick" comment in `AmbientClockTickerTest.ticksPerVirtualSecond` doesn't match the code; reword it) and nit 3 (the design-notes status line should say CI run #226 passed). Run `spotlessCheck` and `:motion:testDebugUnitTest`; push once. | No (Kotlin, needs Spotless) | Both nits fixed, local checks pass, one green CI run. |
| H8 | Branch inventory: list every remote branch with its PR number and state (open, merged, closed, none) and last commit date, as a table for the owner. **Delete nothing.** | Yes | Table posted to the owner; the owner decides what to delete. |
| H9 | Mechanical doc check on #28's changed files: relative links resolve, policy section references match the renumbered sections, and task IDs cited exist in the handoff. Report only. | Yes | Table of problems, or "None found". |

## Deferred validation

Emulator and physical-device behavior, TalkBack traversal, contrast inspection, real-display cadence and hardware haptics remain NOT TESTED until run on real supported environments. CI emulator tests run with animations disabled.
