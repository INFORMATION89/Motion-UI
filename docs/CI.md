# Continuous Integration

## Required workflow

The `Build` workflow runs for every push to `main` and every pull request. It has three jobs:

- **`changes`** is a few-second job that uses plain `git diff base...head` to decide whether the change touches anything other than documentation. Documentation means `**/*.md`, `docs/**`, and `LICENSE*`; everything else, including `.github/**`, is code. A push to `main` always counts as code.
- **`build`** (job id `verify`) runs Spotless, Core and Motion UI unit tests, Motion Lab unit tests, and release builds for the reusable libraries. **Its display name must stay exactly `build`: branch protection on `main` requires a status check with that name.** Never rename it.
- **`Compose UI tests · API 35 emulator`** (job id `instrumentation`) boots the API 35 Pixel 2 emulator and runs `:motion-lab:connectedDebugAndroidTest`. This task builds the debug app and instrumentation APK itself, so `build` does not duplicate `:motion-lab:assembleDebug`.

For code changes, `build` and the emulator job run in parallel after `changes`. The split exposes formatting, unit-test, and release-build failures without waiting for emulator startup. The emulator job is required evidence for code pull requests.

### Documentation-only fast path

When `changes` reports `code=false`:

- `build` still runs and reports. Its checkout, JDK, Gradle setup, and Gradle step are skipped with step-level `if`, so the job succeeds in seconds. The required check therefore always reports.
- The emulator job is skipped through `needs: changes` and `if: needs.changes.outputs.code == 'true'`. GitHub treats a skipped job as passing for required status checks.

Because the skipping is done with job and step conditions rather than workflow-level `paths` filters, no required check is left pending. If `changes` itself fails, `build` is skipped and the pull request cannot merge until it is rerun, which is the safe direction.

### PR hygiene

The separate `PR hygiene` workflow (`.github/workflows/pr-hygiene.yml`, `ci/pr_hygiene.py`) runs on pull request open, edit, synchronize, and reopen, with `contents: read`, per-PR concurrency, and a 5-minute timeout. It fails when:

- a heading from `.github/pull_request_template.md` is missing from the PR description or has no content (HTML comments do not count), or
- a commit in `base..head` that touches non-documentation files carries a `[skip ci]`, `[ci skip]`, `[no ci]`, `[skip actions]`, `[actions skip]`, or `skip-checks: true` marker.

The script uses only the Python standard library and receives the PR body through an environment variable. Run its tests with `cd ci && python3 -m unittest`. Whether `pr-hygiene` is a required check is a branch-protection setting owned by the repository owner; it is not required by default.

### Timeouts and cancellation

`build` has a 20-minute timeout. The emulator job has a 35-minute timeout to allow for Android image setup and emulator boot while preventing an indefinitely occupied runner. The workflow concurrency group cancels superseded runs for the same pull request or branch.

Before starting the emulator, CI attempts to grant the runner read/write access to `/dev/kvm` when the hosted VM exposes it, then records the device permissions and runner groups. The emulator action uses automatic Linux hardware-acceleration detection. When KVM is absent or remains inaccessible, the workflow explicitly enables the tested TCG fallback instead of failing emulator startup. Avoid forcing TCG on runners where KVM is available: it disables VM acceleration and makes boot substantially slower.

## Run-volume discipline

The workflow is triggered by every push to an open pull-request branch and every push to main. Each commit pushed to a PR can therefore create another complete workflow run. The concurrency group cancels a superseded run, but it does not debounce pushes or prevent a new run record; work already started may be wasted.

Keep the required workflow unchanged and control run volume through the development process: batch related changes, publish one atomic multi-file commit per coherent batch, and inspect the resulting workflow before making another push. Do not use one GitHub Contents API write per file for a multi-file change, because each write creates a separate commit. Do not use skip-CI markers on code commits (the PR hygiene check rejects them) and do not add workflow-level `paths` filters: a workflow that never starts can leave a required check pending. The docs-only fast path above uses job-level conditions instead, which was verified against branch protection: the required `build` check always reports, and skipped jobs count as passing.

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
