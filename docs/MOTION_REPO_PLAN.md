# Motion repository plan

This document preserves the repository-extraction decisions from 2026-11-21. The current implementation state below takes precedence over historical target-state descriptions.

## Current state — 2026-11-26

- Motion is an independent Android/Jetpack Compose project with a standalone Gradle build and three included modules: reusable library `:motion`, optional UI library `:motion-ui`, and standalone sample application `:motion-lab`.
- The modules use Android Gradle plugins and Jetpack Compose. This is not a Kotlin Multiplatform project.
- The current target is Android 10 / API 29+, compile SDK 37, Android Build Tools 37, and JDK 21.
- `:motion-lab` is a built Gradle application module and is included in CI through `:motion-lab:assembleDebug`.
- The former `lab/` directory was an unbuilt prototype, not a Gradle module. It has been removed because its sources were application-coupled and carried LGPL-3.0 notices; no implementation was copied into the maintained modules.
- CI runs Spotless, Core unit tests/release build, Motion UI unit tests/release build, Motion Lab unit tests, and the Motion Lab debug build.
- Consumer applications may integrate Motion through a Git submodule and Gradle composite build, pinned to an exact tested commit. Motion is not currently published to Maven.
- Desktop (Windows/macOS/Linux) and iOS support are future design work, not supported targets.
- Physical-device validation remains NOT TESTED / DEFERRED while suitable hardware is unavailable. Passing CI does not establish device behavior.

## MOT0.3A status — delivered

- The optional `:motion-ui` Android library is included in Gradle and depends on `:motion`; Core has no dependency on UI.
- Its initial public API is `MotionUiTheme`, `MotionUiThemeTokens.current`, and configurable semantic shape, spacing, and surface-material tokens.
- Neutral defaults derive surface colors from the caller's Material 3 color scheme. Apps can inject their own Material 3 theme values and replace Motion UI tokens.
- No Haze dependency, glass rendering implementation, visual components, consumer code, or dependency-pin changes are included in the foundation.
- Merged into `main` on 2026-11-27 (PR #13); required CI passed on the final PR head `1c87bfd` (run #176) and on `main` at `51420fd` (run #202).

## MOT0.3B status — contract and implementation spike

- The animated toolbar contract in `docs/MOT0.3B_TOOLBAR_PLAN.md` is accepted (PR #25).
- The prototype is merged in Motion Lab (PR #26, merge commit `109b4e3`). It is intentionally not exposed as a public `:motion-ui` API; the public API is roadmap milestone M1 (`docs/MOTION_ROADMAP.md`).
- The spike exercises stable-ID host progress, variable-width geometry, optional long-press drag with RTL-aware edge auto-scroll, a connected/retractable contextual-action morph with one semantic button target, two configurable visual profiles, and strict single-tab-stop keyboard focus with RTL-aware directional navigation.
- The sample-only implementation and its 15 pure geometry/interaction unit tests passed CI run #197 on exact head `75b6c9dacfb5079fc12dbcc36e0f4789f8d97b40` (Spotless, Core tests/release build, Motion UI tests/release build, Motion Lab unit tests/debug build).
- Public API design (M1) follows only after manual interaction/accessibility review and focused Compose UI tests. Emulator, TalkBack traversal, real focus appearance, contrast, font-scale, live reduced-motion preference changes, connector hit testing, and physical haptic checks remain deferred until performed.

## Original decisions (2026-11-21)

- **Workflow: Git submodule + Gradle composite build.** A consumer repository can keep Motion at a submodule path and build it from source through `includeBuild`. Exact commit pinning keeps the dependency reproducible.
- **Independence:** Motion must not import consumer code or depend on any consuming application. Generic motion behavior belongs in Motion; application styling, screens, assets, and product logic remain app-side.
- **History:** the extracted Motion repository keeps project-owned Motion history, not the history of any consuming application.
- **Motion Lab direction (historical):** a separate Material You Expressive app was planned to demonstrate Motion with standard Material 3 components. This plan has been delivered as `:motion-lab`.

## Intended repository structure

```text
Motion-UI/
  motion/       Android motion library (Gradle module :motion)
  motion-ui/    optional configurable UI library (Gradle module :motion-ui)
  motion-lab/   standalone Android sample app (Gradle module :motion-lab)
  docs/         architecture, motion rules, research, and plans
  gradle/       wrapper and version catalog
  .github/      CI
  LICENSE       Apache-2.0
  README.md     project entry point
```

## How changes reach a consumer application

1. Make and test a reusable change in Motion.
2. Confirm the Motion CI workflow passes on the exact commit intended for adoption.
3. In the consumer repository, fetch Motion and check out that exact tested commit at its configured dependency path.
4. Build and test the consumer application with that dependency pin.
5. Commit the updated dependency pointer in the consumer repository with a message that identifies the SHA and reason.

A change may be prototyped in an application when useful, but the generic implementation must be reviewed in Motion first. Do not copy application code into Motion. Prefer an exact commit checkout over an unreviewed moving branch update.

## Original extraction candidates

The 2026-11-21 inventory identified these generic areas for review:

| Application area | Potential reusable behavior | Application-specific boundary |
|---|---|---|
| Shell/page navigation | page movement, settling, and generic gesture mechanics | route policy, screens, page content |
| Toolbar interaction | selection movement, press feedback, and transitions between states | toolbar composition, visual treatment, icons, and app state |
| Segmented-control press morph | reusable press/morph behavior | control content, labels, and product semantics |
| Sheet state | detents, drag, and source-to-sheet mechanics | screen state and product-specific content |

Treat this as a historical candidate list. Re-inspect current application source, call sites, and tests before planning any extraction. See [the AI adoption guide](AI_CONSUMER_ADOPTION.md).

## Planned phases

1. **Library reliability — complete:** core motion state, physics, validation, and interruption contracts have focused deterministic tests.
2. **Motion Lab — complete:** the standalone `:motion-lab` app is included in Gradle and required CI, with Material 3 Components / Interactions destinations and automatic system light/dark appearance.
3. **Reusable interaction extraction — next engineering phase:** evaluate generic behavior from real application use cases, move one capability at a time, and add focused tests plus a Lab example when useful. Do not assume every UI interaction belongs in Motion Core.
4. **Consumer workflow — documented:** contribution, exact-commit version pinning, and consumer dependency updates are described. Actual consumer dependency changes remain separate, explicitly reviewed changes.

Each phase should have a scoped plan, acceptance criteria, cost-aware verification, and an explicit list of deferred device checks. Do not expand a milestone into unrelated UI components, platform ports, or benchmarks.
