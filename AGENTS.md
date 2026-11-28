# Repository instructions for AI agents

Read this file and [project rules](docs/RULES.md) before making changes. Then read [README.md](README.md), [the framework overview](docs/FRAMEWORK_OVERVIEW.md), [platform support](docs/PLATFORM_SUPPORT.md), [the consumer adoption guide](docs/AI_CONSUMER_ADOPTION.md), [legacy prototype disposition](docs/LEGACY_PROTOTYPE_DISPOSITION.md), [motion principles](docs/MOTION.md), and the relevant implementation or design documents under `docs/`.

## Project boundaries

- This repository contains the reusable Motion framework. It is independent of the consuming application and must remain useful to unrelated apps.
- The current library artifact is an Android library for Jetpack Compose. Do not claim iOS, Windows, macOS, or Linux support until corresponding Gradle targets, platform implementations, tests, and CI exist.
- Never add consumer-specific domain dependencies, source code, assets, or assumptions to Motion.
- Motion Core (`:motion`) owns reusable motion and interaction behavior. The optional Motion UI (`:motion-ui`) layer owns configurable, cross-app component building blocks, but not a complete application framework, product screens, visual brand, navigation policy, or domain logic. Motion UI depends on Core; Core never depends on Motion UI. Follow [the framework overview](docs/FRAMEWORK_OVERVIEW.md) when deciding where code belongs.
- Compose remains the animation, rendering, and interaction foundation. Use Compose primitives such as `Animatable`, `spring`, and gesture APIs; do not invent a second general-purpose animation engine.
- Motion-specific behavior and interaction decisions must follow `docs/MOTION.md`. Do not change agreed, user-visible motion behavior without documenting the trade-off and obtaining the owner's agreement.
- Keep the library API small and intentional. Do not add public inspection hooks, configuration knobs, dependencies, or abstractions solely to simplify one test.

## Current milestone boundary

MOT0.1 is complete and merged into `main`.

MOT0.2 (standalone Motion Lab) is complete and merged into `main`. The delivered `:motion-lab` Android app, Components / Interactions tabs, and automatic system appearance are documented in [docs/MOT0.2_PLAN.md](docs/MOT0.2_PLAN.md). Treat that plan as historical acceptance evidence, not an active implementation queue. The former unbuilt `lab/` prototype was removed after a provenance and license review; see [the disposition record](docs/LEGACY_PROTOTYPE_DISPOSITION.md).

MOT0.3A (the `:motion-ui` theme/token foundation) is merged into `main`, and the MOT0.3B toolbar contract is accepted; the public toolbar API and the Motion Lab prototype (PR #26) remain under review.

The following remain outside the completed MOT0.2 scope and require separate planning/review:
- Changes to Motion Core physics or existing user-visible motion contracts.
- New public Core APIs without a separately reviewed reusable capability gap.
- New Motion UI components beyond the MOT0.3A theme/token foundation; each requires a scoped contract and review.
- Consumer-specific UI, consumer migrations, or dependency-pin updates.
- Kotlin Multiplatform, iOS, desktop, web, or platform expansion.
- Benchmarks or performance dashboards.
- Broad refactors unrelated to making the standalone Lab work.


## Motion Lab product and UI rules

- Treat Motion Lab as both a component showroom and an interaction test bench, not merely an animation gallery.
- Organize the top-level experience into two clear destinations: **Components** and **Interactions**, presented through a familiar Material 3 top app bar / tab pattern. Keep navigation consistent with the rest of the app; do not invent a custom toolbar or navigation system.
- **Components** are content-bearing UI building blocks that present information and context (for example cards, pages, and place-like content surfaces). **Interactions** are the controls and input patterns through which users act, provide input, or navigate (for example toolbars, buttons, dropdown menus, checkboxes, and gestures). Classify demos by what they teach; a component may contain interactions without making the categories interchangeable.
- Support automatic system light/dark appearance. Any additional pure-black/AMOLED appearance must be an explicit optional choice, not a replacement for system-following behavior. Preserve readable contrast and semantic colors in every scheme.
- Predictive-back blur is optional progressive enhancement only. Prefer platform/AndroidX predictive-back and Compose integration; use blur only where supported, keep it subtle, and provide a graceful no-blur fallback. Never build a custom back-navigation framework to demonstrate the effect.
- Reuse Compose, Material 3, AndroidX, and existing Motion APIs before adding abstractions. Do not rebuild standard controls, navigation, blur, or animation primitives that the platform already provides. Add a new Motion abstraction only when a concrete reusable gap is demonstrated and separately justified.
- Keep demo sections focused and independently understandable. Explain the component or interaction, show its context, and provide a direct way to exercise it.

## Implementation and test discipline

- Prefer a failing regression test before changing production code.
- For animation tests, use a deterministic `MonotonicFrameClock` and advance frames deliberately. Do not use wall-clock sleeps or timing-dependent assertions.
- Test observable behavior: final anchor, current bounds, visibility, open/closed intent, and whether motion settles. Do not expose internal velocity as public API just for assertions.
- Retarget from current animated values and preserve each `Animatable`'s current velocity when starting a replacement spring.
- New commands and gestures must supersede stale work. Check pending coroutines and initial placement races, not only the happy path.
- Validate non-finite numeric input before mutating state or launching work. Do not silently turn `NaN` or infinity into a plausible value.
- Keep intentional finite high-velocity clamps; validate raw input before clamping.
- Preserve legitimate zero-size/transient layout anchors if validating `MorphAnchor`; reject invalid geometry rather than silently normalizing it.
- Never weaken or delete a regression assertion merely to make CI green.
- For sample-app code, test the interaction contracts that are owned by the sample; do not duplicate Motion Core's internal tests in the Lab.

## AI-assisted planning and implementation

- Verify the current branch, submodule commit (when relevant), open PRs, source tree, and latest CI before relying on earlier plans or chat context.
- For consumer adoption work, follow [docs/AI_CONSUMER_ADOPTION.md](docs/AI_CONSUMER_ADOPTION.md). The first deliverable is an evidence-based migration plan in the consumer repository, not an automatic code migration or dependency-pin update.
- MOT0.2 is complete; use [docs/MOT0.2_PLAN.md](docs/MOT0.2_PLAN.md) as historical acceptance evidence. Keep Motion Lab as a consumer of Motion's public API; do not let the sample app dictate or leak app-only requirements into Core.
- Inspect existing APIs before proposing new ones. Classify each candidate as direct reuse, app-side composition, a reusable Motion gap, or app-specific behavior.
- Prefer adapting existing components and libraries over rebuilding equivalents. Avoid duplicating spring constants, gesture physics, or component implementations in consuming apps.
- Do not broaden platform support as incidental cleanup. A platform proposal must identify source-set boundaries, platform APIs, tests, and CI requirements first.

## Cost-aware verification

1. **Plan before testing.** For each task, identify the changed modules, acceptance criteria, smallest relevant checks, expensive checks, and evidence that counts as a pass.
2. **Use focused local checks.** Run affected tests, formatting, lint, or builds during implementation. Do not run full CI after every small edit. For documentation-only changes, skip irrelevant local code tests.
3. **Batch changes and commits.** Complete a coherent set of source, test, configuration, and documentation changes before pushing. Do not commit or push per file or per micro-fix. Publish one atomic commit for the batch.
4. **Publish multi-file changes atomically.** Do not call the GitHub Contents API once per file on a CI-enabled branch: each write creates a commit and can trigger another workflow. Use one local Git commit or one Git Data API blobs/tree/commit/ref sequence.
5. **Budget complete CI runs.** Default to one full required workflow for a coherent batch. A second is allowed only after the first exposes an actionable failure and all known fixes are consolidated. Obtain owner approval before further full runs unless already agreed in the task plan.
6. **Do not use CI as an interactive debugger.** Let an active run finish before pushing more work unless an urgent fix is necessary; then consolidate the fix. Inspect failure logs and prefer rerunning only the failed job on the same SHA where supported. Never create no-op commits to trigger CI.
7. **Reuse passing results.** Record SHA and environment. Do not repeat checks if relevant code/dependencies/configuration are unchanged; do not rerun successful jobs without a technical reason.
8. **Limit infrastructure retries.** After an infrastructure/environment failure, allow at most one retry with a materially different setup. If it fails again, stop and mark it BLOCKED.
9. **Protect required checks.** Do not add workflow-level path filters or skip-CI markers. The required `build` check must always report; the docs-only fast path in [docs/CI.md](docs/CI.md) skips steps and the emulator job with job-level conditions, which was verified (a skipped job counts as passing).
10. **Ask before expanding verification.** Obtain owner approval before substantially widening the test matrix or exceeding the CI budget.
11. **Keep device checks separate.** Real-device, live-source, and manual checks remain NOT TESTED or DEFERRED until performed.
12. **Report exact evidence.** State workflow run, tested commit SHA, pass/fail results, checks not run, and blockers. Never claim an unrun or uninspected check passed.

## Git and pull request rules

- Work on a focused branch and use a pull request; do not push development work directly to `main`.
- Keep related changes together and commits understandable.
- For any future milestone, keep implementation PRs in draft until their acceptance criteria are covered and the complete required CI workflow passes on the exact final branch head.
- Do not merge, release, publish, tag, or bump a consuming app's submodule unless explicitly requested.
- Update `CONTINUE.md`, implementation plans, and PR descriptions when branch state or remaining work materially changes.

## AI usage and cost policy

Follow [docs/AI_USAGE_POLICY.md](docs/AI_USAGE_POLICY.md) for model selection, ChatGPT and Claude workflows, multi-agent coordination, context/token efficiency, tool usage, budget controls, privacy, and cost-aware verification.

Repository-specific rules in this file supplement the shared policy; they do not replace it. In particular:
- Use one primary owner agent per coherent task. Bring in a second model only for a clearly independent review or specialist contribution.
- Choose the smallest capable model and escalate based on task risk or demonstrated difficulty. Claude Opus 5.5 is for demanding work, not routine edits.
- Treat subscription allowances and API billing as separate. Do not enable paid API usage, buy credits, or incur open-ended spend without owner approval.
- Keep context focused, reuse valid evidence, and stop repetitive or unproductive agent/tool loops.
- Attribute AI work in the PR's "Tool and tier" section with a simple model label (Opus 5.5, Sonnet 5.5, Haiku 5.5, ChatGPT Free); commit trailers only when truly necessary. See "AI attribution" in [the policy](docs/AI_USAGE_POLICY.md).
- If the active tool or model has a guide under [docs/agent-guides/](docs/agent-guides/), read it as supplementary advice; it never overrides the policy or this file.
