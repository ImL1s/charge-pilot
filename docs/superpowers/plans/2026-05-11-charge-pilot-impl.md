# Charge Pilot — Implementation Plan

**Status:** v1 (drafted from `2026-05-11-charge-pilot-design.md`)
**Date:** 2026-05-11
**Spec:** `/Users/iml1s/Documents/mine/charge_pilot/docs/superpowers/specs/2026-05-11-charge-pilot-design.md`
**Author:** architect agent
**Scope hint:** This is a multi-week project. The plan optimizes for **a first working vertical slice** (Phase 0–3 deliver: a `play`-flavor APK that detects a Samsung Galaxy S24, shows the `PAUSE_PD_DURING_GAMING` capability card, walks the disclosure flow, opens official settings, and writes `pass_through` via WRITE_SETTINGS). Phase 4+ is breadth.

---

## Goal

Deliver `Charge Pilot` per the design spec: an Android-only, AGPL-3.0, dual-flavor (`play`, `full`) capability-detection-and-safe-control framework for charging-related system settings. The first executable slice must:

1. Build both flavors (`assemblePlayDebug`, `assembleFullDebug`).
2. Detect device on boot and render Home with a `DeviceProfileCard` and ordered `CapabilityCard` list driven by `:core:capability`'s registry JSON.
3. Implement the `OFFICIAL_GUIDANCE` and `WRITE_SETTINGS_KEY` strategies end-to-end, gated by the three-step disclosure flow.
4. Persist operation history in Proto DataStore with a one-tap revert.

`SHIZUKU_RPC` and `ROOT_SHELL` strategies are scaffolded in Phase 4 but only required to compile; their full integration with real devices lives in subsequent passes.

---

## Architecture

Now-in-Android style multi-module layout (already scaffolded as empty directories per `settings.gradle.kts`):

- `app/` — single Application + MainActivity, NavHost, product-flavor source sets `play` / `full`.
- `build-logic/convention/` — six Gradle convention plugins applied per module type.
- `core/` — 17 modules: `model`, `common`, `domain`, `data`, `datastore`, `device`, `capability`, `control`, `control-writesettings`, `control-shizuku`, `control-root`, `battery`, `foreground`, `designsystem`, `ui`, `testing`.
- `feature/` — 6 features (`home`, `brands`, `advanced`, `history`, `about`, `disclosure`), each split into `api` (nav keys + interfaces only) and `impl` (everything else).

Dependency rules:
- Features depend only on other features' `:api`.
- `:core:control-shizuku` and `:core:control-root` are bound into `ControlOrchestrator` only in the `full` flavor via `app/src/full/...` Hilt module.
- `:core:capability` owns `capabilities-v1.json`; `:core:data` owns the OTA fetch + cache.

---

## Tech Stack

Per the spec § 3, with concrete pinned-version *placeholders* (decisions flagged below):

| Layer | Choice | Notes |
|---|---|---|
| Language | Kotlin `2.1.20` (placeholder) | latest stable at time of write |
| Build | AGP `8.7.x` (placeholder), Gradle `8.10.2` (placeholder) | Version Catalogs + Convention Plugins |
| Compose | BOM `2025.03.00` (placeholder), Compose Compiler plugin (Kotlin 2.x) | Material 3 Expressive |
| Min / Target SDK | 26 / 36 | per spec |
| DI | Hilt `2.52` (placeholder) | + `androidx.hilt:hilt-navigation-compose` |
| Async | Coroutines `1.9.x`, Flow + Turbine | Turbine `1.2.x` |
| Serialization | `kotlinx.serialization` JSON `1.8.x` | for capability registry |
| DataStore | Proto DataStore `1.1.x` + protobuf-javalite | history + preferences |
| Logging | Timber `5.0.1` | gated by `BuildConfig.DEBUG` |
| Shizuku | `dev.rikka.shizuku:api`, `:provider` `13.x` | `full` only |
| Root | `com.github.topjohnwu.libsu:core` `5.x` | `full` only |
| Tests | JUnit5 `5.11.x`, Robolectric `4.13`, Compose UI Test, Truth | unit + Robolectric + UI |
| Static analysis | ktlint `1.4.x`, detekt `1.23.x` | PR-blocking |

> **All version pins above are placeholders pending Phase 0a decision.** See "Decisions still required."

---

## Phase organization

| Phase | Title | Vertical-slice goal |
|---|---|---|
| **0** | Build foundation | Wrapper + version catalog + convention plugins compile cleanly |
| **1** | Core domain + capability registry | `:core:model`, `:core:common`, `:core:capability` JSON loader green |
| **2a** | Device detection | `:core:device` resolves `DeviceProfile` from `Build.*` |
| **2b** | Battery + foreground | `:core:battery`, `:core:foreground` produce flows |
| **2c** | DataStore (preferences + history) | Proto schema compiles; repository round-trips |
| **2d** | Control core (interface + orchestrator) | `:core:control` compiles; OFFICIAL_GUIDANCE strategy works |
| **2e** | WRITE_SETTINGS strategy | `:core:control-writesettings` writes `pass_through` |
| **2f** | Shizuku scaffold | `:core:control-shizuku` compiles in `full`; binder check works |
| **2g** | Root scaffold | `:core:control-root` compiles in `full`; libsu probe works |
| **3** | Design system + core UI | `:core:designsystem`, `:core:ui` render M3E theme + cards |
| **4** | Features | `home`, `disclosure`, `brands`, `history`, `about`, `advanced` |
| **5** | App wiring + flavors | `:app` boots, navigates, both flavors assemble |
| **6** | Open-source housekeeping + CI | Repo legally + community-ready; CI gates merges |

Each phase ends with a runnable verification command. The fastest path to a "smoke-testable APK" is Phase 0 → 1 → 2a → 2c → 2d → 2e → 3 → minimal 4 (home + disclosure) → 5.

---

## Phase 0 — Build foundation

**Goal:** `./gradlew help` succeeds. Convention plugins are in place but no module yet applies them; root project resolves the version catalog without errors.

### Phase 0a — Decisions and version pinning  *(SERIAL, blocks everything)*

- [ ] **DECISION 0a.1**: Lock AGP / Kotlin / Compose Compiler / Compose BOM versions (placeholders above). User approval needed before Phase 0b.
- [ ] **DECISION 0a.2**: Lock Hilt, Coroutines, kotlinx-serialization, DataStore, Timber, Turbine, JUnit5, Robolectric, ktlint, detekt versions. User approval needed.
- [ ] **DECISION 0a.3**: Confirm `applicationId` (`io.charge_pilot.app`?), keystore alias, package namespace root (`io.charge_pilot.*`?). User approval needed.
- [ ] **DECISION 0a.4**: Confirm OTA registry URL pattern (e.g. `https://raw.githubusercontent.com/<org>/<repo>/main/registry/capabilities-v1.json`). User approval needed.

### Phase 0b — Gradle wrapper  *(PARALLEL with 0c, 0d)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/gradle/wrapper/gradle-wrapper.properties` — pin Gradle distribution URL.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/gradle/wrapper/gradle-wrapper.jar` — committed wrapper jar (run `gradle wrapper --gradle-version <X>` once locally).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/gradlew` and `/Users/iml1s/Documents/mine/charge_pilot/gradlew.bat` — wrapper scripts (mode `+x` on `gradlew`).

### Phase 0c — Version catalog  *(PARALLEL with 0b, 0d)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/gradle/libs.versions.toml` — every plugin alias referenced by root `build.gradle.kts:3-11` (`android-application`, `android-library`, `kotlin-android`, `kotlin-jvm`, `kotlin-serialization`, `compose-compiler`, `hilt`, `ksp`) plus all library coords used by convention plugins and modules. Skeleton:
  ```toml
  [versions]
  agp = "8.7.3"
  kotlin = "2.1.20"
  composeBom = "2025.03.00"
  hilt = "2.52"
  # ...
  [libraries]
  androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "androidxCoreKtx" }
  # ...
  [plugins]
  android-application = { id = "com.android.application", version.ref = "agp" }
  # ... (all eight from build.gradle.kts:3-11)
  [bundles]
  compose = ["androidx-compose-ui", "androidx-compose-ui-tooling-preview", "androidx-compose-material3"]
  ```

### Phase 0d — Convention plugin scaffolding  *(PARALLEL with 0b, 0c, all six plugins serial within this group only after their shared module is built)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/build-logic/settings.gradle.kts` — included build settings for `convention`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/build-logic/convention/build.gradle.kts` — applies `kotlin-dsl`, declares dependencies on AGP, Kotlin Gradle plugin, Compose Compiler plugin, KSP, plus `gradlePlugin { plugins { ... register six ids ... } }` block exposing `chargepilot.android.application`, `.android.library`, `.android.feature`, `.android.compose`, `.android.hilt`, `.android.flavors`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` — sets `compileSdk=36`, `minSdk=26`, `targetSdk=36`, JVM 17, default DSL block.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt` — same SDK/Java settings minus `targetSdk`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt` — applies `chargepilot.android.library`, adds Hilt, navigation-compose, `:core:ui`, `:core:designsystem`, `:core:model`, `:core:common`, `:core:domain` dependencies.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt` — enables Compose, applies `compose-compiler` plugin, pulls Compose BOM + bundle.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/build-logic/convention/src/main/kotlin/AndroidHiltConventionPlugin.kt` — applies KSP, adds `hilt-android` + `hilt-compiler`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/build-logic/convention/src/main/kotlin/AndroidFlavorsConventionPlugin.kt` — declares `flavorDimensions += "distribution"`, `productFlavors { create("play") { dimension = "distribution" }; create("full") { dimension = "distribution" } }`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/build-logic/convention/src/main/kotlin/ChargePilotExt.kt` — shared helpers (e.g. `Project.libs` accessor for the version catalog).

### Phase 0 verification

- [ ] Run from repo root: `./gradlew help --no-configuration-cache` (configuration-cache disabled first time so missing modules don't crash).
- [ ] Run: `./gradlew :build-logic:convention:assemble`. Must succeed.

> Phase 0 is **only complete** when both commands exit 0.

---

## Phase 1 — Core domain + capability registry

**Goal:** Pure-Kotlin (or Android-library-but-no-Android-API) modules compile and unit-test green.

### Phase 1a — `:core:common`  *(PARALLEL with 1b, 1c)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/common/build.gradle.kts` — `plugins { id("chargepilot.android.library") }`, no Android deps beyond coroutines.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/common/src/main/AndroidManifest.xml` — empty `<manifest .../>`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/common/src/main/kotlin/io/chargepilot/core/common/Dispatchers.kt` — `@Qualifier annotation class Dispatcher(val value: ChargePilotDispatcher)`, `enum class ChargePilotDispatcher { Default, IO, Main }`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/common/src/main/kotlin/io/chargepilot/core/common/Result.kt` — `sealed interface CpResult<out T> { data class Success<T>(val data: T); data class Error(val ex: Throwable); data object Loading }`, plus `Flow<T>.asResult()`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/common/src/main/kotlin/io/chargepilot/core/common/Logger.kt` — Timber wrapper with `BuildConfig.DEBUG` check delegated through a `LogConfig` interface so `:core:common` does not import per-module BuildConfig.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/common/src/test/kotlin/io/chargepilot/core/common/ResultTest.kt` — Turbine test for `asResult()`.

### Phase 1b — `:core:model`  *(PARALLEL with 1a, 1c)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/model/build.gradle.kts` — `plugins { id("chargepilot.android.library"); alias(libs.plugins.kotlin.serialization) }`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/model/src/main/AndroidManifest.xml` — empty manifest.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/model/src/main/kotlin/io/chargepilot/core/model/Manufacturer.kt` — `enum class Manufacturer { SAMSUNG, GOOGLE, ONEPLUS, XIAOMI, HONOR, HUAWEI, OTHER }`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/model/src/main/kotlin/io/chargepilot/core/model/DeviceProfile.kt` — `@Serializable data class DeviceProfile(val manufacturer: Manufacturer, val model: String, val codename: String, val androidApi: Int, val androidVersion: String, val romVersion: String?, val buildFingerprint: String)`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/model/src/main/kotlin/io/chargepilot/core/model/CapabilityType.kt` — enum per spec § 5.2.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/model/src/main/kotlin/io/chargepilot/core/model/ControlMethod.kt` — enum per spec.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/model/src/main/kotlin/io/chargepilot/core/model/Precondition.kt` — sealed interface with `PdChargerPresent`, `BatteryLevelAbove(percent)`, `GameInForeground(knownGames)`, plus `@Serializable` polymorphic discriminators.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/model/src/main/kotlin/io/chargepilot/core/model/ControlState.kt` — sealed interface per spec.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/model/src/main/kotlin/io/chargepilot/core/model/Evidence.kt` — enum per spec.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/model/src/main/kotlin/io/chargepilot/core/model/CapabilityDescriptor.kt` — full descriptor incl. `SettingsKey`, `IntentSpec`. Locked API:
  ```kotlin
  @Serializable
  data class CapabilityDescriptor(
      val type: CapabilityType,
      val availableMethods: List<ControlMethod>,
      val settingsKey: SettingsKey?,
      val officialIntent: IntentSpec?,
      val preconditions: List<Precondition>,
      val evidence: Evidence,
      val sourceUrl: String?,
      val verifiedDate: String?,
  )
  @Serializable data class SettingsKey(val namespace: String, val key: String, val type: String)
  @Serializable data class IntentSpec(val action: String, val component: String? = null, val fallbackPath: String? = null)
  ```
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/model/src/main/kotlin/io/chargepilot/core/model/OperationRecord.kt` — `@Serializable data class OperationRecord(val id: String, val capability: CapabilityType, val method: ControlMethod, val before: String?, val after: String?, val timestampMs: Long, val reverted: Boolean)`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/model/src/test/kotlin/io/chargepilot/core/model/CapabilityDescriptorJsonTest.kt` — round-trip a sample JSON; verifies serialization contract is the source of truth for the registry schema.

### Phase 1c — `:core:capability` (registry loader)  *(SERIAL: depends on 1a + 1b compiled)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/capability/build.gradle.kts` — depends on `:core:model`, `:core:common`, kotlinx-serialization-json.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/capability/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/capability/src/main/assets/capabilities-v1.json` — initial registry with the Samsung S24 / S25 `pause_pd` rule from spec § 5.4 plus Pixel 8/9 `BATTERY_LIMIT_80` (official-guidance only) so two cards render in the slice.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/capability/src/main/kotlin/io/chargepilot/core/capability/RegistrySchema.kt` — `@Serializable` wrapper types `RegistryFile(val schemaVersion: Int, val rules: List<Rule>)`, `Rule(val id: String, val matchers: Matchers, val capabilities: List<CapabilityDescriptor>)`, `Matchers(val manufacturer: Manufacturer, val modelRegex: String?, val minRomVersion: RomVersion?)`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/capability/src/main/kotlin/io/chargepilot/core/capability/RegistryLoader.kt` — `interface RegistryLoader { suspend fun load(): RegistryFile }` and `class AssetRegistryLoader(@ApplicationContext ctx: Context, json: Json)`. Reads `assets/capabilities-v1.json`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/capability/src/main/kotlin/io/chargepilot/core/capability/CapabilityRegistry.kt` — `class CapabilityRegistry(private val loader: RegistryLoader) { suspend fun resolve(profile: DeviceProfile): List<CapabilityDescriptor> }`. Matches by manufacturer + regex + ROM-version comparison.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/capability/src/main/kotlin/io/chargepilot/core/capability/SchemaValidator.kt` — pure-Kotlin schema check (no JSON-Schema lib): asserts every required field, enum values valid; throws `RegistrySchemaException`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/capability/src/test/kotlin/io/chargepilot/core/capability/RegistryMatchTest.kt` — feeds fake `DeviceProfile`s, asserts correct rules selected.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/capability/src/test/kotlin/io/chargepilot/core/capability/SchemaValidatorTest.kt` — corrupted JSON inputs, asserts validator rejects.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/capability/src/androidTest/assets/capabilities-v1.json` — test-only fixture (or share via testing module in Phase 4).

### Phase 1 verification

- [ ] `./gradlew :core:common:test :core:model:test :core:capability:test` — all green.
- [ ] `./gradlew :core:capability:assemble` — bundled JSON validates against schema in a Gradle task hooked into the `check` lifecycle (write a `RegistryValidationTask` in the convention plugin OR as a `tasks.register` block in the module — recommend the latter for simplicity in v1).

---

## Phase 2 — Platform-facing core modules

> The seven Phase-2 sub-phases are largely independent. Recommended parallel batches below.

### Phase 2a — `:core:device`  *(PARALLEL with 2b, 2c)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/device/build.gradle.kts` — depends on `:core:model`, `:core:common`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/device/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/device/src/main/kotlin/io/chargepilot/core/device/DeviceProfileSource.kt` — `interface DeviceProfileSource { fun current(): DeviceProfile }`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/device/src/main/kotlin/io/chargepilot/core/device/BuildDeviceProfileSource.kt` — pulls `Build.MANUFACTURER`, `Build.MODEL`, `Build.DEVICE`, `Build.VERSION.SDK_INT`, `Build.VERSION.RELEASE`, `Build.FINGERPRINT`, derives `Manufacturer` enum, calls `RomVersionParser`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/device/src/main/kotlin/io/chargepilot/core/device/RomVersionParser.kt` — parses `ro.build.version.oneui`, `ro.build.version.miui`, `ro.build.version.colorOS`, etc., via `SystemProperties` reflection or `Runtime.exec("getprop ...")` (pick one and document). Returns nullable `RomVersion(type: RomType, version: String)`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/device/src/main/kotlin/io/chargepilot/core/device/di/DeviceModule.kt` — Hilt `@Module @InstallIn(SingletonComponent::class)` binding `DeviceProfileSource`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/device/src/test/kotlin/io/chargepilot/core/device/RomVersionParserTest.kt` — Robolectric, fakes `SystemProperties` per shadow.

### Phase 2b — `:core:battery` + `:core:foreground`  *(PARALLEL with 2a, 2c)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/battery/build.gradle.kts`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/battery/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/battery/src/main/kotlin/io/chargepilot/core/battery/BatteryStateSource.kt` — `interface BatteryStateSource { fun observe(): Flow<BatteryState> }`, `data class BatteryState(val percent: Int, val isCharging: Boolean, val isPdCharger: Boolean, val chargerWatts: Int?)`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/battery/src/main/kotlin/io/chargepilot/core/battery/AndroidBatteryStateSource.kt` — `BatteryManager` + `ACTION_BATTERY_CHANGED` `BroadcastReceiver` wrapped in `callbackFlow`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/battery/src/main/kotlin/io/chargepilot/core/battery/di/BatteryModule.kt` — Hilt binding.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/battery/src/test/kotlin/io/chargepilot/core/battery/AndroidBatteryStateSourceTest.kt` — Robolectric.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/foreground/build.gradle.kts`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/foreground/src/main/AndroidManifest.xml` — empty (`PACKAGE_USAGE_STATS` is declared on `:app`).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/foreground/src/main/kotlin/io/chargepilot/core/foreground/ForegroundAppSource.kt` — `interface ForegroundAppSource { suspend fun current(): String?; fun observe(): Flow<String?> }`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/foreground/src/main/kotlin/io/chargepilot/core/foreground/UsageStatsForegroundSource.kt` — `UsageStatsManager.queryEvents()`-based; falls back to `null` if permission missing. Keep polling interval configurable.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/foreground/src/main/kotlin/io/chargepilot/core/foreground/UsageStatsPermissionChecker.kt` — `fun isGranted(): Boolean` via `AppOpsManager`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/foreground/src/main/kotlin/io/chargepilot/core/foreground/di/ForegroundModule.kt`.

### Phase 2c — `:core:datastore`  *(PARALLEL with 2a, 2b)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/datastore/build.gradle.kts` — pulls `androidx.datastore:datastore`, `protobuf-javalite`, applies `protobuf-gradle-plugin` (add to version catalog).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/datastore/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/datastore/src/main/proto/user_preferences.proto` — `syntax="proto3"; package io.chargepilot.core.datastore;` with `bool dynamic_color_enabled`, `string preferred_method`, `bool seen_disclosure_for_<capability>` repeated map, `bool ota_registry_enabled`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/datastore/src/main/proto/operation_history.proto` — `repeated OperationRecord records;` with all `OperationRecord` fields.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/datastore/src/main/kotlin/io/chargepilot/core/datastore/UserPreferencesSerializer.kt` — `object UserPreferencesSerializer : Serializer<UserPreferences>`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/datastore/src/main/kotlin/io/chargepilot/core/datastore/OperationHistorySerializer.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/datastore/src/main/kotlin/io/chargepilot/core/datastore/UserPreferencesRepository.kt` — `class UserPreferencesRepository @Inject constructor(private val store: DataStore<UserPreferences>) { val flow: Flow<UserPreferences>; suspend fun setDynamicColor(enabled: Boolean); ... }`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/datastore/src/main/kotlin/io/chargepilot/core/datastore/OperationHistoryRepository.kt` — `append(OperationRecord)`, `markReverted(id)`, `observe(): Flow<List<OperationRecord>>`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/datastore/src/main/kotlin/io/chargepilot/core/datastore/di/DataStoreModule.kt` — provides `DataStore<UserPreferences>` and `DataStore<OperationHistory>` singletons.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/datastore/src/test/kotlin/io/chargepilot/core/datastore/UserPreferencesRepositoryTest.kt` — Robolectric, temp DataStore.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/datastore/src/test/kotlin/io/chargepilot/core/datastore/OperationHistoryRepositoryTest.kt`.

### Phase 2d — `:core:control` (interface + orchestrator + OFFICIAL_GUIDANCE)  *(SERIAL: depends on 2a + 2c)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control/build.gradle.kts` — depends on `:core:model`, `:core:common`, `:core:device`, `:core:battery`, `:core:foreground`, `:core:datastore`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control/src/main/kotlin/io/chargepilot/core/control/ControlStrategy.kt` — locked contract:
  ```kotlin
  interface ControlStrategy {
      val method: ControlMethod
      suspend fun isAvailable(profile: DeviceProfile, descriptor: CapabilityDescriptor): Boolean
      suspend fun getCurrentState(descriptor: CapabilityDescriptor): ControlState
      suspend fun setEnabled(descriptor: CapabilityDescriptor, enabled: Boolean): ControlResult
      suspend fun reset(descriptor: CapabilityDescriptor): ControlResult
  }
  sealed interface ControlResult {
      data object Success : ControlResult
      data class Failed(val reason: FailureReason) : ControlResult
  }
  enum class FailureReason {
      UNSUPPORTED_DEVICE,
      SPECIAL_ACCESS_NOT_GRANTED,
      KEY_NOT_FOUND, KEY_NOT_WRITABLE,
      SHIZUKU_NOT_RUNNING, SHIZUKU_PERMISSION_DENIED,
      ROOT_DENIED,
      PRECONDITIONS_NOT_MET,
      UNKNOWN,
  }
  ```
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control/src/main/kotlin/io/chargepilot/core/control/PreconditionEvaluator.kt` — `class PreconditionEvaluator @Inject constructor(battery, foreground) { suspend fun evaluate(list: List<Precondition>): List<Precondition> /* unmet */ }`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control/src/main/kotlin/io/chargepilot/core/control/ControlOrchestrator.kt` — locked structure:
  ```kotlin
  @Singleton
  class ControlOrchestrator @Inject constructor(
      private val strategies: Map<ControlMethod, @JvmSuppressWildcards ControlStrategy>,
      private val prefs: UserPreferencesRepository,
      private val history: OperationHistoryRepository,
      private val evaluator: PreconditionEvaluator,
  ) {
      suspend fun pickStrategy(descriptor: CapabilityDescriptor): ControlStrategy?  // user pref → fallback chain
      suspend fun execute(descriptor: CapabilityDescriptor, enabled: Boolean): ControlResult
      suspend fun revert(opId: String): ControlResult
  }
  ```
  Fallback order per spec: `OFFICIAL_GUIDANCE → WRITE_SETTINGS_KEY → SHIZUKU_RPC → ROOT_SHELL`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control/src/main/kotlin/io/chargepilot/core/control/strategy/OfficialGuidanceStrategy.kt` — uses `IntentSpec` to launch `Settings.ACTION_*` intents; `setEnabled` returns `Success` only after intent is fired (it does not wait for outcome — the user makes the change manually).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control/src/main/kotlin/io/chargepilot/core/control/di/ControlModule.kt` — Hilt module that uses `@IntoMap @ControlMethodKey(ControlMethod.OFFICIAL_GUIDANCE) @Provides` style binding so each strategy module contributes itself. Locked Hilt pattern:
  ```kotlin
  @MapKey annotation class ControlMethodKey(val value: ControlMethod)

  @Module @InstallIn(SingletonComponent::class)
  abstract class OfficialGuidanceModule {
      @Binds @IntoMap @ControlMethodKey(ControlMethod.OFFICIAL_GUIDANCE)
      abstract fun bind(impl: OfficialGuidanceStrategy): ControlStrategy
  }
  ```
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control/src/test/kotlin/io/chargepilot/core/control/ControlOrchestratorTest.kt` — fake strategies, asserts fallback order and history-write side effect.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control/src/test/kotlin/io/chargepilot/core/control/PreconditionEvaluatorTest.kt`.

### Phase 2e — `:core:control-writesettings`  *(SERIAL: depends on 2d)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-writesettings/build.gradle.kts` — depends on `:core:control`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-writesettings/src/main/AndroidManifest.xml` — declares `<uses-permission android:name="android.permission.WRITE_SETTINGS"/>`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-writesettings/src/main/kotlin/io/chargepilot/core/controlws/WriteSettingsStrategy.kt` — wraps `Settings.System.putInt/getInt`, `Settings.Global.putInt/getInt` etc., gated by `Settings.System.canWrite(context)`; before-value captured for revert.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-writesettings/src/main/kotlin/io/chargepilot/core/controlws/WriteSettingsPermissionLauncher.kt` — `fun openWriteSettingsScreen(ctx: Context)` for UI.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-writesettings/src/main/kotlin/io/chargepilot/core/controlws/di/WriteSettingsModule.kt` — `@Binds @IntoMap @ControlMethodKey(WRITE_SETTINGS_KEY)`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-writesettings/src/test/kotlin/io/chargepilot/core/controlws/WriteSettingsStrategyTest.kt` — Robolectric, asserts read/write/revert round-trip plus permission-denied path.

### Phase 2f — `:core:control-shizuku`  *(PARALLEL with 2g)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-shizuku/build.gradle.kts` — depends on `:core:control`, `dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`. Marked as `full`-flavor-only via comment; binding is conditional in `:app` (see Phase 5).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-shizuku/src/main/AndroidManifest.xml` — `<provider android:name="rikka.shizuku.ShizukuProvider" android:authorities="${applicationId}.shizuku" android:multiprocess="false" android:enabled="true" android:exported="true" android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-shizuku/src/main/kotlin/io/chargepilot/core/controlshizuku/ShizukuClient.kt` — wraps `Shizuku.pingBinder()`, `Shizuku.checkSelfPermission()`, `Shizuku.requestPermission()` callbacks as suspend functions.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-shizuku/src/main/kotlin/io/chargepilot/core/controlshizuku/ShizukuStrategy.kt` — runs `cmd settings put system <key> <value>` via Shizuku's `newProcess()`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-shizuku/src/main/kotlin/io/chargepilot/core/controlshizuku/di/ShizukuModule.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-shizuku/src/test/kotlin/io/chargepilot/core/controlshizuku/ShizukuStrategyTest.kt` — uses fake `ShizukuClient`; full integration is manual on device.

### Phase 2g — `:core:control-root`  *(PARALLEL with 2f)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-root/build.gradle.kts` — depends on `:core:control`, `com.github.topjohnwu.libsu:core`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-root/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-root/src/main/kotlin/io/chargepilot/core/controlroot/RootClient.kt` — `Shell.getShell()` lifecycle wrapper.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-root/src/main/kotlin/io/chargepilot/core/controlroot/RootStrategy.kt` — `Shell.cmd("settings put system <key> <value>").exec()`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-root/src/main/kotlin/io/chargepilot/core/controlroot/di/RootModule.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/control-root/src/test/kotlin/io/chargepilot/core/controlroot/RootStrategyTest.kt` — fakes `Shell`.

### Phase 2 verification

- [ ] `./gradlew :core:device:test :core:battery:test :core:foreground:test :core:datastore:test :core:control:test :core:control-writesettings:test :core:control-shizuku:test :core:control-root:test`
- [ ] `./gradlew :core:control:assemble :core:control-writesettings:assemble :core:control-shizuku:assemble :core:control-root:assemble` — all four assemble.

---

## Phase 3 — Design system + reusable UI

### Phase 3a — `:core:designsystem`  *(SERIAL — blocks 3b)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/build.gradle.kts` — `plugins { id("chargepilot.android.library"); id("chargepilot.android.compose") }`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/src/main/res/font/fira_code_variable.ttf` — bundled font (download from upstream once during decision phase).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/src/main/res/font/fira_sans_regular.ttf`, `fira_sans_medium.ttf`, `fira_sans_bold.ttf`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/src/main/res/values/colors.xml` — fallback palette tokens.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/src/main/kotlin/io/chargepilot/core/designsystem/theme/Color.kt` — slate-800, green-500, etc., per spec § 4.2.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/src/main/kotlin/io/chargepilot/core/designsystem/theme/Type.kt` — Fira Code + Fira Sans typography.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/src/main/kotlin/io/chargepilot/core/designsystem/theme/Shape.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/src/main/kotlin/io/chargepilot/core/designsystem/theme/Motion.kt` — exposes `MotionScheme.expressive()`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/src/main/kotlin/io/chargepilot/core/designsystem/theme/ChargePilotTheme.kt` — `@Composable fun ChargePilotTheme(useDynamicColor: Boolean = true, darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)` wrapping `MaterialExpressiveTheme`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/src/main/kotlin/io/chargepilot/core/designsystem/component/CpButton.kt` — primary CTA with shape-morph press animation per spec § 4.2.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/src/main/kotlin/io/chargepilot/core/designsystem/component/CpStatusChip.kt`, `CpSourceChip.kt`, `CpCard.kt` — primitives reused by `:core:ui`.

### Phase 3b — `:core:ui`  *(SERIAL — blocks features)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/ui/build.gradle.kts` — depends on `:core:designsystem`, `:core:model`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/ui/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/ui/src/main/kotlin/io/chargepilot/core/ui/DeviceProfileCard.kt` — renders manufacturer, model, ROM, Android version.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/ui/src/main/kotlin/io/chargepilot/core/ui/CapabilityCard.kt` — accepts `CapabilityCardState` (sealed: `Unsupported`, `Idle`, `Pending(unmet)`, `Active(since)`); renders all four visual states from spec § 4.3.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/ui/src/main/kotlin/io/chargepilot/core/ui/DisclosureSheet.kt` — three-step disclosure UI with `BackHandler` to intercept predictive back per spec § 4.4 / § 4.5.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/ui/src/main/kotlin/io/chargepilot/core/ui/SourceAttributionRow.kt` — `Evidence` enum → label + chip color.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/ui/src/main/kotlin/io/chargepilot/core/ui/PreconditionChecklist.kt` — list with check/cross icons.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/ui/src/test/kotlin/io/chargepilot/core/ui/CapabilityCardTest.kt` — Compose UI test, four states render correctly.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/ui/src/test/kotlin/io/chargepilot/core/ui/DisclosureSheetTest.kt` — verifies first invocation shows all 3 steps, subsequent shows only step 3 (driven by `seenDisclosureFor` flag).

### Phase 3c — `:core:testing`  *(PARALLEL with 3b after 3a green)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/testing/build.gradle.kts` — depends on `:core:model`, JUnit5, Turbine, Compose UI Test.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/testing/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/testing/src/main/kotlin/io/chargepilot/core/testing/SampleProfiles.kt` — canned `DeviceProfile` for Galaxy S24, Pixel 9, OnePlus 13, etc.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/testing/src/main/kotlin/io/chargepilot/core/testing/SampleDescriptors.kt` — canned `CapabilityDescriptor` instances.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/core/testing/src/main/kotlin/io/chargepilot/core/testing/MainDispatcherRule.kt` — JUnit5 extension for coroutine tests.

### Phase 3 verification

- [ ] `./gradlew :core:designsystem:assemble :core:ui:assemble :core:testing:assemble`.
- [ ] `./gradlew :core:ui:test` — Compose UI tests green.

---

## Phase 4 — Feature modules

> Feature priority order (vertical slice first): `home` → `disclosure` → `about` → `history` → `brands` → `advanced`. Each `feature/<name>/api` is small (often a single nav-key file + nav-graph entry) and can be done in parallel with the corresponding `impl`.

### Phase 4a — `:feature:home` (api + impl)  *(SERIAL — first feature, end-to-end vertical slice)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/home/api/build.gradle.kts` — `chargepilot.android.library` only; no impl deps.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/home/api/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/home/api/src/main/kotlin/io/chargepilot/feature/home/HomeNavKey.kt` — `@Serializable data object HomeNavKey`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/home/impl/build.gradle.kts` — `chargepilot.android.feature`; depends on `:feature:home:api`, `:feature:disclosure:api`, `:core:capability`, `:core:device`, `:core:control`, `:core:datastore`, `:core:battery`, `:core:foreground`, `:core:ui`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/home/impl/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/home/impl/src/main/kotlin/io/chargepilot/feature/home/HomeViewModel.kt` — collects `DeviceProfileSource` + `CapabilityRegistry` + `BatteryStateSource` + `ForegroundAppSource`, computes `HomeUiState(profile, cards: List<CapabilityCardState>)`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/home/impl/src/main/kotlin/io/chargepilot/feature/home/HomeUiState.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/home/impl/src/main/kotlin/io/chargepilot/feature/home/HomeScreen.kt` — `LazyColumn` of `DeviceProfileCard` + `CapabilityCard`s; on click opens disclosure flow.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/home/impl/src/main/kotlin/io/chargepilot/feature/home/HomeNavGraph.kt` — `fun NavGraphBuilder.homeGraph(...)`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/home/impl/src/test/kotlin/io/chargepilot/feature/home/HomeViewModelTest.kt` — Turbine, fakes everything.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/home/impl/src/test/kotlin/io/chargepilot/feature/home/HomeScreenTest.kt` — Compose UI test on canned states.

### Phase 4b — `:feature:disclosure` (api + impl)  *(SERIAL — needed for slice)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/disclosure/api/build.gradle.kts`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/disclosure/api/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/disclosure/api/src/main/kotlin/io/chargepilot/feature/disclosure/DisclosureNavKey.kt` — `@Serializable data class DisclosureNavKey(val capabilityId: String)`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/disclosure/impl/build.gradle.kts` — depends on `:core:control`, `:core:datastore`, `:core:ui`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/disclosure/impl/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/disclosure/impl/src/main/kotlin/io/chargepilot/feature/disclosure/DisclosureViewModel.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/disclosure/impl/src/main/kotlin/io/chargepilot/feature/disclosure/DisclosureScreen.kt` — three-step gating with two acknowledgement checkboxes; calls `ControlOrchestrator.execute(...)` on confirm.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/disclosure/impl/src/main/kotlin/io/chargepilot/feature/disclosure/DisclosureNavGraph.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/disclosure/impl/src/test/kotlin/io/chargepilot/feature/disclosure/DisclosureViewModelTest.kt`.

### Phase 4c — `:feature:about` (api + impl)  *(PARALLEL with 4d, 4e)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/about/api/build.gradle.kts`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/about/api/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/about/api/src/main/kotlin/io/chargepilot/feature/about/AboutNavKey.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/about/impl/build.gradle.kts`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/about/impl/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/about/impl/src/main/kotlin/io/chargepilot/feature/about/AboutScreen.kt` — disclosure copy, privacy summary, license, FAQ, "Submit device report" link.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/about/impl/src/main/kotlin/io/chargepilot/feature/about/AboutNavGraph.kt`.

### Phase 4d — `:feature:history` (api + impl)  *(PARALLEL with 4c, 4e)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/history/api/build.gradle.kts`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/history/api/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/history/api/src/main/kotlin/io/chargepilot/feature/history/HistoryNavKey.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/history/impl/build.gradle.kts`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/history/impl/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/history/impl/src/main/kotlin/io/chargepilot/feature/history/HistoryViewModel.kt` — collects `OperationHistoryRepository`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/history/impl/src/main/kotlin/io/chargepilot/feature/history/HistoryScreen.kt` — list with one-tap revert button per record.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/history/impl/src/main/kotlin/io/chargepilot/feature/history/HistoryNavGraph.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/history/impl/src/test/kotlin/io/chargepilot/feature/history/HistoryViewModelTest.kt`.

### Phase 4e — `:feature:brands` (api + impl)  *(PARALLEL with 4c, 4d)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/brands/api/build.gradle.kts`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/brands/api/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/brands/api/src/main/kotlin/io/chargepilot/feature/brands/BrandsNavKey.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/brands/impl/build.gradle.kts` — depends on `:core:capability`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/brands/impl/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/brands/impl/src/main/kotlin/io/chargepilot/feature/brands/BrandsViewModel.kt` — flattens registry to a `Manufacturer × CapabilityType` matrix.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/brands/impl/src/main/kotlin/io/chargepilot/feature/brands/BrandsScreen.kt` — read-only matrix with `Evidence` chips.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/brands/impl/src/main/kotlin/io/chargepilot/feature/brands/BrandsNavGraph.kt`.

### Phase 4f — `:feature:advanced` (api + impl, with `play` / `full` source sets)  *(SERIAL — last feature; depends on flavors plumbing in Phase 5)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/advanced/api/build.gradle.kts`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/advanced/api/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/advanced/api/src/main/kotlin/io/chargepilot/feature/advanced/AdvancedNavKey.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/advanced/impl/build.gradle.kts` — applies `chargepilot.android.feature` + `chargepilot.android.flavors`. Adds `:core:control-shizuku`, `:core:control-root` to `fullImplementation` only.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/advanced/impl/src/main/AndroidManifest.xml` — empty.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/advanced/impl/src/main/kotlin/io/chargepilot/feature/advanced/AdvancedNavGraph.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/advanced/impl/src/main/kotlin/io/chargepilot/feature/advanced/AdvancedRouter.kt` — interface; bound to flavor-specific impl below.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/advanced/impl/src/play/kotlin/io/chargepilot/feature/advanced/PlayAdvancedScreen.kt` — "Shizuku and Root features are only available in the F-Droid / GitHub release" card with link.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/advanced/impl/src/play/kotlin/io/chargepilot/feature/advanced/di/PlayAdvancedModule.kt`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/advanced/impl/src/full/kotlin/io/chargepilot/feature/advanced/FullAdvancedScreen.kt` — Shizuku status, ADB instructions deep-link, root probe button, mode picker.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/advanced/impl/src/full/kotlin/io/chargepilot/feature/advanced/AdvancedViewModel.kt` — calls `ShizukuClient.bind()`, `RootClient.probe()`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/feature/advanced/impl/src/full/kotlin/io/chargepilot/feature/advanced/di/FullAdvancedModule.kt`.

### Phase 4 verification

- [ ] `./gradlew :feature:home:impl:test :feature:disclosure:impl:test :feature:history:impl:test`.
- [ ] `./gradlew :feature:home:impl:assemble :feature:disclosure:impl:assemble :feature:history:impl:assemble :feature:brands:impl:assemble :feature:about:impl:assemble :feature:advanced:impl:assemblePlayDebug :feature:advanced:impl:assembleFullDebug` — all flavors compile.

---

## Phase 5 — App wiring + flavors

### Phase 5a — `:app` skeleton  *(SERIAL — depends on Phase 0–4)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/build.gradle.kts` — `plugins { id("chargepilot.android.application"); id("chargepilot.android.compose"); id("chargepilot.android.hilt"); id("chargepilot.android.flavors") }`. Wires all `:feature:*:impl` and `:core:control-writesettings`. Adds `signingConfigs.release` reading `keystore.properties`. Build types `debug` (default) and `release` (R8 + ProGuard).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/proguard-rules.pro` — kotlinx-serialization, Hilt, libsu, Shizuku rules.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/AndroidManifest.xml` — declares `INTERNET`, `ACCESS_NETWORK_STATE`, `WRITE_SETTINGS`, `PACKAGE_USAGE_STATS`; `<application android:name=".ChargePilotApp" android:theme="@style/Theme.ChargePilot.Splash" android:enableEdgeToEdge="true">` + `<activity android:name=".MainActivity" .../>`. NEVER declare `BIND_ACCESSIBILITY_SERVICE`, `QUERY_ALL_PACKAGES`, `RECEIVE_BOOT_COMPLETED`, `SYSTEM_ALERT_WINDOW` per spec § 7.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/kotlin/io/chargepilot/app/ChargePilotApp.kt` — `@HiltAndroidApp class ChargePilotApp : Application() { override fun onCreate() { super.onCreate(); if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree()) } }`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/kotlin/io/chargepilot/app/MainActivity.kt` — `@AndroidEntryPoint`; calls `enableEdgeToEdge()`, `setContent { ChargePilotTheme { ChargePilotNavHost() } }`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/kotlin/io/chargepilot/app/navigation/ChargePilotNavHost.kt` — `NavHost` calling each feature's `*NavGraph` extension, plus `NavigationBar` with five top-level destinations.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/kotlin/io/chargepilot/app/navigation/TopLevelDestination.kt` — enum (Home, Brands, Advanced, History, About).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/res/values/strings.xml` — app name + nav labels.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/res/values/themes.xml` — `Theme.ChargePilot.Splash` (system splash screen 12+).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/res/xml/data_extraction_rules.xml` — empty allow-list (no auto-backup of secrets).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/res/xml/backup_rules.xml`.

### Phase 5b — Flavor source sets  *(PARALLEL with 5c)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/play/AndroidManifest.xml` — only the play-flavor differences (none v1; placeholder).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/play/kotlin/io/chargepilot/app/di/FlavorControlModule.kt` — empty `@Module @InstallIn(SingletonComponent::class) object PlayFlavorControlModule {}` (no extra strategies).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/play/res/values/strings.xml` — `app_name = Charge Pilot`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/full/AndroidManifest.xml` — declares Shizuku-related receivers if any.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/full/kotlin/io/chargepilot/app/di/FlavorControlModule.kt` — re-exports `:core:control-shizuku`'s and `:core:control-root`'s `@IntoMap` bindings (they self-register, but this module enforces inclusion).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/full/res/values/strings.xml` — `app_name = Charge Pilot (Full)`.

### Phase 5c — Splash + first-run polish  *(PARALLEL with 5b)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/kotlin/io/chargepilot/app/firstrun/FirstRunGate.kt` — checks `UserPreferencesRepository.seenFirstRun`; navigates to `:feature:about`'s privacy primer on first launch.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/res/drawable/ic_app_foreground.xml` — adaptive icon foreground.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`.

### Phase 5 verification

- [ ] `./gradlew :app:assemblePlayDebug` — produces `app-play-debug.apk`.
- [ ] `./gradlew :app:assembleFullDebug` — produces `app-full-debug.apk`.
- [ ] Manual smoke: install `app-play-debug.apk` on a Pixel + Galaxy S24 emulator, verify Home renders, disclosure flow gates a write.
- [ ] `./gradlew :app:lint :app:lintFullDebug :app:lintPlayDebug`.

> **Vertical slice complete after Phase 5 verification passes.** Phase 6 is mandatory before public release but does not block internal use.

---

## Phase 6 — Open-source housekeeping + CI

### Phase 6a — Repo hygiene docs  *(PARALLEL across all of 6a)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/CONTRIBUTING.md` — PR rules, device-matrix evidence requirement (video / ADB log / screen recording), commit-style guide, AGPL implications.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/SECURITY.md` — responsible disclosure address, scope, embargo policy.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/CODE_OF_CONDUCT.md` — Contributor Covenant 2.1.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/docs/PRIVACY.md` — "no telemetry; only the OTA registry fetch" statement.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/docs/DeviceCompatibilityMatrix.md` — generated artifact placeholder; explain it is regenerated by a Gradle task on each release.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/docs/architecture/overview.md` — points to spec, plus pointer to Now-in-Android module diagram.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/docs/architecture/control-strategies.md` — detail the strategy chain.

### Phase 6b — Issue templates  *(PARALLEL with 6a)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/.github/ISSUE_TEMPLATE/device_report.yml` — required fields: manufacturer, model, ROM version, ADB log, screen recording link.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/.github/ISSUE_TEMPLATE/bug_report.yml`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/.github/ISSUE_TEMPLATE/feature_request.yml`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/.github/ISSUE_TEMPLATE/config.yml` — disable blank issues.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/.github/PULL_REQUEST_TEMPLATE.md`.

### Phase 6c — CI workflows  *(SERIAL — depends on Phase 5)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/.github/workflows/ci.yml` — matrix `[api: 26, 30, 33, 36] × [flavor: play, full]`; steps: setup-jdk-17, setup-gradle, `./gradlew ktlintCheck detekt :app:assemblePlayDebug :app:assembleFullDebug`, plus `:core:capability:validateRegistry` task.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/.github/workflows/release.yml` — tag-triggered: build signed `release` for both flavors; attach AABs and APKs to GitHub Release; regenerate `docs/DeviceCompatibilityMatrix.md`.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/.github/workflows/registry-validate.yml` — runs only when `core/capability/src/main/assets/capabilities-v1.json` changes; runs schema validator alone for fast feedback.
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/.github/dependabot.yml` — Gradle + GitHub-Actions ecosystems weekly.

### Phase 6d — Static analysis configs  *(PARALLEL with 6c)*

- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/config/detekt/detekt.yml` — baseline ruleset (start from default, deviations only when justified).
- [ ] Create `/Users/iml1s/Documents/mine/charge_pilot/.editorconfig` — ktlint config (`max_line_length = 120`, etc.).
- [ ] Wire ktlint + detekt into `chargepilot.android.library` and `chargepilot.android.application` convention plugins (minor edit to Phase-0 files; defer to here so we only do it once we know what rules we want).

### Phase 6 verification

- [ ] `./gradlew ktlintCheck detekt` — clean across all modules.
- [ ] Push a branch, open a PR, confirm `ci.yml` matrix is green.
- [ ] Trigger `release.yml` with a `v0.0.1` tag in a test fork; verify artifacts attach and matrix doc regenerates.

---

## Decisions still required (gates Phase 0a)

These need user sign-off before any code lands:

1. **Version pins**: AGP / Kotlin / Compose Compiler / Compose BOM / Hilt / KSP / Coroutines / kotlinx-serialization / DataStore / Timber / Turbine / JUnit5 / Robolectric / ktlint / detekt / Shizuku API / libsu. Placeholders above are reasonable defaults but not researched.
2. **`applicationId`** and **package namespace** root. Plan assumes `io.chargepilot.app` for the main `applicationId` (no flavor suffix per spec § 7) and `io.chargepilot.*` for code packages.
3. **OTA registry URL pattern** — `https://raw.githubusercontent.com/<org>/<repo>/main/registry/capabilities-v1.json`? Needs `<org>/<repo>` decision now since `:core:data` will hardcode it (or pull from `BuildConfig`).
4. **Compose Material 3 Expressive availability** — at the time of writing, M3E APIs (`MaterialExpressiveTheme`, `MotionScheme.expressive()`) are bundled with `androidx.compose.material3:material3` 1.4.x+. Confirm the chosen Compose BOM ships a version that exposes these symbols; if not, the design system theme will need a temporary fallback to non-expressive M3.
5. **Min ROM-version detection mechanism** — `SystemProperties` reflection vs. `Runtime.exec("getprop")`. Reflection breaks on hidden-API blocklist for non-system apps on API 28+; the plan currently leans toward `Runtime.exec("getprop")`. User to confirm acceptable.
6. **Font licensing** — Fira Code and Fira Sans are SIL OFL; needs a `NOTICE` entry. The plan calls them out but the Phase-3a font copy task includes a license-attribution check.
7. **`PACKAGE_USAGE_STATS` UX** — the spec uses foreground game detection; confirm whether v1 should request the special access on first launch, on first capability that needs it, or never (with the precondition stuck pending). Plan currently defers to the disclosure flow, requesting on demand.
8. **Keystore management** — v1 uses a local `keystore.properties`. Confirm the GitHub Actions release flow stores the keystore as an encrypted secret (`KEYSTORE_BASE64` + `KEYSTORE_PASSWORD`).

---

## Recommended first parallel batch (after 0a sign-off)

The fastest path through Phase 0–1 is:

- **Lane A** (developer #1 / agent #1): Phase 0b (Gradle wrapper) → Phase 0c (`libs.versions.toml`).
- **Lane B** (developer #2 / agent #2): Phase 0d (six convention plugin scaffolds, internally serial because `chargepilot.android.feature` extends `chargepilot.android.library`).
- After Phase 0 verification (both lanes green): **Phase 1a, 1b in parallel**, then **Phase 1c serially** since it imports both.

After Phase 1 verification, **Phase 2a / 2b / 2c run in parallel**, **Phase 2d serially** (it imports 2a + 2c), then **2e / 2f / 2g in parallel**.

After Phase 2: **Phase 3a serially**, then **3b and 3c in parallel**.

Phase 4 features split as documented (4a → 4b serial for the slice; 4c / 4d / 4e parallel; 4f last).

Phase 5 must be serial.
Phase 6 sub-phases all parallelizable except 6c which depends on a green Phase 5.

---

## Honest scope estimate

| Phase | Skilled-engineer hours | Calendar days (1 dev, 6h focused) |
|---|---|---|
| 0 | 4–6 | 1 |
| 1 | 6–8 | 1.5 |
| 2 | 18–24 | 4 |
| 3 | 8–12 | 2 |
| 4 | 14–20 | 3 |
| 5 | 6–8 | 1.5 |
| 6 | 6–10 | 1.5 |
| **Total** | **62–88** | **~14 calendar days** |

Plus ~1 week of device-matrix verification (Phase 2e + 4a smoke testing on real Galaxy + Pixel + OnePlus + Xiaomi + HONOR hardware). Plus ~2 weeks of community feedback + registry-data PRs before tagging `v0.1.0`.

This plan delivers a working `play`-flavor APK that is honest about what it can do, before attempting any device-by-device control work beyond Samsung S2x.
