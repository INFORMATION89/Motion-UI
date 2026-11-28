# Continuous Integration

## Required workflow

The `Build` workflow runs two independent jobs in parallel for every push to `main` and every pull request:

- **Fast checks** runs Spotless, Core and Motion UI unit tests, Motion Lab unit tests, and release builds for the reusable libraries.
- **Compose UI tests** boots the API 35 Pixel 2 emulator and runs `:motion-lab:connectedDebugAndroidTest`. This task builds the debug app and instrumentation APK itself, so the fast job does not duplicate `:motion-lab:assembleDebug`.

Both jobs are required evidence for a green workflow. The split exposes formatting, unit-test, and release-build failures without making those checks wait for emulator startup, while the emulator suite runs at the same time.

The fast job has a 20-minute timeout. The emulator job has a 35-minute timeout to allow for Android image setup and emulator boot while preventing an indefinitely occupied runner. The workflow concurrency group cancels superseded runs for the same pull request or branch.

Before starting the emulator, CI attempts to grant the runner read/write access to `/dev/kvm` when the hosted VM exposes it, then records the device permissions and runner groups. The emulator action uses automatic Linux hardware-acceleration detection. When KVM is absent or remains inaccessible, the workflow explicitly enables the tested TCG fallback instead of failing emulator startup. Avoid forcing TCG on runners where KVM is available: it disables VM acceleration and makes boot substantially slower.

## Run-volume discipline

The workflow is triggered by every push to an open pull-request branch and every push to main. Each commit pushed to a PR can therefore create another complete workflow run. The concurrency group cancels a superseded run, but it does not debounce pushes or prevent a new run record; work already started may be wasted.

Keep the required workflow unchanged and control run volume through the development process: batch related changes, publish one atomic multi-file commit per coherent batch, and inspect the resulting workflow before making another push. Do not use one GitHub Contents API write per file for a multi-file change, because each write creates a separate commit. Do not add path filters or skip-CI markers without first validating required-check and branch-protection behavior; skipped workflows can leave required checks pending.

See [AI Usage and Cost Policy](AI_USAGE_POLICY.md#7-cost-aware-verification-and-ci) and [repository agent instructions](../AGENTS.md#cost-aware-verification) for the mandatory run budget, failure diagnosis, and approval rules. Cancelled workflow counts do not establish billable minutes or monetary cost.

## Gradle performance settings

The root `gradle.properties` enables these settings, and CI must exercise them on every run:

- `org.gradle.parallel=true`: independent subproject tasks can execute concurrently.
- `org.gradle.caching=true`: Gradle can reuse cacheable task outputs.
- `org.gradle.configuration-cache=true`: the build must remain Configuration Cache compatible. Gradle's default problem mode is fail; do not change it to warning mode just to make CI pass.

`gradle/actions/setup-gradle` manages the Gradle User Home cache, including downloaded dependencies and the local build cache. Keep this as the single Gradle cache owner; do not add a second overlapping cache for the same Gradle directories.

The Configuration Cache lives in the build workspace under `.gradle/configuration-cache`. GitHub-hosted jobs use separate ephemeral workspaces, so this workflow does not claim to reuse Configuration Cache entries between jobs or separate workflow runs. In CI it is a strict compatibility gate; local development builds can reuse the configuration entry while their workspace remains available. Gradle's parallel project execution is separately enabled through `org.gradle.parallel=true`.

Configuration-on-demand is intentionally not enabled: the required CI task sets involve all three modules, so it does not avoid configuring projects in this workflow. Avoid enabling incubating configuration optimizations without measuring a real benefit.

## Test coverage boundary

Passing CI proves formatting, compilation, unit behavior, and the automated Compose semantics/interaction assertions that run in the emulator. It does not replace manual validation with TalkBack, physical keyboard hardware, RTL visual inspection, large font scaling, contrast checks, reduced-motion preference changes, or physical haptic output. Record those as deferred until actually tested.
