# Plan 003: Characterization tests for control preconditions and strategies

> **Executor instructions**: Follow step by step. Verify each step. Update `plans/README.md` when done.
>
> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/domain core/control-writesettings core/control core/capability core/battery feature/home/impl/src/test`

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: plans/002-enable-junit-platform-convention.md
- **Category**: tests
- **Planned at**: commit `1f293cf`, 2026-07-21

## Why this matters

AGENTS.md requires unit tests for `core/control*` changes. The product’s safety story lives in `DefaultPreconditionChecker`, `WriteSettingsStrategy` allow-gates, and registry validation — all almost untested. `HomeViewModelBridgeTest` **duplicates** bridge policy instead of testing production code. Without characterization tests, plans 004–010 (ROM match, read-after-write, allowlist, Shizuku) will regress silently.

## Current state

- `core/domain/.../DefaultPreconditionChecker.kt` — PD / battery `>` / game checks; **no** `src/test`.
- `core/control-writesettings/.../WriteSettingsStrategy.kt` — `descriptorAllowsWrite`, `canWrite` gate; empty `src/test`.
- `core/control/.../ControlOrchestratorTest.kt` — good pattern (fake strategies, Truth, `runTest`).
- `feature/home/impl/.../HomeViewModelBridgeTest.kt` — local `bridge()` mirror of VM (lines 53–65).
- `core/capability/.../RegistrySerializationTest.kt` — parses asset with Json; does **not** call `BundledCapabilityRegistryLoader.validate`.
- Battery tests exist but need plan 002 to run.

**Conventions**: JUnit 5 + Truth; `kotlinx.coroutines.test.runTest`; package `com.chargepilot.<module>`; name tests `SubjectBehaviorTest`.

## Commands you will need

| Purpose | Command | Expected |
|---------|---------|----------|
| Domain tests | `./gradlew :core:domain:test` | SUCCESS, new tests run |
| Write-settings tests | `./gradlew :core:control-writesettings:test` | SUCCESS |
| Capability tests | `./gradlew :core:capability:test` | SUCCESS |
| Home tests | `./gradlew :feature:home:impl:test` | SUCCESS |
| Focused CI set + battery | `./gradlew :core:model:test :core:capability:test :core:control:test :core:domain:test :core:battery:test :core:control-writesettings:test` | SUCCESS |

## Scope

**In scope**:
- `core/domain/src/test/kotlin/.../DefaultPreconditionCheckerTest.kt` (create)
- `core/domain/build.gradle.kts` — ensure junit/truth + `useJUnitPlatform` if not via convention
- `core/control-writesettings/src/test/...` — allow-list / failure reason tests (Robolectric only if required; prefer pure descriptor failures first)
- `core/capability/src/test/...` — loader validate + resolve known profiles against bundled snapshot
- `feature/home/impl/src/test/...` — either extract `bridgeEngagedToActive` to a testable pure function under `core/domain` or test via package-visible helper; **delete** the duplicated mirror once real code is under test
- `core/battery` additional cases only if quick (chargerType / current threshold) — optional in this plan

**Out of scope**:
- Full Shizuku bind integration tests (plan 010)
- History DataStore tests (plan 013)
- Real device instrumentation
- Implementing product features (disclosure, revert)

## Git workflow

- Branch: `advisor/003-characterization-control-tests`
- Commit: `test(control): cover preconditions, write-settings gates, registry validate`

## Steps

### Step 1: DefaultPreconditionChecker tests

Create fakes:

```kotlin
// Minimal fakes implementing only what checker calls
class FakeBattery(
  var pd: Boolean = false,
  var level: Int? = 50,
) : /* inject into checker — if BatteryProvider is concrete class, use a test subclass or extract interface */

```

If `BatteryProvider` / `ForegroundDetector` are concrete `@Singleton` classes without interfaces, **do not** invent a large DI rewrite. Options (pick simplest that compiles):

1. Make checker accept functional interfaces / existing types with open methods, **or**
2. Test pure extraction: move `isMet` logic to `PreconditionEvaluator` object taking `(pd, level, isGame) ->` and unit-test that; keep `DefaultPreconditionChecker` as thin wrapper.

**Required cases**:
- empty preconditions → allMet true
- PdChargerPresent false → unmet contains PdChargerPresent
- BatteryLevelAbove(20) with level 20 under current `>` semantics → unmet (documents current behavior before plan 005)
- BatteryLevelAbove(20) with level 21 → met
- level null → unmet
- GameInForeground with knownGames set membership

**Verify**: `./gradlew :core:domain:test` — all new tests pass.

### Step 2: WriteSettingsStrategy allow-list characterization

Without device, test paths that do not need real Settings:

- descriptor missing `WRITE_SETTINGS_KEY` in `availableMethods` → `setEnabled` → `Failed(UNSUPPORTED_DEVICE)`
- namespace != `system` → UNSUPPORTED_DEVICE
- type != `int` → UNSUPPORTED_DEVICE  
- Use Robolectric `@Config(sdk=[26])` + ApplicationContext only if `canWrite` must be false → SPECIAL_ACCESS_NOT_GRANTED

Pattern: look at how `ControlOrchestratorTest` builds descriptors.

**Verify**: `./gradlew :core:control-writesettings:test`

### Step 3: Registry loader validate + bundled resolve

Add tests that:

1. Call validate logic (publicize `validate` for tests or test via `load()` with Context from Robolectric, **or** load JSON from classpath resource copy of asset).
2. Resolve profiles:
   - Samsung `SM-S9280` with One UI 6+ ROM → includes PAUSE_PD
   - Google Pixel → BATTERY_LIMIT_80 / ADAPTIVE
   - Model on negative list if present (POCO F7) → empty or guidance-only as JSON defines

**Verify**: `./gradlew :core:capability:test`

### Step 4: Kill bridge test theatre

Extract production bridge to something testable, e.g. in `core/domain`:

```kotlin
object ControlStateBridge {
  suspend fun fromEngaged(raw: ControlState, preconditions: List<Precondition>, checker: PreconditionChecker, previousActiveSince: Long?): ControlState
}
```

HomeViewModel calls it. Tests call it. Delete duplicated `bridge()` in `HomeViewModelBridgeTest`.

**Verify**: `./gradlew :feature:home:impl:test` + `:core:domain:test`

### Step 5: Full verification

```bash
./gradlew :core:domain:test :core:control-writesettings:test :core:capability:test :core:control:test :feature:home:impl:test :core:battery:test
```

## Test plan (summary)

| File | Cases |
|------|--------|
| DefaultPreconditionCheckerTest | PD/level/game combinations |
| WriteSettingsStrategyTest | allow-list failures |
| CapabilityRegistryLoader/resolve tests | validate + real asset matrix samples |
| ControlStateBridgeTest (or updated Home test) | Engaged→Active/Pending; preserve since when previousActive provided |

## Done criteria

- [ ] Precondition checker covered by real unit tests (not only comments)
- [ ] WriteSettings allow-list failures tested
- [ ] Bundled registry validate or resolve-from-asset tested
- [ ] No duplicated bridge mirror left in home tests
- [ ] Commands in Step 5 exit 0
- [ ] `plans/README.md` row 003 → DONE

## STOP conditions

- Robolectric cannot run on the agent environment → implement pure allow-list tests without Robolectric and report gap for canWrite.
- Extracting bridge requires multi-module API redesign beyond one pure function → stop and report design options.

## Maintenance notes

- New OEM preconditions must add checker tests.
- Plan 005 will flip `>` to `>=` — update the level==20 case then.
