# Plan 002: Enable JUnit Platform in convention plugins

> **Executor instructions**: Follow step by step. Verify each step. On STOP conditions, report — do not improvise. Update `plans/README.md` when done.
>
> **Drift check**: `git diff --stat 1f293cf..HEAD -- build-logic/convention/src/main/kotlin core/battery/build.gradle.kts core/model/build.gradle.kts`

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: tests | dx
- **Planned at**: commit `1f293cf`, 2026-07-21

## Why this matters

CI runs `./gradlew test`. Modules with JUnit 5 (`org.junit.jupiter.api.Test`) only execute when `useJUnitPlatform()` is set. `:core:battery` declares Jupiter + Truth and has `BatterySnapshotTest`, but its `build.gradle.kts` does **not** call `useJUnitPlatform()`. Other modules set it ad-hoc. Result: green CI can mean “0 tests discovered” for PD/charger logic that feeds Active vs PendingConditions. Centralize the flag so every new test module works.

## Current state

- `core/battery/build.gradle.kts` — has `testImplementation(libs.junit.jupiter)` and Truth; **no** `useJUnitPlatform()`.
- Modules that **do** set it (copy pattern from these): `core/model`, `core/capability`, `core/control`, `core/device`, `feature/home/impl` — each has:
  ```kotlin
  tasks.withType<Test>().configureEach { useJUnitPlatform() }
  ```
- `build-logic/convention/src/main/kotlin/KotlinAndroid.kt` and `AndroidLibraryConventionPlugin.kt` — configure JVM 17 etc., but **do not** configure Test tasks.
- `BatterySnapshotTest.kt` uses `@Test` from `org.junit.jupiter.api.Test`.

## Commands you will need

| Purpose | Command | Expected |
|---------|---------|----------|
| Battery tests only | `./gradlew :core:battery:test --info 2>&1 \| rg -i 'BatterySnapshot|tests completed|0 tests'` | Non-zero tests executed for BatterySnapshot |
| All unit tests | `./gradlew test` | BUILD SUCCESSFUL |
| Count battery results | `ls core/battery/build/test-results/testDebugUnitTest/TEST-*.xml 2>/dev/null \| wc -l` | ≥ 1 |

## Scope

**In scope**:
- `build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt`
- `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` (if it builds unit tests)
- `build-logic/convention/src/main/kotlin/JvmLibraryConventionPlugin.kt` (if used for pure JVM modules)
- Optionally remove redundant per-module `useJUnitPlatform()` blocks from `core/model`, `core/capability`, `core/control`, `core/device`, `feature/home/impl` **after** convention works (cleanup only; keep if you prefer leave-duplicates)
- Do **not** add empty tests elsewhere

**Out of scope**: Writing new battery cases (plan 003/005); instrumented tests; adding mockk.

## Git workflow

- Branch: `advisor/002-junit-platform-convention`
- Commit: `test(build-logic): enable JUnit Platform for all unit tests`

## Steps

### Step 1: Baseline — prove battery may run 0 tests (optional if already known)

```bash
./gradlew :core:battery:cleanTest :core:battery:test
ls core/battery/build/test-results/testDebugUnitTest/ 2>/dev/null || true
```

Note whether `TEST-*.xml` exists.

### Step 2: Add JUnit Platform to Android library convention

In `AndroidLibraryConventionPlugin.kt` (after `android { ... }` / kotlin config), add:

```kotlin
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

Ensure import: `import org.gradle.api.tasks.testing.Test` and `import org.gradle.kotlin.dsl.withType` if needed.

Mirror for application and jvm-library convention plugins that run unit tests.

**Verify**: `./gradlew :core:battery:test` produces `TEST-com.chargepilot.core.battery.BatterySnapshotTest.xml` (or similar) and log shows tests completed > 0.

### Step 3: Full suite

```bash
./gradlew test
```

**Verify**: BUILD SUCCESSFUL. If previously-hidden failures appear in battery tests, **fix those tests or production bugs** (do not skip). Prefer fix production if tests are correct.

### Step 4: Optional cleanup

Remove duplicate `tasks.withType<Test>…` from individual modules that now inherit convention.

**Verify**: `./gradlew :core:capability:test :core:control:test :core:battery:test` still SUCCESS.

## Test plan

- Existing `BatterySnapshotTest` must execute (≥4 tests per file content).
- No new test file required in this plan.

## Done criteria

- [ ] Convention plugins call `useJUnitPlatform()` for unit `Test` tasks
- [ ] `:core:battery:test` produces JUnit XML and runs BatterySnapshot tests
- [ ] `./gradlew test` exits 0
- [ ] `plans/README.md` row 002 → DONE

## STOP conditions

- Enabling platform causes mass failures in non-Jupiter modules → investigate dual JUnit4/5; report rather than force-disable.
- Convention plugin API differs from excerpts → adapt to current plugin structure, still centralize flag.

## Maintenance notes

- New modules with Jupiter tests no longer need per-module boilerplate.
- Reviewer: confirm battery XML artifacts exist in CI logs after merge.
