# Charge Pilot — Design Specification

**Status:** v1 (brainstorming output, awaiting user review)
**Date:** 2026-05-11
**Author:** Claude (drafted from `analyst` brainstorming + research via context7 + Bright Data + ui-ux-pro-max)

---

## 1. Goal

Build an Android-only, open-source utility (`Charge Pilot`) that:

- **Detects** what charging-related features the user's device officially supports.
- **Guides** the user to the manufacturer's documented settings entry points.
- **Optionally controls** charging-related system settings on whitelisted devices, with full disclosure, user consent, and reversibility.
- Ships in two flavors: `play` (Google Play, layers 1+2) and `full` (F-Droid / GitHub, layers 1+2+3 including Shizuku and root).

Charge Pilot is explicitly **not** a "bypass charging cracker". It is a *capability detection and safe-control framework* whose primary job is to tell the user what is and isn't possible, and to make any actual change transparent and reversible.

---

## 2. Non-goals

- Modifying device settings without explicit user action and consent.
- Background or scheduled changes to charging behavior.
- Mass-market promises that "any Android phone can do bypass charging" — most cannot.
- Accessibility-service-driven UI automation of OEM Settings apps.
- Companion-Device or Device-Owner / MDM workflows for consumer devices.
- Cross-platform (iOS does not have these features).

---

## 3. Tech stack

| Layer | Choice | Notes |
|---|---|---|
| Language | Kotlin 2.x | latest stable |
| UI | Jetpack Compose + Material 3 Expressive | M3E launched alongside Android 16; uses `MaterialExpressiveTheme`, `MotionScheme.expressive()`, shape morphing |
| Min SDK | 26 (Android 8) | covers ~95% of devices, supports `WRITE_SETTINGS`, `UsageStatsManager`, notification channels |
| Target SDK | 36 (Android 16) | edge-to-edge mandatory, predictive back default, latest Play Store requirement |
| DI | Hilt | Now in Android standard; mature multi-module ecosystem |
| State | ViewModel + StateFlow + `collectAsStateWithLifecycle` | Compose recommended pattern |
| Async | Kotlin Coroutines + Flow | standard |
| Build | Gradle KTS + Version Catalogs (`gradle/libs.versions.toml`) + Convention Plugins (`build-logic/`) | Now in Android pattern |
| Serialization | kotlinx.serialization (JSON) | for capability registry |
| Local storage | Proto DataStore | type-safe; preferences + operation history |
| Logging | Timber | gated by `BuildConfig.DEBUG` |
| Testing | JUnit5 + Turbine + Compose UI Test + Robolectric | Flow testing via Turbine |
| Shizuku | `dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider` | `full` flavor only |
| Root | `com.github.topjohnwu.libsu:core` | `full` flavor only |

Android 16 implications:
- `enableEdgeToEdge()` mandatory; all UI must respect `WindowInsets`.
- `onBackPressed()` no longer called; use `BackHandler` composable + `OnBackPressedCallback`.
- 16 KB page size support recommended for native libs.

---

## 4. UX

### 4.1 Information architecture

Five top-level destinations (NavigationBar):

1. **Home** — `DeviceProfileCard` + ordered `CapabilityCard` list.
2. **Brands** — read-only cross-brand support matrix (educational; renders directly from `capabilities-v1.json`).
3. **Advanced** — `full` flavor: Shizuku/ADB/Root mode UI; `play` flavor: explanation card pointing to F-Droid / GitHub releases.
4. **History** — DataStore-backed log of every settings write, with one-tap revert.
5. **About** — disclosure, privacy, license, FAQ, device-report link.

### 4.2 Visual design

- Style: Swiss-Modernism layout discipline + Material 3 Expressive motion overlay.
- Theme: dynamic color (Material You) on Android 12+, with toggle to fall back to fixed palette.
- Mode: light / dark / follow-system (default). Dark mode required.
- Fallback palette:
  - Primary `#1E293B` (slate-800)
  - CTA / Active `#22C55E` (green-500)
  - Background dark `#0F172A`, light `#F8FAFC`
  - Warning `#F59E0B`, Danger `#EF4444`
- Typography: Fira Code (display/heading), Fira Sans (body).
- Motion: `MotionScheme.expressive()` (0–300 ms range); the primary "Try it" button uses shape morphing on press.

### 4.3 Capability card states

| State | Visual treatment |
|---|---|
| `Unsupported` | Grayscale card with lock icon; offers alternative (e.g., 80% limit on Pixel). |
| `Available, idle` | Outlined card; CTA enabled. |
| `Pending conditions` | Amber border + pulse; shows checklist of unmet preconditions (PD charger, battery ≥ 20%, game in foreground). |
| `Active` | Green border + filled CTA + animated check; shows since-when. |

### 4.4 Disclosure and trust

For any operation that writes a system setting, the first invocation must show the full three-step disclosure:

1. **What this changes** — exact namespace + key + before/after value, source attribution (official doc / official community / community-tested / project-verified / unverified).
2. **What it means** — verified devices list, preconditions, reversibility, "no background changes" promise.
3. **Confirm** — two checkboxes acknowledging experimentality, then `[I understand, continue]`.

Subsequent invocations of the same capability show only step 3.

Permanent UX guarantees on every capability card:
- "Reversible" badge.
- "Logged in History" pointer.
- "Open Official Settings" button always visible.
- Source attribution always visible.

### 4.5 Edge-to-edge and predictive back

- All activities call `enableEdgeToEdge()`; layouts use `WindowInsets`.
- Disclosure flow uses `BackHandler` to intercept predictive back gestures so the user is not accidentally navigated out before reading the disclosure.

---

## 5. Architecture

### 5.1 Module layout (Now-in-Android style)

```
charge_pilot/
├── app/                              # MainActivity, NavHost, Application
├── build-logic/convention/           # Gradle convention plugins
├── core/
│   ├── designsystem/                 # M3E theme, tokens, fonts, ChargePilotTheme
│   ├── ui/                           # CapabilityCard, DeviceProfileCard, DisclosureSheet
│   ├── common/                       # Dispatchers, Result, Logger
│   ├── model/                        # Domain types (DeviceProfile, CapabilityType, ...)
│   ├── domain/                       # Use cases
│   ├── data/                         # Repositories
│   ├── datastore/                    # Proto DataStore (UserPreferences, OperationHistory)
│   ├── device/                       # Build.* detection, ROM version parsing
│   ├── capability/                   # CapabilityRegistry + JSON loader
│   ├── control/                      # ControlStrategy interface + ControlOrchestrator + OfficialGuidanceStrategy
│   ├── control-writesettings/        # WriteSettingsStrategy (both flavors)
│   ├── control-shizuku/              # Shizuku integration (full flavor only)
│   ├── control-root/                 # libsu-based root strategy (full flavor only)
│   ├── battery/                      # BatteryManager wrapper + BroadcastReceiver
│   ├── foreground/                   # UsageStatsManager wrapper
│   └── testing/                      # Test utilities
└── feature/
    ├── home/{api,impl}/
    ├── brands/{api,impl}/
    ├── advanced/{api,impl}/          # play and full source sets
    ├── history/{api,impl}/
    ├── about/{api,impl}/
    └── disclosure/{api,impl}/
```

Feature `api` contains only navigation keys; feature `impl` contains everything else. Features depend only on other features' `api`.

### 5.2 Domain model (in `:core:model`)

```kotlin
enum class Manufacturer { SAMSUNG, GOOGLE, ONEPLUS, XIAOMI, HONOR, HUAWEI, OTHER }

data class DeviceProfile(
    val manufacturer: Manufacturer,
    val model: String,
    val codename: String,
    val androidApi: Int,
    val androidVersion: String,
    val romVersion: String?,
    val buildFingerprint: String,
)

enum class CapabilityType {
    PAUSE_PD_DURING_GAMING,   // Samsung
    BYPASS_CHARGING,           // OnePlus / Xiaomi etc.
    CHARGE_SEPARATION,         // HONOR
    BATTERY_LIMIT_80,          // Pixel and others
    ADAPTIVE_CHARGING,         // Pixel
}

enum class ControlMethod { OFFICIAL_GUIDANCE, WRITE_SETTINGS_KEY, SHIZUKU_RPC, ROOT_SHELL }

sealed interface Precondition {
    data object PdChargerPresent : Precondition
    data class BatteryLevelAbove(val percent: Int) : Precondition
    data class GameInForeground(val knownGames: Set<String>?) : Precondition
}

sealed interface ControlState {
    data object Unknown : ControlState
    data object Inactive : ControlState
    data class PendingConditions(val unmet: List<Precondition>) : ControlState
    data class Active(val sinceEpochMs: Long) : ControlState
}

enum class Evidence { OFFICIAL_DOC, OFFICIAL_COMMUNITY, COMMUNITY_TESTED, PROJECT_VERIFIED, UNVERIFIED }

data class SettingsKey(val namespace: String, val key: String, val type: String)

data class IntentSpec(val action: String, val component: String?, val fallbackPath: String?)

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
```

### 5.3 ControlStrategy contract (in `:core:control`)

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

@Singleton
class ControlOrchestrator @Inject constructor(
    private val strategies: Map<ControlMethod, @JvmSuppressWildcards ControlStrategy>,
)
```

`ControlOrchestrator.pickStrategy` chooses by user preference, falling back through `OFFICIAL_GUIDANCE → WRITE_SETTINGS_KEY → SHIZUKU_RPC → ROOT_SHELL`.

### 5.4 CapabilityRegistry

JSON schema lives in `:core:capability/src/main/assets/capabilities-v1.json`. App startup performs a non-blocking fetch of the latest version from a versioned GitHub raw URL; on success, the new JSON is cached in DataStore and applied on next start. On failure the bundled asset is used.

A registry rule looks like:

```json
{
  "id": "samsung-galaxy-s2x-pause-pd",
  "matchers": {
    "manufacturer": "SAMSUNG",
    "modelRegex": "SM-S(91|92|93)[0-9].*",
    "minRomVersion": { "type": "ONE_UI", "version": "6.0" }
  },
  "capabilities": [{
    "type": "PAUSE_PD_DURING_GAMING",
    "availableMethods": ["OFFICIAL_GUIDANCE", "WRITE_SETTINGS_KEY"],
    "settingsKey": { "namespace": "system", "key": "pass_through", "type": "int" },
    "officialIntent": {
      "action": "android.settings.SETTINGS",
      "fallbackPath": "Game Booster › Pause USB PD charging when gaming"
    },
    "preconditions": [
      { "type": "PD_CHARGER_PRESENT" },
      { "type": "BATTERY_LEVEL_ABOVE", "percent": 20 },
      { "type": "GAME_IN_FOREGROUND" }
    ],
    "evidence": "PROJECT_VERIFIED",
    "sourceUrl": "https://www.samsung.com/...",
    "verifiedDate": "2026-04-12"
  }]
}
```

OTA updates load *data only*, never code, and are applied on next launch.

---

## 6. Per-brand strategy matrix

| Brand | Tier | Capability | Methods | Notes |
|---|---|---|---|---|
| Samsung Galaxy S/Fold/Flip (One UI 6+) | S1 | `PAUSE_PD_DURING_GAMING` | Official guidance + `WRITE_SETTINGS_KEY` (`pass_through`) | Preconditions: PPS PD ≥25 W, battery ≥20%, game in foreground. |
| Google Pixel | S2 | `BATTERY_LIMIT_80`, `ADAPTIVE_CHARGING` | Official guidance only | No bypass; UX must not promise it. |
| OnePlus 12/13/13R/15, Nord CE5, Pad Go 2 | S2 | `BYPASS_CHARGING` | Official guidance + Shizuku (`full` only) | Public WRITE_SETTINGS key not confirmed; verify per build. |
| Xiaomi / Redmi / POCO | S2 | `BYPASS_CHARGING` | Official guidance + Shizuku (`full` only) | POCO F7 explicitly negative; K90 community-mentioned. |
| HONOR (MagicOS 10) | S2 | `CHARGE_SEPARATION` | Official guidance (Game Manager 18.0.23.305+) | UI deep-link to Game Manager. |
| Huawei | S3 | — | Placeholder ("unverified") | — |
| Other (ASUS, ROG, Black Shark, RedMagic) | S3 | — | Future | Many gaming phones support bypass; add as community PRs land. |

Tiers:
- **S1** — official + WRITE_SETTINGS verified by project.
- **S2** — official guidance + Shizuku route in `full` flavor.
- **S3** — placeholder.

---

## 7. Flavors and build

Convention plugins under `build-logic/convention`:
- `AndroidApplicationConventionPlugin`
- `AndroidLibraryConventionPlugin`
- `AndroidFeatureConventionPlugin`
- `AndroidComposeConventionPlugin`
- `AndroidHiltConventionPlugin`
- `AndroidFlavorsConventionPlugin`

Product flavors (dimension `distribution`):
- `play` — `applicationIdSuffix=""`, no Shizuku/root deps. The `:feature:advanced:impl` `play` source set renders an explanation card pointing to F-Droid / GitHub releases.
- `full` — adds `:core:control-shizuku` and `:core:control-root` as runtime dependencies; `:feature:advanced:impl` `full` source set surfaces the real UI.

Build types: `debug`, `release` (R8 + ProGuard). Signing config wired via `keystore.properties`.

Permissions:
- Both flavors: `INTERNET` (only used for capability-registry OTA fetch and clearly disclosed), `ACCESS_NETWORK_STATE`.
- Both flavors, special access (user-grantable): `WRITE_SETTINGS`, `PACKAGE_USAGE_STATS`.
- Never used: `BIND_ACCESSIBILITY_SERVICE`, `QUERY_ALL_PACKAGES`, `RECEIVE_BOOT_COMPLETED` for changing settings, `SYSTEM_ALERT_WINDOW`.

---

## 8. Testing and CI

- Unit tests: `:core:model`, `:core:capability`, `:core:control` (pure Kotlin / JUnit5 / Turbine).
- Robolectric tests: `:core:device`, `:core:battery`, `:core:foreground`.
- Compose UI tests: `:feature:*:impl`.
- Smoke tests (manual): per-flavor APK installed on Galaxy S24/S25 (S1) and Pixel 8/9 (S2-no-bypass).
- GitHub Actions matrix: `[api: 26, 30, 33, 36] × [flavor: play, full]`.
- Static analysis: ktlint + detekt; PR-blocking.
- Spec compliance test: a JSON schema validator runs against `capabilities-v1.json` on every build.

---

## 9. Open source and repo

- License: AGPL-3.0-or-later. Documentation: CC BY 4.0.
- `CONTRIBUTING.md` requires PRs that add device-matrix entries to attach evidence (video, ADB log, or screen recording).
- `SECURITY.md` describes responsible disclosure.
- Issue templates: `device_report.yml`, `bug_report.yml`, `feature_request.yml`.
- `docs/DeviceCompatibilityMatrix.md` is generated from the registry JSON on each release.
- Privacy: `docs/PRIVACY.md`. No telemetry. No analytics. No personal data leaves the device. The only network call is the read-only OTA fetch of the capability registry from a public GitHub raw URL.

---

## 10. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Samsung renames `pass_through` in a future One UI | Registry JSON is OTA-updatable; failure modes degrade to OFFICIAL_GUIDANCE; `verifiedDate` exposed in UI. |
| Play Store flags WRITE_SETTINGS as deceptive | Disclosure flow, never-background promise, "Open Official Settings" always visible, History log, Reset button. |
| Shizuku users confused by reboot reset | First-run primer in the `full` flavor; status banner in `Advanced`. |
| OTA registry fetch becomes attack surface | JSON is data-only, validated against schema, cached, signed-URL optional in v2. |
| User assumes a write succeeded ⇒ bypass active | UI separates "key written" from "preconditions met"; `Active` state requires both. |

---

## 11. Out of scope (v1)

- iOS / cross-platform.
- Companion-Device or Device-Owner workflows.
- AccessibilityService-based automation.
- Cloud sync of operation history.
- Telemetry / crash reporting (deliberate).
- Automated foreground game detection that infers "this is a game" from package metadata (v1 uses an explicit known-games list).
