# Architecture overview

Charge Pilot is a multi-module Kotlin Android app. Live truth for product claims is this tree plus `README.md`, `docs/brand-methods.md`, and the bundled `core/capability/src/main/assets/capabilities-v1.json`. Older design docs under `docs/superpowers/` are historical snapshots.

## Module graph (from `settings.gradle.kts`)

```
app
 ├─ core:designsystem, core:ui, core:common, core:model
 ├─ core:domain, core:data, core:datastore
 ├─ core:device, core:capability
 ├─ core:control, core:control-writesettings
 ├─ core:battery, core:foreground, core:testing
 ├─ feature:{home,brands,advanced,history,about,disclosure}:{api,impl}
 └─ full flavor only:
      core:control-shizuku, core:control-root
```

Build conventions live in `build-logic/convention` (Android library/application, Compose, Hilt, flavors, JUnit Platform).

## Flavors

| Flavor | Dependencies | Control layers |
|--------|--------------|----------------|
| `play` | No Shizuku/root modules | Official guidance; WRITE_SETTINGS strategy present but registry must list a method + allowlisted key |
| `full` | `fullImplementation` of `control-shizuku` and `control-root` | Above + Shizuku RPC; root is a non-functional stub |

## Data flow (Home)

1. `DeviceDetector` builds a `DeviceProfile` from `Build.*` + OEM props.
2. `CapabilityRegistry` loads **bundled** JSON once (in-memory cache) and matches rules.
3. `ControlOrchestrator` picks a registered `ControlStrategy` by method.
4. Strategies return `Engaged`/`Inactive` for key state; `ControlStateBridge` + `PreconditionChecker` map Engaged → Active or PendingConditions.
5. Direct writes require disclosure consent (`UserPreferencesDataSource`) and go through allowlisted keys only.
6. Operation history is Preferences DataStore JSON on device only.

## Network

None in the current release. The capability registry is not fetched over the network. See `docs/PRIVACY.md` and plan 026 (signed OTA spike only).
