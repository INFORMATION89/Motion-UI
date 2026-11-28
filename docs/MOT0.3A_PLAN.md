# MOT0.3A — Motion UI theme foundation

**Status:** Delivered; merged into main on 2026-11-27 (PR #13).  
**Scope:** optional Motion UI Android module, theme/token foundation, CI integration, and consumer-neutral repository documentation.

## Goal

Establish the optional Motion UI Android library and its app-configurable theme/token contract without adding visual components or changing Motion Core behavior.

## Delivered

- Add the optional Motion UI Android library module.
- Include it in the standalone Gradle build.
- Set dependency direction to Motion UI → Motion Core; Core remains independent.
- Expose `MotionUiTheme` to supply Material 3 color scheme, typography, shapes, and Motion UI tokens.
- Expose semantic `MotionUiShapes`, `MotionUiSpacing`, and `MotionUiMaterials` tokens.
- Derive neutral material defaults from the app-provided Material 3 color scheme.
- Add focused unit tests for light/dark defaults and consumer overrides.
- Add Motion UI tests and release assembly to required CI.
- Add a live token preview to Motion Lab.
- Generalize repository documentation and remove the obsolete, unbuilt prototype tree.

## Public API contract

- Apps own their Material 3 theme and may supply the values to `MotionUiTheme`.
- Apps may replace Motion UI token values without forking the library.
- Defaults are neutral and based on Material 3 surface roles; they are not a fixed Motion brand.
- Motion UI does not create replacement buttons, fields, navigation, or accessibility semantics.
- Core APIs and behavior remain unchanged.
- Token classes remain limited to values needed by shared components; this is not a second full design system.

## Out of scope

- Animated toolbar implementation and consumer migration (planned separately in MOT0.3B).
- Reusable card/page/sheet components until the toolbar contract and prototype are established.
- Glass rendering and Haze dependency (requires a separate review).
- Consumer application integration or dependency-pin changes.
- Motion Core changes, publishing, and platform expansion.
- Device/TalkBack validation unless separately scheduled and available.

## Verification

The original implementation head `6910c608831fa1812debb047d38cc1cb4385efae` passed required CI run #120 (Spotless, Core unit tests/release build, Motion UI unit tests/release build, and Motion Lab debug build). The PR was merged into `main` on 2026-11-27 (head 1c87bfd). Required CI run #176 passed on that exact head, and push run #202 passed on `main` at `51420fd`.

- [x] Focused Motion UI token tests and implementation build passed on the original implementation head.
- [x] Dependency direction was inspected: Motion UI depends on Core; Core does not depend on UI.
- [x] Spotless and full required CI pass on the final PR head `1c87bfd` (run #176).
- [x] README, API docs, and CI reflect the delivered module.
- [x] No consumer-specific source, branding, dependency, or unsupported platform work remains.
- [ ] Emulator/TalkBack and physical-device checks (not claimed as completed).

## Next stage

MOT0.3B defines and prototypes the animated toolbar as a first-class reusable component. It must preserve variable-width, progress-driven selection and optional contextual-action interactions while supporting multiple visual profiles. Reusable card/surface work follows this toolbar milestone.
