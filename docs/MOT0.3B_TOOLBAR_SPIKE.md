# MOT0.3B — Toolbar implementation spike

## Status

Internal Motion Lab prototype. This is an experiment to validate the contract before designing a public API for `:motion-ui`. It is not a supported reusable component and must not be consumed by another application.

## Scope

The spike exercises the same interaction model under two visual profiles without changing Motion Core or introducing a public toolbar API:

- Material 3 Expressive-oriented appearance using the active Material 3 color scheme.
- App-defined appearance reading Motion UI semantic surface and shape tokens.
- Stable tab IDs and consumer-controlled selected destination.
- Host-driven visual progress represented by `fromTabId`, `toTabId`, and a bounded fraction.
- Measured, variable-width indicator geometry and label expansion/collapse.
- Optional long-press drag selection, cancellation, and a consumer selection request.
- Optional contextual Pin/Unpin action and semantic haptic feedback.
- A single contextual-action Button target with a connecting Canvas morph that narrows/retracts as the action detaches; there is no second focusable connector or duplicate action node.
- Horizontal overflow with selected-tab visibility management and auto-scroll when a long-press drag enters the viewport edge zones; scroll direction is mirrored for RTL.
- Strict single-tab-stop focus: only the current tab can receive focus, arrow keys transfer focus and request selection, and the visible focus indicator follows the active tab. Directional keys map to logical order in LTR and RTL.
- Android system animator-disable preference used as the Motion Lab reduced-motion signal.

The pager and destination data belong to Motion Lab. The prototype does not make Motion UI own navigation or a pager state.

## Implementation boundaries

- `ToolbarPrototype.kt` is sample-only UI and depends on existing Motion Core and Motion UI APIs.
- `ToolbarGeometry.kt` contains sample-owned pure progress, geometry, pointer-coordinate conversion, and edge-scroll helpers. It validates finite progress, stable endpoint IDs, adjacency, and measured bounds.
- `ToolbarGeometryTest.kt` contains 15 tests covering endpoint validation, reverse progress, variable-width interpolation, missing geometry fallback, nearest-tab selection, keyboard navigation/wrap-around, duplicate IDs, LTR/RTL coordinate conversion, edge acceleration, and invalid frame inputs.
- No new dependency is added. The Motion Lab module reuses the repository's existing test bundle.
- CI now runs `:motion-lab:testDebugUnitTest` as well as its debug build.

## Known limitations to resolve during review

- The connector morph and single-target structure are implemented in the sample. Automated tests exercise pointer input at the trailing edge of the adjacent final tab and direct pointer activation of the action in both attached and detached states; transition timing and device-level hit-target behavior still need manual validation.
- Edge auto-scroll uses a quadratic velocity ramp in a 48 dp edge zone, is capped at 900 dp/s, clamps frame delta to 50 ms, and mirrors scroll direction in RTL. Narrow viewport behavior and gesture competition still require manual review.
- The tabs use strict single-tab-stop focus with directional arrow navigation and a visible outline. TalkBack traversal, focus appearance, keyboard hardware behavior, and focus recovery while tabs are reordered/removed still require manual validation.

- Visual comparison uses existing Material 3 colors and Motion UI theme tokens; it is not yet a finalized style interface.
- Android's animator-enabled signal is checked by the sample during composition. Runtime preference changes while the screen is already open need a lifecycle/manual check.
- Physical haptic output, contrast, RTL rendering, large font scale, live reduced-motion preference changes, emulator/device interaction, and rapid gesture interruption remain unverified until exercised.

## Verification

- Historical CI run #197 passed on implementation head `75b6c9dacfb5079fc12dbcc36e0f4789f8d97b40`, before the current Compose UI coverage and CI optimization work.
- The current PR workflow splits fast formatting/unit/release checks from the API 35 emulator suite. CI #215 on PR head `83925ba41f9bcf17f70733379a493577642e3c6e` passed the fast job; the emulator booted in 25.9 seconds through the TCG fallback, then instrumentation compilation exposed a missing `click` test import. Commit `19c78d9` fixes that import, adds explicit KVM device permission handling/diagnostics, and includes long-press drag completion/cancellation tests. Exact-head CI must verify that follow-up before the prototype can be considered green.
- Compose UI tests cover tab semantics, host-progress invariants, LTR/RTL keyboard focus, a single contextual-action semantic target, real pointer hit-testing in attached/detached states, and long-press drag success/cancellation.
- `docs/MOT0.3B_MANUAL_VALIDATION.md` records the remaining device/accessibility review. Its checks are all NOT TESTED until completed on the designated emulator/device.
- Automated CI does not establish TalkBack, focus appearance, visual contrast, RTL rendering, large font scale, dynamic reduced-motion behavior, haptic output, or physical-device interaction.

## Exit criteria

1. Keep CI passing on any subsequent code or documentation commit.
2. Review confirms that host progress never commits navigation state and cannot move the indicator for invalid/non-adjacent endpoints.
3. Compose UI tests cover controlled tab callbacks, roving focus, action semantics, adjacent-tab pointer hit-testing, and drag release/cancel.
4. Manual review exercises forward/reverse swipes, rapid retargeting, drag release/cancel, edge auto-scroll, both styles, reduced motion, RTL, narrow widths, TalkBack, and font scaling using `docs/MOT0.3B_MANUAL_VALIDATION.md`.
5. Only after the manual checks should the public toolbar API and its module-owned UI tests be designed.

Do not treat a green CI run as proof of visual, accessibility, haptic, or physical-device validation.
