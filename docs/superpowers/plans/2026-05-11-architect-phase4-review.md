# Charge Pilot Phase 4 Architecture Review

**Author:** architect agent
**Date:** 2026-05-11
**Inputs reviewed:** spec, impl plan, codex plan review, all 70+ source files under `app/`, `core/*`, `feature/*`, `build-logic/`.

---

## Summary (top 3 + verdict)

**Verdict: Yellowlight.** The skeleton is structurally honest — module graph, flavor split, IntoMap strategy registration, and the `Engaged != Active` distinction are all correctly wired. However the v0 vertical slice the spec promises does **not** function end-to-end yet because the precondition pipeline that turns `Engaged` into `Active`/`PendingConditions` is not connected. Top three findings:

1. **P0** — `HomeViewModel` never resolves preconditions, so the home screen will permanently render `Engaged` ("configured (preconditions not verified)") on a real Samsung S24 — the central UX promise of the app is silently bypassed.
2. **P1** — `OfficialGuidanceStrategy.setEnabled()` calls `context.startActivity()` from a `@Singleton` `@ApplicationContext` (using `FLAG_ACTIVITY_NEW_TASK`); this works but couples a "core/control" Singleton to UI launching, breaking ActivityResult, Compose previews, and disclosure-flow control. Codex flagged this; it is not fixed.
3. **P1** — `:core:battery`, `:core:foreground`, `:core:datastore` are stubs returning `false`/`null`/no-op while `app/build.gradle.kts:46-47` and `feature/history/impl/build.gradle.kts` already wire them in. The slice **looks** complete to a build but cannot record history, evaluate preconditions, or persist user preferences.

Greenlight to proceed once P0 and the stub fills (battery, foreground, datastore, OTA fetch) land.

---

## 1. Architecture conformance

### Module graph — clean
Feature `impl` modules depend only on other features' `api` (`feature/home/impl/build.gradle.kts:11` depends on `:feature:disclosure:api`, never `:impl`). The convention plugin (`build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt:18-30`) injects core deps but no feature-impl deps. **Pass.**

### Strategy pattern — correctly implemented
`@MapKey ControlMethodKey` lives in `core/control/src/main/kotlin/com/chargepilot/core/control/di/ControlModule.kt:13-14`; each strategy module (`writesettings/di/WriteSettingsModule.kt:16-19`, `shizuku/di/ShizukuModule.kt:16-19`, `root/di/RootModule.kt:16-19`) uses `@Binds @IntoMap @ControlMethodKey(...)` and `ControlOrchestrator` (`core/control/src/main/kotlin/com/chargepilot/core/control/ControlOrchestrator.kt:17`) injects `Map<ControlMethod, @JvmSuppressWildcards ControlStrategy>`. Pickup order respects `descriptor.availableMethods` and optional user preference. Tests at `core/control/src/test/.../ControlOrchestratorTest.kt:43-102` cover order, fallthrough, override, empty. **Pass.**

### `Engaged` vs `Active` — defined correctly, **broken at the seam**
`core/model/src/main/kotlin/com/chargepilot/core/model/Control.kt:21` adds `data object Engaged` (not in spec but a deliberate, correct addition; see codex P1 about Success-vs-Active conflation). `WriteSettingsStrategy.getCurrentState` (`core/control-writesettings/.../WriteSettingsStrategy.kt:51-64`) returns `Engaged` when the int is non-zero. The CapabilityCard (`core/ui/.../CapabilityCard.kt:103-105`) renders `Engaged` distinctly. **The seam fails at `HomeViewModel.kt:35-39`**: it stores `strategy.getCurrentState(descriptor)` directly into `CapabilityRow.state` without ever invoking `PreconditionChecker`. Since `core/domain/.../PreconditionChecker.kt:11` is a `fun interface` with no implementation bound in any Hilt module, even injecting it would NPE. Result: `Engaged` is rendered as "preconditions not verified" forever. **Fail.**

## 2. Functional completeness for the v0 slice

**Working end-to-end:**
- Detection: `DeviceDetector.detect()` + `Manufacturer.fromBuild()` + `RomVersionParser` (`core/device/.../DeviceDetector.kt:19-63`) — real `Build.*` reads, real getprop reads.
- Registry: `BundledCapabilityRegistryLoader` reads `core/capability/src/main/assets/capabilities-v1.json`; `CapabilityRegistry.resolve()` filters by manufacturer + regex + ROM version (`CapabilityRegistry.kt:19-51`). Samsung S24 (`SM-S921B`, One UI 6+) resolves to one rule.
- WriteSettings: `WriteSettingsStrategy.setEnabled()` does a real `Settings.System.putInt()` with `canWrite()` gating and `SecurityException` translation (`WriteSettingsStrategy.kt:66-89`).
- OfficialGuidance: real `Intent` fire with fallback to `ACTION_SETTINGS` (`OfficialGuidanceStrategy.kt:36-60`).
- Compose UI: `HomeScreen` renders Loading/Error/Ready and `LazyColumn` of `CapabilityCard`s (`HomeScreen.kt:21-73`); CapabilityCard distinguishes 5 states.

**Stubbed but pretending to be real (P0/P1):**
- `core/battery/src/main/kotlin/.../BatteryProvider.kt:12-13` — returns `false` and `null`. No `BatteryManager` or sticky `ACTION_BATTERY_CHANGED`. **P0** for the slice promise.
- `core/foreground/src/main/kotlin/.../ForegroundDetector.kt:11-14` — returns `null`/`false`. No `UsageStatsManager`. **P0** for Samsung's `GameInForeground` precondition.
- `core/datastore/src/main/kotlin/.../UserPreferencesDataSource.kt:9` — empty class body. No Proto DataStore, no `OperationHistoryRepository`, no revert. The plan's goal item 4 ("Persist operation history with one-tap revert") is unmet. **P1.**
- `core/data/src/main/kotlin/.../CapabilityRepository.kt:9-18` — exists but only forwards to `CapabilityRegistry`. **No OTA fetcher, no cache, no schema validator, no signature verification.** Codex P1 (registry safety) is unaddressed.
- `feature/disclosure/impl/.../DisclosureScreen.kt:11-19` — placeholder text. The three-step disclosure required by the spec is not built; `HomeScreen.kt:68-69` `onTryClick`/`onOpenOfficialClick` are no-ops.
- `feature/{advanced,history,about,brands}/impl/...Screen.kt` — all placeholders.

**Will the home screen show usable state on a real Samsung S24?** Yes for the device card and the capability card row, but the row will read "Status: unknown" or "Status: configured (preconditions not verified)" forever, with both buttons inert.

## 3. Architectural shortcuts that will be expensive to undo

**P1 — `OfficialGuidanceStrategy` launches activities from a Singleton with `FLAG_ACTIVITY_NEW_TASK`** (`OfficialGuidanceStrategy.kt:42-47`). Same problem codex flagged: this kills ActivityResult, blocks Compose previews of disclosure paths, and creates an outside-task launch on Android 14+ that some OEMs already restrict. Concrete patch (4 lines):
```kotlin
// In ControlStrategy.kt or new sealed type:
sealed interface ControlResult {
    data object Success : ControlResult
    data class RequiresUserAction(val intent: Intent) : ControlResult  // NEW
    data class Failed(val reason: FailureReason) : ControlResult
}
```
Then `OfficialGuidanceStrategy.setEnabled` returns `RequiresUserAction(intent)` and the disclosure screen uses `rememberLauncherForActivityResult`.

**P1 — `feature/advanced/impl` declares `fullImplementation` deps directly** (`feature/advanced/impl/build.gradle.kts:21-22`) on `:core:control-shizuku` and `:core:control-root`. Codex's recommendation was to keep flavor wiring in `:app` only. Today the deps are duplicated in both `app/build.gradle.kts:70-71` AND `feature/advanced/impl/build.gradle.kts:21-22`. This will cause Hilt aggregation duplicate-binding errors as soon as the Shizuku/root strategies stop returning `isAvailable=false`. Concrete patch (2 lines): delete lines 21-22 of `feature/advanced/impl/build.gradle.kts`; the app already aggregates them.

**P1 — `HomeViewModel` does not collect lifecycle-aware flows** (`HomeViewModel.kt:26-45`). State is computed exactly once in `init { load() }`. The whole point of `BatteryStateSource.observe()` and `ForegroundAppSource.observe()` is reactive. Once stubs are filled, this view-model needs a rewrite to `combine(...).stateIn(viewModelScope, WhileSubscribed(5000), Loading)`. Not a "shortcut to undo" — but the current shape will be replaced wholesale.

**P2 — `ChargePilotApp.kt:11` references `BuildConfig.DEBUG`** but the `:app` module's `buildConfig=true` is in `app/build.gradle.kts:31`. This works, but no other module enables `BuildConfig`, so the `Logger` abstraction the plan calls for (Phase 1a `LogConfig` interface) will need to be retrofitted across modules.

## 4. CapabilityRegistryLoader OTA-readiness

`core/capability/.../CapabilityRegistryLoader.kt:16-18` is a single-method `interface CapabilityRegistryLoader { suspend fun load(): CapabilityRegistrySnapshot }`. **The shape is correct for adding OTA without breaking changes** — a future `OtaCapableRegistryLoader` can wrap a delegate (cache-or-bundled) and the `BundledCapabilityRegistryLoader` continues to be the offline-safe fallback. The Hilt binding (`di/CapabilityModule.kt:16-18`) can swap implementations via a `@Qualifier` or direct rebinding without touching call sites. **However**, the snapshot type at `Registry.kt:7-11` is missing the OTA-required fields codex listed: `registryVersion`, `minAppVersion`, `generatedAt`, `sourceRevision`, `signature`. Adding them later is **not** a breaking change at the loader interface — it is at the JSON schema level. Recommendation: add the optional fields to `CapabilityRegistrySnapshot` now (with `= null` defaults) so OTA can land later without invalidating the bundled asset.

Concrete 4-line patch:
```kotlin
data class CapabilityRegistrySnapshot(
    val schemaVersion: Int,
    val lastUpdated: String,
    val rules: List<CapabilityRule>,
    val registryVersion: Long? = null,        // NEW: monotonic for OTA rollback
    val minAppVersion: Int? = null,           // NEW
    val signature: String? = null,            // NEW: detached Ed25519 hex
)
```

---

## P0 / P1 patches (concrete, < 5 lines each)

### P0 — Wire `PreconditionChecker` into `HomeViewModel`
File: `feature/home/impl/src/main/kotlin/com/chargepilot/feature/home/impl/HomeViewModel.kt:35-38`
```kotlin
val rows = descriptors.map { d ->
    val s = controlOrchestrator.pickStrategy(profile, d)
    val raw = s?.getCurrentState(d) ?: ControlState.Unknown
    val effective = if (raw is ControlState.Engaged) {
        val unmet = preconditionChecker.check(d.preconditions).unmet
        if (unmet.isEmpty()) ControlState.Active(System.currentTimeMillis()) else ControlState.PendingConditions(unmet)
    } else raw
    CapabilityRow(d, effective)
}
```
Plus add `private val preconditionChecker: PreconditionChecker` to constructor and a Hilt `@Provides` binding in `:core:domain` (or move the impl into `:core:control` next to `PreconditionEvaluator` as the plan intended).

### P1 — Decouple `OfficialGuidanceStrategy` from activity launch
File: `core/model/src/main/kotlin/com/chargepilot/core/model/Control.kt:27-30`
```kotlin
sealed interface ControlResult {
    data object Success : ControlResult
    data class RequiresUserAction(val intent: android.content.Intent) : ControlResult
    data class Failed(val reason: FailureReason) : ControlResult
}
```
Then in `OfficialGuidanceStrategy.kt:36-60` build the `Intent` and `return ControlResult.RequiresUserAction(intent)` instead of calling `context.startActivity`. `WriteSettingsStrategy.buildSpecialAccessIntent()` should also be exposed via this same shape.

### P1 — Remove duplicate flavor deps in advanced impl
File: `feature/advanced/impl/build.gradle.kts:21-22`
```kotlin
// DELETE these two lines; app/build.gradle.kts already aggregates:
//   "fullImplementation"(project(":core:control-shizuku"))
//   "fullImplementation"(project(":core:control-root"))
```

### P1 — Fill the BatteryProvider so preconditions can resolve
File: `core/battery/src/main/kotlin/com/chargepilot/core/battery/BatteryProvider.kt`
```kotlin
@Singleton
class BatteryProvider @Inject constructor(@ApplicationContext private val ctx: Context) {
    private fun snapshot() = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    fun batteryLevelPercent(): Int? = snapshot()?.let {
        val l = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); val s = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (l < 0 || s <= 0) null else (l * 100 / s)
    }
    fun isPdChargerConnected(): Boolean = snapshot()?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) == BatteryManager.BATTERY_PLUGGED_USB
}
```
(PD detection from `EXTRA_PLUGGED` is approximate; precise PPS detection requires a `UsbManager`/`UsbPort` query on API 31+, but unblocks the slice.)

### P1 — Add OTA-ready optional fields to registry snapshot
File: `core/capability/src/main/kotlin/com/chargepilot/core/capability/Registry.kt:7-11`
```kotlin
@Serializable
data class CapabilityRegistrySnapshot(
    val schemaVersion: Int, val lastUpdated: String, val rules: List<CapabilityRule>,
    val registryVersion: Long? = null, val minAppVersion: Int? = null, val signature: String? = null,
)
```

---

## Trade-offs

| Option for P0 fix | Pros | Cons |
|---|---|---|
| Inject `PreconditionChecker` into `HomeViewModel` (above) | Minimal change; matches doc on `Control.kt:14-21` | View-model still owns mapping; future strategies must remember to do this |
| Move into `ControlOrchestrator.observeStates(profile): Flow<Map<id, snapshot>>` | Single source of truth (codex's recommendation) | Larger refactor; needs reactive flows from battery/foreground first |

Recommend the first now (cheap, 8 lines), the second after stubs are filled.

| Option for P1 OfficialGuidance | Pros | Cons |
|---|---|---|
| `RequiresUserAction(intent)` (above) | Decouples strategy from UI; testable; matches Android lifecycle | Tiny breaking change to `ControlResult` callers (only 5 sites today) |
| Inject an `IntentLauncher` interface | Less invasive at `ControlResult` site | Hides intent target from UI; harder to test |

Recommend `RequiresUserAction`.

---

## References

- `core/control/src/main/kotlin/com/chargepilot/core/control/ControlStrategy.kt:18-34` — strategy contract; doc explicitly warns about Engaged vs Active conflation, validating the model design.
- `core/control/src/main/kotlin/com/chargepilot/core/control/ControlOrchestrator.kt:17-35` — `Map<ControlMethod, ControlStrategy>` injected; pickStrategy honors descriptor order and user preference.
- `core/control/src/main/kotlin/com/chargepilot/core/control/di/ControlModule.kt:13-22` — `@MapKey ControlMethodKey` + `@IntoMap @Binds` for OFFICIAL_GUIDANCE.
- `core/control-writesettings/src/main/kotlin/com/chargepilot/core/control/writesettings/WriteSettingsStrategy.kt:51-64` — returns `ControlState.Engaged` (not `Active`) intentionally.
- `core/control-writesettings/src/main/kotlin/com/chargepilot/core/control/writesettings/di/WriteSettingsModule.kt:16-19` — IntoMap binding for WRITE_SETTINGS_KEY.
- `core/control-shizuku/.../ShizukuStrategy.kt:22-26` — `isAvailable=false`, so orchestrator never picks it (safe).
- `core/control-root/.../RootStrategy.kt:18-22` — same.
- `core/model/src/main/kotlin/com/chargepilot/core/model/Control.kt:21-25` — `Engaged` is a deliberate, well-documented addition.
- `core/capability/src/main/kotlin/com/chargepilot/core/capability/CapabilityRegistryLoader.kt:16-18` — clean OTA-extension point.
- `core/capability/src/main/assets/capabilities-v1.json:1-80` — Samsung S24 + Pixel + OnePlus rules already populated; descriptor `id` is in the format `<rule>::<TYPE>` per codex P1 fix.
- `core/battery/src/main/kotlin/com/chargepilot/core/battery/BatteryProvider.kt:12-13` — STUB returns false/null.
- `core/foreground/src/main/kotlin/com/chargepilot/core/foreground/ForegroundDetector.kt:11-14` — STUB.
- `core/datastore/src/main/kotlin/com/chargepilot/core/datastore/UserPreferencesDataSource.kt:9` — empty class.
- `core/domain/src/main/kotlin/com/chargepilot/core/domain/PreconditionChecker.kt:11-14` — `fun interface`, NO impl bound anywhere.
- `feature/home/impl/src/main/kotlin/com/chargepilot/feature/home/impl/HomeViewModel.kt:30-45` — does not invoke `PreconditionChecker`; no flow reactivity.
- `feature/home/impl/src/main/kotlin/com/chargepilot/feature/home/impl/HomeScreen.kt:64-71` — onTryClick/onOpenOfficialClick are no-ops.
- `feature/disclosure/impl/.../DisclosureScreen.kt:11-19` — placeholder text only.
- `feature/advanced/impl/build.gradle.kts:21-22` — duplicate `fullImplementation` deps that conflict with `app/build.gradle.kts:70-71`.
- `app/src/main/AndroidManifest.xml:5-7` — `WRITE_SETTINGS` and `PACKAGE_USAGE_STATS` declared; no permission education flow exists yet.
- `app/src/main/kotlin/com/chargepilot/app/navigation/ChargePilotNavHost.kt:11-16` — only Home is wired into NavHost; brands/advanced/history/about/disclosure routes are not registered despite the bottom-nav-bar UX in spec § 4.1.
- `build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt:18-30` — feature convention pulls `:core:designsystem`, `:core:ui`, `:core:common`, `:core:model`, `:core:domain` for every feature impl; no impl-to-impl deps possible.
- `build-logic/convention/src/main/kotlin/AndroidFlavorsConventionPlugin.kt:13-17` — `full` carries `applicationIdSuffix=".full"`, contradicting plan decision 0a.3 that said "no suffix"; this lets play and full coexist on the same device, which is the pragmatic choice — flag for confirmation.
- `core/control/src/test/kotlin/.../ControlOrchestratorTest.kt:43-102` — orchestrator tests are real, fakes are reasonable; no Robolectric; passes JUnit5.
