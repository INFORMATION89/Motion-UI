# Motion Runtime Design Notes

**Date:** 2026-11-27  
**Branch:** `claude/dazzling-galileo-v4bahp` (PR for task S2)  
**Baseline:** `51420fdbb117ddfb8738263cf428b85e197a7719` (`main`)  
**Status:** ambient-clock implementation (§1, §5) built and unit-tested at `344c02d`: `./gradlew --no-daemon --max-workers=1 spotlessCheck :motion:testDebugUnitTest :motion:assembleRelease :motion-ui:testDebugUnitTest :motion-lab:assembleDebug` passed (72 `:motion` JVM tests, 0 failures). CI run #226 passed on `370081c`, whose code is the same as `344c02d`. Not run: instrumentation, emulator, and device. §2–§4 remain design only, with no implementation.

## 1. Ambient clock: frame scheduling is not actually paused

Source: `motion/src/main/kotlin/io/github/information89/motion/AmbientClock.kt`.

The current loop requests `withFrameNanos` continuously while the lifecycle is RESUMED. It checks the latest `running` predicate inside every frame callback and only writes the returned state when the predicate is true and the FPS interval has elapsed.

Consequences:

- `running = false` suppresses state writes, but the coroutine still receives a callback every display frame.
- `fps` caps state updates, not frame callback scheduling. At 30 FPS on a 120 Hz display, the loop still wakes for the intervening display frames.
- The documentation's "paused while unseen" wording is stronger than the implementation. It pauses value updates, not the frame loop itself.

This is a concrete efficiency/documentation mismatch, not a correctness failure in the returned time value.

### Recommended implementation direction

Use an event-driven active/inactive boundary rather than polling the predicate on every display frame:

1. Observe changes to the supplied Compose-state-backed `running` predicate.
2. When inactive, suspend the frame loop entirely.
3. When active, schedule updates at the requested cadence, aligning each emitted update to a display frame.
4. Keep the existing RESUMED lifecycle boundary; leaving RESUMED must cancel the active loop and re-entering it must resume from wall-clock time.
5. Keep `rememberUpdatedState` or an equivalent latest-lambda mechanism so changing the predicate does not require restarting the whole composable effect.
6. Validate `fps` bounds and preserve the existing 1–120 clamp unless the public contract is deliberately revised.

Implementation observes the predicate with `snapshotFlow` + `collectLatest` and cancels the active ticker immediately when it becomes false. Ticks run in `withFrameNanos` and are gated on frame timestamps (`now - last >= interval - 1 ms`); `last` is kept across activations so rapid toggling cannot exceed the cap. A cancellable timer sleeps for the time left until the next tick minus one observed frame period and 2 ms slack (floored at 0), so it only avoids waking for frames that would be skipped and never lengthens the period. The composable retains ``rememberUpdatedState` and the existing `RESUMED` lifecycle boundary. No dependency or public API is added. `running` must read Compose snapshot state; this is documented in the KDoc and called out as a consumer-visible change.

An earlier iteration (`delay(ceil(interval))` after every frame) stacked the delay on the wait for the next vsync and delivered about 20 fps for a 30 fps request at 60 Hz, 24 fps at 120 Hz, and 30 fps for a 60 fps request at 60 Hz. The cadence tests below fail against that loop and pass against this one.

### Required regression evidence before implementation

- Inactive state produces no further clock updates and no active frame loop.
- Switching inactive → active resumes without waiting for an unrelated recomposition.
- Switching active → inactive cancels the active loop.
- FPS pacing emits no faster than the configured cap while aligning emitted values to frame time.
- Lifecycle STOP/RESUME cancels and restarts the loop without duplicate collectors.
- The returned value resumes from current wall-clock phase, not stale accumulated elapsed time.
- A replaced `running` lambda is honored without stale captures.

Use a controllable frame clock and Compose snapshot state. No sleeps or real-time timing assertions.

## 2. System motion preferences: keep Android observation internal

Current `rememberReducedMotion()` and `rememberAmbientMotionAllowed()` are remembered snapshots. They do not observe later changes to animator duration scale or battery saver.

Recommended design boundary:

- One internal Android-specific source of system-motion state, scoped to the current composition/context and disposed with it.
- Observe animator-duration-scale changes through a `ContentObserver`.
- Observe battery-saver transitions through the documented power-save-mode broadcast.
- Publish the combined values as Compose state; avoid a process-global mutable preference cache.
- Handle registration/unregistration, context changes, lifecycle, and API-level receiver requirements explicitly.
- Keep the existing public composable names and Boolean return types initially. Do not add a new public settings API or multiplatform abstraction.

The implementation must decide whether the two existing helpers share one observer owner or use one narrowly scoped observer each. Prefer one shared internal owner only if it avoids duplicate registrations without coupling unrelated compositions or creating global state.

### Required regression evidence

- Initial values match the system APIs.
- Animator setting changes invalidate the observed value while composed.
- Battery-saver changes invalidate the observed value while composed.
- Disposal unregisters every observer/receiver exactly once.
- Recomposition and context/lifecycle changes do not leak or duplicate registrations.
- Observer callbacks arriving after disposal cannot mutate active UI state.
- Failures to read/register system signals degrade safely and are documented.

The system's live setting changes still require an emulator/device check; unit tests alone cannot prove Android broadcasts and settings observation on supported OS versions.

## 3. Haptics: clarify rate-limit semantics before touching dispatch

Current `MotionHaptics.perform` advances `HapticToken.last` before checking the system feedback setting and before calculating whether the final intensity is effectively zero. Thus a suppressed event can consume the token's minimum-gap interval.

This is currently an observable behavior, but the source does not establish whether it is intentional. Recommended contract for owner review:

- A disabled system feedback setting should not consume a token's rate-limit window.
- A computed intensity at or below the existing dispatch threshold should not consume it.
- Once a real dispatch attempt is made, the rate-limit timestamp should advance even if primitive dispatch fails and the view fallback is attempted. This prevents repeated failed dispatch attempts from flooding the fallback path.
- Preserve per-token rate limiting and current fallback behavior unless separate evidence demonstrates a defect.

Do not change this behavior in this design-only batch. Before implementation, add focused tests for disabled feedback, zero intensity, rate-limited repeat, unsupported primitive fallback, and dispatch failure fallback. Keep any platform seam internal and production-meaningful; do not add public hooks solely for tests.

## 4. Scope and sequencing

1. Complete deterministic ambient-clock ticker coverage and verify lifecycle cancellation/re-entry.
2. Implement the internal system-motion observer and focused lifecycle/disposal tests.
3. Clarify and test haptic rate-limit behavior before making the smallest implementation change.
4. Add a toolbar reduced-motion rendering/interaction test only after the live signal behavior is stable.
5. Run one coherent verification batch after the implementation batch is complete.

No public API, dependency, component, platform target, consumer, CI workflow, or existing motion contract is changed by this note.


## 5. Implementation progress — ambient clock

Built at `344c02d` (JVM tests in `motion/src/test/kotlin/io/github/information89/motion/AmbientClockTickerTest.kt`, JUnit 5, no new dependencies or instrumentation setup):

- inactive state requests no frames; state-driven activation; deactivation cancels the pending request;
- cadence on a virtual-time vsync clock: 30 fps on 60 Hz and 120 Hz → 30 ± 1 ticks per second; 60 fps on 60 Hz → 60 ± 1;
- rapid true/false/true toggling never ticks faster than the cap (within the 1 ms tolerance);
- a replaced `running` lambda is honored.

Cadence result before the fix (the earlier delay-per-frame loop): 20, 24 and 30 ticks per second respectively, and the toggling test failed. After the fix all eight tests pass.

Still not covered: lifecycle STOP/RESUME integration and wall-clock phase after resume (they live in `rememberAmbientClock`, which needs a Compose host). No instrumentation, emulator, device, or CI result is claimed here.
