# ChatGPT handoff — 2026-11-28

**From:** Claude Code session (Opus review plus a Sonnet doc sync), branch `claude/determined-goodall-rap2d4`.
**For:** ChatGPT / Codex. Pick the tool per the tool tiers in [AI usage policy](AI_USAGE_POLICY.md) §2:
- **H** (Haiku): mechanical, easily verified work. **F-eligible** (ChatGPT Free) when Haiku is not used.
- **S** (Sonnet or a balanced ChatGPT/Codex coding model): standard implementation. Interchangeable; pick one primary agent.
- **O** (Opus): keep with Opus, or mark **Opus review pending**. Never assign to H or F.
- **F** (ChatGPT Free): bounded docs, explainers, and PR text. Every session starts with [agent-guides/CHATGPT_FREE_STARTER.md](agent-guides/CHATGPT_FREE_STARTER.md).

Read [AGENTS.md](../AGENTS.md), [RULES.md](RULES.md), [CONTINUE.md](../CONTINUE.md) and [the Opus review](reviews/OPUS_REVIEW_2026-11-28.md) first. Finding IDs (A1, B4, …) refer to that review.

## Ground rules (repeat of the repository rules that matter here)

- Never name or reference consuming applications in this repository. Motion stays consumer-neutral.
- Do not merge, release, tag, or force-push. Keep implementation PRs in draft until exact-head CI is green.
- One coherent change per branch/PR. Don't stack unrelated work, which is how `fix/mot03b-pointer-injection-scope` went wrong.
- Docs-only changes need no local code tests. Report the exact SHA and CI run number for anything you claim passed. Device, TalkBack, and haptics stay **NOT TESTED**.
- Spend at most one full CI run per finished batch. If a job fails, rerun only that job.

## State

See the State table in [CONTINUE.md](../CONTINUE.md). Open drafts: #26 (toolbar prototype, green), #28 (this docs PR, green), #29 (S2 AmbientClock, green, O2 approved, needs owner approval).

## Tier H tasks

Haiku-tier tasks are kept in [CONTINUE.md](../CONTINUE.md#haiku-tasks-tier-h), with their F-eligibility marked.

## Tier S tasks (Sonnet or balanced ChatGPT/Codex model)

| ID | Task | Notes |
|---|---|---|
| S1 | ~~Cherry-pick **only** `956f044` onto `feat/mot03b-toolbar-prototype` and push. Run CI once.~~ **Done 2026-11-28** (`f1ed520`, run #222 green). | Finding B0. |
| S2 | **Done 2026-11-28: PR #29** (CI run #226 green). ~~Recreate the AmbientClock change as its own PR from `main`. Include only `AmbientClock.kt`, the design notes, and the tests. Fix the cadence (A1). Move the tests to `motion/src/test` with JUnit 5 and add the 60 Hz/120 Hz cadence tests (A1, A2). Drop the new `:motion` androidTest dependencies.~~ | Opus re-review done (O2); **owner approval** still required. |
| S8 | In #29, add a test that frames are actually skipped while running (about 2 frame requests per tick at 30 fps on 120 Hz). O2 nit 2; optional. | Optional; same branch as #29. |
| S3 | Put the five research/audit docs from the fix branch in a separate docs-only PR: `MOTION_ARCHITECTURE_REVIEW.md`, `MOTION_RUNTIME_AND_TEST_AUDIT.md`, `REFERENCE_MATERIAL3_EXPRESSIVE.md`, plus their `CONTINUE.md`/spike-doc edits. Scrub any consumer names. | Docs only, no code CI needed. |
| S5 | In PR #26, apply B4: commit selection from `settledPage`. Also apply the small B6 fixes: toggleable semantics on the context action, reduced motion via Core's `rememberReducedMotion()` passed in as a parameter, a disabled tab in the geometry tests, and no throwing on zero-size bounds. | Keep within the Lab module. No public API. |
| S6 | After owner approval (Q3), add a path filter so `**/*.md`-only changes skip the emulator job. Check branch protection first. | Finding B5. |
| S7 | After S1 and S5, resolve doc conflicts between PR #26 and the refreshed `main` docs. Keep only toolbar-specific doc edits in #26. | Finding B7. |

## Tier O tasks (Opus): do not start without Opus or explicit owner go-ahead

| ID | Task | Notes |
|---|---|---|
| O1 | Cross-repository CI and documentation audits from `docs/OPUS_REVIEW_BRIEF.md` (on the closed PR #15 branch). Still **Opus review pending**. | The policy part was superseded by PR #28. Needs a session with access to all related repositories. |
| O2 | **Done 2026-11-28** (Opus 5.5): #29 approved with three optional nits; see the #29 description. Nits 1 and 3 are H7 in `CONTINUE.md`; nit 2 is S8. | Core behavior change; owner approval still required. |
| O3 | Public toolbar API design, including B1 (settle hand-off without snap-back) and B3 (same-pass measure and indicator placement), after S1, S5 and the owner decisions below. | |

## Tier F tasks (ChatGPT Free, starter guide required)

| ID | Task | Done when |
|---|---|---|
| F1 | Write a plain-language explainer for the owner of `docs/reviews/OPUS_REVIEW_2026-11-28.md`. Check every claim against the review; add nothing it does not say. | The explainer is accurate and every statement traces to the review. |
| F2 | Draft PR descriptions and handoffs for the PRs in this list in the `.github/pull_request_template.md` format. | Each section is filled in with verified facts only. |
| F3 | Clarity review of the AI usage policy changes (`docs/AI_USAGE_POLICY.md`, `docs/agent-guides/`). Wording only. | Suggested wording fixes; no change of meaning. |

## Sonnet coding sessions plan

Two short **Sonnet 5.5** sessions, not one long one: S2 and S5 touch different branches and modules, and [the Claude Code guide](agent-guides/CLAUDE_CODE_USAGE.md) says to start fresh between unrelated tasks. Run both on the **Motion-UI** cloud environment (Android SDK preinstalled, so Gradle and Spotless work). Run them one at a time; either order is fine.

**Status:** Session A is done (PR #29). Session B (S5) is next.

| | Session A: S2 | Session B: S5 |
|---|---|---|
| **What** | AmbientClock change as its own PR, with the cadence fix (review A1–A4) | Small toolbar prototype fixes on PR #26 (review B4, B6) |
| **Branch** | New, from `main` | Existing `feat/mot03b-toolbar-prototype` (owner-authorized) |
| **Module** | `:motion` (Core) | `:motion-lab` only |
| **Local checks** | `spotlessCheck`, `:motion:testDebugUnitTest`, `:motion:assembleRelease`, downstream builds | `spotlessCheck`, `:motion-lab:testDebugUnitTest`, `:motion-lab:compileDebugAndroidTestKotlin`, `:motion-lab:assembleDebug` |
| **CI budget** | 1 full run (+1 fix at most) | 1 full run incl. emulator (+1 fix at most) |
| **After** | Opus 5.5 re-review (O2), then owner approval | B1–B3 go to the O3 API design |

Not in either session: B1–B3 (needs Q4 and Opus design), S6 (needs Q3), S7 (after this PR merges), S3 (assigned to ChatGPT Free). Stop rules: after two failed CI fixes, stop and report; stop and ask if a fix seems to need a public API change, a new dependency, or (session B) a Core change.

<details>
<summary>Session A prompt (S2)</summary>

```text
Task S2 (tier S) in INFORMATION89/Motion-UI: recreate the Motion Core AmbientClock
frame-scheduling change as its own focused draft PR, fixing the issues found in review.
You are Sonnet 5.5 in Claude Code on the Motion-UI environment (Android SDK preinstalled).

READ FIRST
- AGENTS.md, docs/RULES.md, docs/reviews/OPUS_REVIEW_2026-11-28.md section A (A1–A5),
  docs/agent-guides/CLAUDE_CODE_USAGE.md, docs/AI_USAGE_POLICY.md "AI attribution".
- Source of the original change: branch fix/mot03b-pointer-injection-scope @ 900ba26.
  Take ONLY: motion/src/main/kotlin/io/github/information89/motion/AmbientClock.kt,
  motion/src/androidTest/kotlin/io/github/information89/motion/AmbientClockTickerTest.kt (to move),
  docs/MOTION_RUNTIME_DESIGN_NOTES.md. Not the motion/build.gradle.kts androidTest changes,
  the toolbar test fix, CONTINUE.md, or the other research docs (task S3).

BRANCH: from current main; the session's designated branch, else fix/ambient-clock-frame-scheduling.

FIX
A1 (blocking) cadence: 900ba26 does `withFrameNanos { onTick() }; delay(ceil(interval))`, so the
  delay and the wait for the next vsync add up (30 fps on 60 Hz -> ~20 fps). Required:
  no frame requests while running is false (keep snapshotFlow + collectLatest); while running,
  tick at the requested fps within one display frame at 60 Hz and 120 Hz using frame-timestamp
  gating (now - last >= interval - small tolerance), keeping `last` across activations so
  toggling can't exceed the cap; the timer may only skip frames, never lengthen the period.
  Keep RESUMED lifecycle, rememberUpdatedState, wall-clock ambientSeconds(), 1-120 fps clamp,
  and rememberAmbientClock's public signature.
A2 tests in CI: move the test to motion/src/test as JUnit 5 (useJUnitPlatform; follow existing
  motion/src/test style). Add no androidTest deps or testInstrumentationRunner.
A3 KDoc: `running` must read Compose snapshot state; observed, not polled. Call it out in the
  PR as consumer-visible.
Tests (manual MonotonicFrameClock + runTest virtual time, no sleeps): keep the 3 existing
  scenarios; add 60 Hz (16_666_667 ns) and 120 Hz (8_333_333 ns) cadence tests (fps=30 ->
  30 +/- 1 ticks per virtual second at both; fps=60 -> 60 +/- 1 at 60 Hz); rapid toggling never
  exceeds the cap; a replaced running lambda is honored. Show the cadence test FAILS on 900ba26
  first, then passes after the fix.
A4/H5: update docs/MOTION_RUNTIME_DESIGN_NOTES.md status to what was actually built and run.

VERIFY (login shell; add --max-workers=1 on HTTP 429)
  bash -lc './gradlew --no-daemon spotlessApply'
  bash -lc './gradlew --no-daemon spotlessCheck :motion:testDebugUnitTest :motion:assembleRelease :motion-ui:testDebugUnitTest :motion-lab:assembleDebug'
  Device/emulator: NOT TESTED.

PUSH ONCE (1-3 commits, no [skip ci], no attribution trailers). Draft PR against main with
.github/pull_request_template.md (Tool and tier: Sonnet 5.5, tier S). State: "Core behavior
change - Opus review pending (O2); owner approval required before merge." One CI run; after two
failed fixes stop. Do not merge; do not edit PR #26 or fix/mot03b-pointer-injection-scope.
STOP AND ASK if the fix seems to need a public API change, a new dependency, or a change to the
ambient-motion contract beyond A3.
FINISH with a handoff: branch, SHA, PR link, changes, checks run/not run, open questions.
```

</details>

<details>
<summary>Session B prompt (S5)</summary>

```text
Task S5 (tier S) in INFORMATION89/Motion-UI: small fixes to the Motion Lab toolbar prototype in
PR #26. You are Sonnet 5.5 in Claude Code on the Motion-UI environment (Android SDK preinstalled).

READ FIRST: AGENTS.md, docs/RULES.md, docs/reviews/OPUS_REVIEW_2026-11-28.md section B,
docs/AI_USAGE_POLICY.md "AI attribution".

BRANCH: the owner authorizes pushing to feat/mot03b-toolbar-prototype (PR #26) only. Never rebase,
amend, force-push, or merge. Do not touch :motion, :motion-ui, Gradle files, or CI workflows.

FILES: motion-lab/src/main/kotlin/io/github/information89/motion/lab/ToolbarPrototype.kt,
ToolbarGeometry.kt, motion-lab/src/test/.../ToolbarGeometryTest.kt,
motion-lab/src/androidTest/.../ToolbarPrototypeUiTest.kt (only if a test must follow a change).

FIXES
1. B4: derive selectedTabId from pagerState.settledPage, not currentPage; keep progress endpoints
   from currentPage + currentPageOffsetFraction (toolbarProgressForPager stays).
2. B6 reduced motion: replace both `!ValueAnimator.areAnimatorsEnabled()` reads with Core's
   rememberReducedMotion() (io.github.information89.motion, Morph.kt) in ToolbarPrototype and pass
   it to ToolbarPrototypeBar as a new `reducedMotion: Boolean` parameter; update setToolbar() in the
   UI test to pass it (keep reducedMotion = true unless a test needs otherwise).
3. B6 toggle semantics: Pin/Unpin exposes on/off state (toggleableState or stateDescription
   "Pinned"/"Not pinned") while staying exactly ONE semantic clickable target (the three existing
   context-action target tests must still pass). Add one UI test that the state changes on click.
4. B6 zero-size bounds: in the tab's onGloballyPositioned, skip publishing bounds when width <= 0
   instead of constructing ToolbarTabBounds (which throws). Keep its require() checks.
5. B6 disabled tabs (tests only): ToolbarGeometryTest cases showing that with only enabled IDs
   passed, nearestToolbarTabId and toolbarAdjacentTabId never return an excluded ID, and progress
   with an excluded endpoint falls back to the committed selection.
Write each new test first and note whether it failed before the fix (item 5 may already pass).

VERIFY (login shell; add --max-workers=1 on HTTP 429)
  bash -lc './gradlew --no-daemon spotlessApply'
  bash -lc './gradlew --no-daemon spotlessCheck :motion-lab:testDebugUnitTest :motion-lab:compileDebugAndroidTestKotlin :motion-lab:assembleDebug'
  Emulator tests run only in CI.

PUSH ONCE (1-3 commits, no [skip ci], no attribution trailers); one CI run; after two failed fixes
stop and report. Add an "S5 changes" section to PR #26's description (changes, commands, CI run and
result, NOT TESTED items) and "Tool and tier: Sonnet 5.5, tier S". Keep it a draft.
STOP AND ASK if a fix needs changes outside :motion-lab, a public API, a new dependency, or would
change B1/B2/B3 behavior.
FINISH with a handoff: branch, SHA, CI run, changes per item, checks run/not run, open questions.
```

</details>

## Owner decisions

Decided by the owner on 2026-11-28:

- **Q2: toolbar first.** Motion UI leads with the animated toolbar; README line 7 gets aligned (H6, now unblocked).
- **Q3: emulator job required for code PRs; docs-only PRs skip it.** S6 is unblocked; check branch protection first.
- **Q4: the drag indicator follows the finger continuously**, with a lifted/pressed look, as the accepted contract says. No contract change. Part of O3 (with B1/B3).
- **Q5: programmatic far jumps travel directly** to the target; intermediate labels stay collapsed. Part of O3.

Still open:


1. **Q1.** Should `docs/OPUS_REVIEW_BRIEF.md`, which names other repositories, live in this consumer-neutral repository, or somewhere shared or private?
6. **Q6.** Should a PR-hygiene CI check be added (required template sections, no `[skip ci]` on code commits)? A CI change needs owner approval.
