# Motion roadmap to 1.0

**Recorded:** 2026-11-28, after a code audit of `main` (`51420fd`) and the open PRs #26, #28 and #29.
**Authority:** subordinate to [AGENTS.md](../AGENTS.md), [RULES.md](RULES.md) and [MOTION.md](MOTION.md). Each milestone still needs its own scoped contract and owner approval before code, as `AGENTS.md` requires. This roadmap is the sequence, not the approval.
**Task routing:** tiers O/S/H/F per [AI_USAGE_POLICY.md](AI_USAGE_POLICY.md) §2. Live task status is in [CONTINUE.md](../CONTINUE.md); ready-made prompts are in [CHATGPT_HANDOFF.md](CHATGPT_HANDOFF.md).

## Definition of done (Motion 1.0)

1. **Motion UI delivers its planned scope** (FRAMEWORK_OVERVIEW.md, MOTION_UI_STYLE_ARCHITECTURE.md):
   - the animated toolbar;
   - card and sheet building blocks;
   - a page frame with header-action slots;
   - message/feedback surfaces.
   
   Every component supports at least two style profiles. Glass is optional and depends on an owner decision (M7).
2. **The public API is intentional and checked:**
   - explicit API mode is on in `:motion` and `:motion-ui`;
   - every public declaration has KDoc;
   - a binary-compatibility check runs in CI.
3. **Tests:**
   - every public API has deterministic tests;
   - each component has Compose UI tests, including at least one with animations enabled and a controlled `mainClock`.
4. **Motion Lab** shows every component and interaction through the public API only. No prototype copies remain.
5. **Documentation:** README, the module READMEs and the consumer adoption guide match the delivered API.
6. **Owner sign-off:**
   - a manual device pass (TalkBack, haptics, reduced motion, RTL, large font), recorded in a checklist;
   - the 1.0 tag.

Not part of done: platform expansion (iOS/desktop), consumer migrations (separate repos), benchmarks, and the optional Lab ideas in MOTION_LAB_PLAN.md.

## Code health at the start (audit, 2026-11-28)

- **Size and tests:**
  - `:motion` is 12 files, about 3,000 lines, with 64 JVM tests, strong on physics, MotionValue and Morph motion.
  - `:motion-ui` is the theme/token foundation only: 1 file, 3 tests.
  - `:motion-lab` has 6 demos in one 867-line `MainActivity.kt`, with no tests on `main`; PR #26 adds the toolbar prototype and 16 UI tests.
- **Public API is accidental:** explicit API mode is off, so everything is public. Mutable globals are public: `MotionTime.scale`, `Haptic.intensity`, and `MotionValue.restPoints`/`slowFlick`/`fastFlick`. Likely-internal types are also public (`MorphGeometry`, `MorphFrame`, `MorphHandOff`, `MorphDefaults`).
- **Untested public APIs:** `EdgeBlur`/`scrollEdgeBlur`, `BlurCrossfade`, `PageSpring`, the `Dial` composable, the `Morph`/`SpringMorph` composables, `MorphSettings`, `MorphStyle`, `rememberReducedMotion`. There are no Compose UI tests on `main`.
- **Validation gaps:** `MotionSpring` accepts +Infinity; `DialScale`, `EdgeBand` and `MorphStyle` have no finite checks. `requireFinite` is duplicated in two files.
- **Build:** `:motion` uses `androidx.lifecycle.compose` (`AmbientClock.kt`) without declaring the dependency.
- **Large files:** `SpringMorph.kt` (912 lines) and the Lab's `MainActivity.kt` (867). Split them only when a milestone touches them; no standalone refactor.
- **No TODO/FIXME markers.** Spotless/ktlint is enforced. CI on `main` is a single fast job; PR #26 adds the emulator job.

## Milestones

### M0: land in-flight work (now)

| ID | Task | Tier | Depends on |
|---|---|---|---|
| M0-1 | Owner review and merge of #28 (docs) and #29 (AmbientClock, Core behavior change, O2 approved) | Owner | — |
| M0-2 | S5: #26 prototype fixes (B4, B6) | S | — |
| M0-3 | S6: emulator job required for code PRs, docs-only skip (Q3 decided) | S | #26 merged or on #26 |
| M0-4 | S7: resolve #26 doc conflicts with refreshed `main` | S | M0-1 |
| M0-5 | Owner: merge #26 as the Lab prototype, so the M1 work builds on `main` | Owner | M0-2…M0-4 |
| M0-6 | H1, H6–H9 from CONTINUE.md | H/F | as listed |

### M1: toolbar public API (completes MOT0.3B)

| ID | Task | Tier | Depends on |
|---|---|---|---|
| M1-1 | O3: smallest public toolbar API for `:motion-ui`. Covers stable tab IDs, host progress, drag that follows the finger (Q4), direct far jumps (Q5), B1 settle hand-off retargeting from current values, B3 same-pass measure and placement, the contextual action, style-profile API, and semantics. Written as an API doc plus a type sketch. | O | M0-5 |
| M1-2 | Owner approves the M1-1 API | Owner | M1-1 |
| M1-3 | Implement the component in `:motion-ui`, with unit tests for geometry, state and cancellation | S | M1-2 |
| M1-4 | Compose UI tests: semantics, focus, RTL, drag, cancel, and one animations-enabled test with `mainClock` (closes B5) | S | M1-3 |
| M1-5 | Switch the Lab to the public component with two profiles; delete the prototype files | S | M1-3 |
| M1-6 | Opus review of the M1-3…M1-5 diff | O | M1-5 |
| M1-7 | Owner manual pass with the `MOT0.3B_MANUAL_VALIDATION.md` checklist | Owner | M1-5 |

Exit: MOT0.3B acceptance criteria met using the public API.

### M2: Core API hardening (can run alongside M1; touches `:motion`, not `:motion-ui`)

| ID | Task | Tier | Depends on |
|---|---|---|---|
| M2-1 | Public/internal classification of every `:motion` declaration. Propose replacements for the public mutable globals (keep `MotionTime.scale` for Lab slow motion behind an explicit API, or move it to a test/debug hook). | O | — |
| M2-2 | Owner approves M2-1. This is a breaking change for consumers; document the migration. | Owner | M2-1 |
| M2-3 | Turn on explicit API mode in `:motion` and apply M2-1; declare `lifecycle-runtime-compose` explicitly | S | M2-2 |
| M2-4 | Finite validation: `MotionSpring`, `DialScale`, `EdgeBand`, `MorphStyle`, each with a failing test first. Share one internal `requireFinite`. | S | — |
| M2-5 | Deterministic tests for the untested public APIs listed above | S | — |
| M2-6 | Same as M2-3 for `:motion-ui` | S | M1-3 |

### M3: card and sheet building blocks

| ID | Task | Tier | Depends on |
|---|---|---|---|
| M3-1 | Contract doc (like MOT0.3B_TOOLBAR_PLAN.md) for card and sheet. Decide whether the sheet detent/presence state holder named in MOTION.md is a Core gap or UI-only. | O | M1-2 |
| M3-2 | Owner approves M3-1 | Owner | M3-1 |
| M3-3 | Implement, with unit and UI tests and two style profiles | S | M3-2 |
| M3-4 | Lab demos under Components (card, sheet), through the public API | S | M3-3 |
| M3-5 | Opus review of the diff | O | M3-4 |

### M4: page frame, header actions, message surfaces

Same pattern as M3:
- M4-1 contract (O), including typography and motion token names for `MotionUiTokens`;
- M4-2 owner approval;
- M4-3 implementation and tests (S);
- M4-4 Lab demos (S);
- M4-5 review (O).

### M5: Lab completeness

| ID | Task | Tier |
|---|---|---|
| M5-1 | Demos for public APIs no demo exercises: `SpringMorph`/`MorphMotion`, `BlurCrossfade`, `rememberAmbientClock`, `scrollEdgeBlur`, `ProvideHaptics` | S |
| M5-2 | Split `MainActivity.kt` into one file per demo as part of M5-1 (no behavior change) | S |
| M5-3 | Reduced-motion preview toggle in the Lab (from the MOTION_LAB_PLAN.md ideas; the only one needed for done) | S |

### M6: release readiness (1.0)

| ID | Task | Tier | Depends on |
|---|---|---|---|
| M6-1 | Owner approves a binary-compatibility check plugin (new build dependency) | Owner | — |
| M6-2 | Add the check to CI; dump the API baseline | S | M6-1, M2-3, M2-6 |
| M6-3 | KDoc for every public declaration; README and module README refresh | S | M2–M4 |
| M6-4 | Consumer adoption guide refresh against the final API | S | M6-3 |
| M6-5 | Final Opus review of the public API and docs | O | M6-2…M6-4 |
| M6-6 | Owner device pass (TalkBack, haptics, reduced motion, RTL, large font) and 1.0 tag | Owner | M6-5 |

### M7: optional glass/material wrappers

Owner decision first: Haze's exact version, licence, maintenance, compatibility and no-blur fallback. If approved: contract (O), implementation with fallback (S), Lab demo (S). If declined, M7 is dropped and done is unaffected.

## Order and parallelism

- **Critical path:** M0 → M1 → M3 → M4 → M6.
- **M2** runs alongside M1. M2-4 and M2-5 need no approval and can start now.
- **M5** fits between milestones.
- Run one Sonnet session per task row. Rows that touch the same files run one at a time.

## Owner decisions still needed (in order of need)

1. Merge #28, #29 (M0-1) and #26 (M0-5).
2. Approve the toolbar API (M1-2).
3. Approve the Core public/internal split and mutable-globals plan (M2-2). This one is breaking.
4. Approve the card/sheet contract (M3-2), then the page-frame and message-surface contract (M4-2).
5. Approve a binary-compatibility plugin (M6-1).
6. Decide on Haze/glass (M7).
7. Device pass and the 1.0 tag (M6-6).
