# MOT0.3B — Manual validation record

## Purpose and status

Use this checklist for the Saturday emulator/device review of the internal Motion Lab toolbar prototype. It is a test record, not a statement that validation has already happened.

**Current status: NOT TESTED.** Leave each checkbox empty until the check has actually been performed and record failures or deviations below. Passing automated CI does not mark a manual item as passed.

## Test environment

| Field | Value |
|---|---|
| Motion commit SHA | |
| Date and time | |
| Device / emulator model | |
| Android version / API | |
| Screen resolution and density | |
| Font scale | |
| Theme | Light / Dark |
| Layout direction | LTR / RTL |
| Animator duration scale | |
| TalkBack version / state | |
| Tester | |

## Semantics and assistive technology

- [ ] TalkBack identifies the toolbar as a coherent tab set.
- [ ] Every tab has a useful, stable spoken label; icon-only or collapsed labels remain understandable.
- [ ] The committed tab is announced as selected, and selection changes are not announced from visual pager progress alone.
- [ ] Pin / Unpin is announced as one action, not as separate connector and button targets.
- [ ] Expanding, retracting, attaching, and detaching the contextual action do not create duplicate announcements or unexpectedly move TalkBack focus.
- [ ] Disabled or unavailable destinations are announced and behave consistently.

Record announcements, focus order, and any unexpected traversal:

## Keyboard and focus

- [ ] Tab traversal exposes one toolbar tab stop at a time, not every tab as a separate sequential stop.
- [ ] LTR Arrow Left / Arrow Right follow logical order and wrap at both ends.
- [ ] RTL Arrow Left / Arrow Right follow logical order and wrap at both ends.
- [ ] Enter / Space activates the focused tab and contextual action as expected.
- [ ] The focus indicator is visible in both visual profiles and light / dark themes.
- [ ] Focus follows a tab after it becomes selected and remains visible after horizontal auto-scroll.
- [ ] Focus recovers sensibly when a destination is disabled, removed, or reordered.

Record keyboard, focus, and RTL observations:

## Selection ownership and gestures

- [ ] A tab tap emits one destination request without locally committing controlled selection.
- [ ] Host pager progress previews the indicator without emitting a selection request on every frame.
- [ ] A long press followed by a drag to another tab requests that destination only when released.
- [ ] Cancelling a long-press drag does not request a destination or commit selection.
- [ ] A normal tap is not mistaken for a drag; a movement before long-press timeout does not begin drag selection.
- [ ] Drag auto-scroll activates near both viewport edges and remains capped during long holds.
- [ ] Edge auto-scroll direction is correct in both LTR and RTL.
- [ ] Rapid direction reversals, a new host progress update, and gesture interruption leave the final selected destination consistent with host state.
- [ ] Dragging over the contextual action does not activate it or transfer the gesture to it.

Record gesture paths, destinations, and any callback or selection mismatch:

## Contextual action and hit targets

- [ ] Contextual action expansion and retraction complete without clipping or jumping.
- [ ] Attached and detached states preserve one accessible, clickable action target.
- [ ] Hidden state exposes no clickable or focusable action target.
- [ ] Taps at the trailing edge of the adjacent tab still activate that tab.
- [ ] Taps on the contextual action activate only the action.
- [ ] Touch targets are practical and at least 48 dp where applicable.

Record transition, target size, and hit-testing observations:

## Layout and visual profiles

- [ ] Both M3 Expressive-oriented and CUSTOM profiles use the same tabs, callbacks, and selection model.
- [ ] Variable-length labels remain legible and do not overlap the contextual action.
- [ ] Narrow widths (at least 320 dp and 360 dp) support the expected scroll and gesture behavior.
- [ ] RTL visual order, indicator placement, and contextual-action placement are correct.
- [ ] Light and dark themes have sufficient selected, unselected, disabled, and focus contrast.
- [ ] Font scales 1.3 and 2.0 do not clip essential labels, focus indicators, or action targets.
- [ ] Window resizing and density changes preserve sensible geometry.

Record profile, dimensions, contrast, and font-scale observations:

## Motion and reduced motion

- [ ] Normal motion does not stutter or leave the indicator between tabs after an interrupted transition.
- [ ] Repeated retargeting, reverse swipes, and quick tab changes settle on the correct destination.
- [ ] Reduced-motion / animator scale 0 removes or simplifies nonessential motion while preserving every action and final state.
- [ ] Changing the system animator preference while the screen is already open is handled correctly, or the limitation is recorded.
- [ ] Haptic feedback occurs only for the intended optional events and is not required for task completion.

Record timing, reduced-motion, and haptic observations:

## Result

| Area | Result | Evidence / issue reference |
|---|---|---|
| TalkBack and semantics | Not tested | |
| Keyboard and focus | Not tested | |
| Selection and gestures | Not tested | |
| Contextual action | Not tested | |
| Layout and visual profiles | Not tested | |
| Reduced motion and haptics | Not tested | |

**Overall outcome:** Not tested

**Blocking issues:**

**Follow-up fixes:**

**Retest commit SHA:**
