# Opus review — 2026-11-28

**Reviewer:** Claude Opus 5.5 (Claude Code), independent pass.
**Status:** Review complete for the two items below. Findings are recommendations; the owner decides. Nothing was merged, and no other branch was changed.
**Not covered here:** PR #15 (AI model-routing policy) and the cross-repository CI and documentation audits in its brief. This session could reach this repository only. Those parts remain **Opus review pending**. (Update: PR #15 was closed on 2026-11-28 as superseded by PR #28; the cross-repository audits are still pending.)

| Item | Ref reviewed | Verdict |
|---|---|---|
| A. Motion Core `AmbientClock` scheduling change | `fix/mot03b-pointer-injection-scope` @ `900ba26` | **Request changes.** Split it into its own PR; it needs a cadence fix, JVM tests, and owner sign-off on a user-visible change. |
| B. Toolbar prototype | PR #26 `feat/mot03b-toolbar-prototype` @ `89766a5` | **Good spike; not ready to shape a public API yet.** Land the one-line CI fix, then address the important findings before the API design. |

Method: static reading of source, tests, CI workflow, and Gradle config at the refs above. **No builds, tests, emulator, or device runs were performed in this review.** Severity: **blocking**, **important**, **minor**.

---

## A. `AmbientClock` frame-scheduling change (Motion Core)

Files: `motion/src/main/kotlin/io/github/information89/motion/AmbientClock.kt`, `motion/src/androidTest/kotlin/io/github/information89/motion/AmbientClockTickerTest.kt`, `motion/build.gradle.kts`.

The motivation is correct. The current `main` implementation calls `withFrameNanos` on every display frame while RESUMED, even when `running` is false. So "paused while unseen" stops state writes but not frame scheduling. The fix direction (observe `running`, suspend while inactive) is right. Three problems need addressing before it lands.

### A1. Blocking: effective cadence drops well below the requested `fps`

```kotlin
while (true) {
    withFrameNanos { onTick() }
    delay(intervalMillis)          // interval rounded UP to whole ms
}
```

Each period is `ceil(interval)`, plus the wait for the *next* vsync after the delay expires. The delay starts after the frame callback, so the waits add up rather than overlap:

| Requested | Display | Old behavior (frame-gated) | New behavior |
|---|---|---|---|
| 30 fps (default) | 60 Hz | 30 fps (every 2nd frame) | 34 ms → next vsync at 50 ms → **20 fps** |
| 30 fps | 120 Hz | 30 fps (every 4th frame) | 34 ms → 41.7 ms → **24 fps** |
| 60 fps | 60 Hz | 60 fps | 17 ms → 33.3 ms → **30 fps** |

This is a user-visible change to an agreed motion contract (F4). `AGENTS.md` requires owner agreement for that, and here it's unintended. The existing test `timerDoesNotRequestFramesMoreFrequentlyThanConfiguredInterval` checks only the lower bound, so it can't catch this.

**Recommended fix.** Keep the frame-timestamp gate from the original code for accuracy, and use the timer only to avoid waking for frames that will be skipped. For example: after a tick at frame time `t`, delay roughly `interval − (one observed frame period) − small slack`, never below 0. Then in each `withFrameNanos`, tick only when `now − last ≥ interval − tolerance`. Any equivalent design is fine if it meets both of these:
- no frame requests while inactive;
- within one display frame of the requested cadence at 60 Hz and 120 Hz.

**Required regression test.** Use a manual frame clock that emits frames at 16.67 ms (and 8.33 ms), together with virtual time. Assert that `fps = 30` produces 30 ± 1 ticks per virtual second. Assert the same for `fps = 60` at 60 Hz.

### A2. Important: the new tests never run in CI

- `AmbientClockTickerTest` is under `motion/src/androidTest`. CI runs only `:motion:testDebugUnitTest` and `:motion-lab:connectedDebugAndroidTest`. Nothing runs `:motion:connectedAndroidTest`, so these tests are dead weight today.
- The tests don't need a device: `MonotonicFrameClock`, `mutableStateOf`, and `runTest` are all JVM-only. Move them to `motion/src/test`, using JUnit 5 like the rest of `:motion` (`useJUnitPlatform()`).
- Then drop the three new `androidTestImplementation` dependencies and the `testInstrumentationRunner` line from `motion/build.gradle.kts`. That avoids adding a new instrumentation layer to Core, which `AGENTS.md` rule 6 says needs owner approval.

### A3. Important: `running` now must read Compose snapshot state (silent contract change)

The new code uses `snapshotFlow { running() }`, which re-evaluates only when snapshot state it read changes. A lambda that reads a plain `var` or a non-snapshot source used to work (it was polled every frame). Now it would never pause or resume.
- If the restriction is intended, document it in the KDoc for `rememberAmbientClock`: "`running` must read Compose state". Also record the change in the PR as a behavior change for consumers.
- `rememberAmbientClock` has no callers inside this repository, so in-repo usage gives no evidence about consumers. Treat it as public API with unknown callers.

### A4. Minor

- Fast true→false→true toggling ticks on the next frame each time, ignoring the previous tick time, so it can briefly go over the cap. That's acceptable, but cover it in the A1 fix by keeping `last` across activations.
- The design-notes test list includes lifecycle STOP/RESUME, a replaced lambda, and resuming from the wall-clock phase. None of these are tested yet. Add at least the replaced-lambda case to the JVM tests.
- `MOTION_RUNTIME_DESIGN_NOTES.md` says "not yet built or run". Update it to match whatever the new PR verifies.

### A5. Process

The change sits on a branch named for a one-line test fix and stacked on PR #26. Recreate it as its own PR from `main` with only `AmbientClock.kt`, the moved tests, and the design-notes doc. Mark it **Opus re-review after A1/A2** and get **owner approval** before merge (Core behavior change).

---

## B. PR #26 — Motion Lab toolbar prototype

Files reviewed: `motion-lab/.../ToolbarPrototype.kt`, `ToolbarGeometry.kt`, `ToolbarGeometryTest.kt`, `ToolbarPrototypeUiTest.kt`, `.github/workflows/build.yml`.

**What is solid:**
- Progress endpoints use stable IDs, and invalid, non-finite, or non-adjacent progress falls back to the committed selection.
- Selection is only requested (via `onSelectionRequest`), never committed locally.
- Host progress is ignored while a drag is captured.
- `onSelectionInteractionStarted` fires once.
- Cancellation emits no request.
- There is a single roving focus stop, with RTL-mirrored arrow keys.
- The connector `Canvas` has no semantics and doesn't intercept clicks (tested).
- Both profiles share one state model.
- The pure geometry functions have good unit coverage.

### B0. Blocking (already known): CI is red

Cherry-pick **only** `956f044` (`size.width` → `width`) onto #26 and run CI once. Do **not** merge the whole `fix/mot03b-pointer-injection-scope` branch (see A5).

### B1. Important: the indicator snaps back after a drag release

On release, `settlingTargetId = target` shows the indicator at the target, then `requestSelection` starts `animateScrollToPage`. On the next frame, the pager reports non-zero offset. `activeSettlingTargetId` is then nulled because `hostProgress != null`. Host progress starts from the *old* page, so the indicator jumps back to the origin tab and sweeps forward again.

The settle should hand off from the indicator's current geometry, not restart from the pager's origin (see `AGENTS.md`: "retarget from current animated values"). Possible fixes:
- Keep the local settle until host progress reaches the target's neighborhood.
- Animate the indicator bounds with `Animatable`s that retarget from their current value, whichever source drives them.

CI can't catch this today (see B5).

### B2. Important: during drag the indicator jumps tab-to-tab instead of tracking the pointer

The contract (§2, §5) says the selection surface is "pressed and lifted, then dragged", and that "drag position should track the pointer predictably". The prototype sets `indicator = tabBounds[dragTargetId]`, so it hops between tabs, with no lift or press state. Before the public API is designed, decide whether pointer tracking is required:
- If yes, the prototype should show it, because it changes the state model (continuous drag position vs. discrete target).
- If no, amend the contract text.

This is an **owner decision**.

### B3. Important: the indicator reads last frame's tab bounds

During a swipe, label widths change every frame (`labelWidth * fraction`), which moves the tab bounds. The bounds are published through `onGloballyPositioned` into a state map after layout. The indicator, computed in composition, therefore uses the previous frame's geometry: a one-frame lag that shows up as jitter while widths change.

The whole bar also recomposes every frame because `hostProgress` is read in composition. That's acceptable in a spike. For the public component, measure tabs and place the indicator in the same layout pass (a custom `Layout`, or `Modifier.layout`), and defer progress reads to layout or draw.

### B4. Important: the host mapping uses `currentPage` as the committed selection

`selectedTabId` comes from `pagerState.currentPage`, which flips at the halfway point of a swipe. As a result:
- the `selected` semantics and `isCommittedSelection` change mid-gesture;
- the long-press `pointerInput` key changes mid-gesture;
- the scroll-into-view effect retargets mid-gesture.

The committed selection should be `pagerState.settledPage` (or the target page of a programmatic scroll). Progress endpoints can still come from `currentPage` plus the offset. This mapping will become the reference example in consumer docs, so it should be right.

### B5. Important: the CI emulator job can't exercise animation paths, and it runs on every PR

- `disable-animations: true` sets the animator scale to 0, so `ValueAnimator.areAnimatorsEnabled()` returns false and `reducedMotion` is always true in UI tests. Every spring becomes `snap()`, so B1-type defects are untestable. There is also no `mainClock` control in `ToolbarPrototypeUiTest`.
  - Either drive animations with `composeRule.mainClock.autoAdvance = false` and explicit `advanceTimeBy` in a test that injects "animations enabled", or accept that motion paths are covered only by JVM state tests.
  - Make reduced motion an *input* to `ToolbarPrototypeBar` instead of reading `ValueAnimator` inside it.
- The `instrumentation` job (~5 min, emulator) has no path filter, so documentation-only PRs pay for it. Add a `paths`/`paths-ignore` filter or a changed-files gate, so `**/*.md`-only changes skip it, while keeping it required for code changes. Confirm branch-protection compatibility first.
- Adding this emulator layer is an expansion of verification, and needs recorded owner approval (`AGENTS.md`, cost-aware rule 6).

### B6. Minor / accessibility

- **Context action toggle state:** Pin/Unpin is a plain `Button` whose label changes. The contract says it "can represent checked/toggled state". Expose it as toggleable (`Modifier.toggleable` / `IconToggleButton` semantics, or `stateDescription`) so TalkBack announces the state. The optional state-change haptic isn't implemented.
- **Reduced motion:** read through `ValueAnimator.areAnimatorsEnabled()` directly in two composables. Motion Core already provides `rememberReducedMotion()`, so reuse it, or take it as a parameter (see B5).
- **Bounds validation:** `ToolbarTabBounds.init` throws on zero width, and it's constructed inside `onGloballyPositioned`. That's safe in this spike because padding guarantees width > 0. A public API must not crash on a transient zero-size layout (`AGENTS.md`: preserve legitimate zero-size transient anchors). Skip or ignore such samples instead.
- **Disabled and hidden tabs:** the geometry helpers (`isValidFor`, `nearestToolbarTabId`, `toolbarAdjacentTabId`) assume every ID passed in is visible and enabled. The prototype has no disabled tabs, so the contract rules on disabled or removed tabs are untested. Add one disabled tab to the geometry tests before API design.
- **Programmatic jumps:** a tap on a far tab makes the indicator sweep across every intermediate tab, unfolding each label in turn (from `animateScrollToPage`). This may be fine, but it's an owner design call.
- **Label measurement:** `textMeasurer.measure` runs per tab on every recomposition. That's cached, but tie it to the B3 layout rework.

### B7. Doc overlap with `main`

PR #26 also edits `AGENTS.md`, `CONTINUE.md`, `README.md`, `RULES.md`, `MOTION_REPO_PLAN.md`, and `MOT0.3B_TOOLBAR_PLAN.md`. Those files were refreshed on `main`'s status-sync branch on 2026-11-28, so expect conflicts. Keep only the toolbar-specific doc changes in #26 when rebasing.

---

## Recommended order

1. B0: cherry-pick `956f044` into #26 and run CI once. *(Sonnet / balanced tier)*
2. A5: recreate the AmbientClock work as its own PR; apply A1–A3. *(Sonnet implements; Opus re-reviews.)*
3. B4 and B6 (small fixes) in #26. *(Sonnet)*
4. Owner decisions: B2 (pointer tracking), the B6 programmatic-jump sweep, and B5 (CI emulator approval and path filter).
5. B1 and B3 as part of the public-API design spike. *(Opus designs; Sonnet implements.)*

## Evidence

- `main` @ `51420fd`: push run #202 **passed**. The final PR #13 head `1c87bfd`: run #176 **passed**.
- PR #26 @ `89766a5`: run #217, fast checks **passed**, API 35 instrumentation **failed** (compile error at `ToolbarPrototypeUiTest.kt:339-340`).
- `fix/mot03b-pointer-injection-scope` @ `900ba26`: **no CI run inspected**; `:motion` androidTest is not wired into CI.
- Device, TalkBack, contrast, haptics: **NOT TESTED**.
