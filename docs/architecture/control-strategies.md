# Control strategies

## Orchestrator

`ControlOrchestrator` holds a multimap of `ControlMethod` → `ControlStrategy` (Hilt multibinding). `pickStrategy` walks `descriptor.availableMethods` (or a user preference override) and returns the first strategy that reports `isAvailable`.

## Strategies

| Method | Module | Behavior |
|--------|--------|----------|
| `OFFICIAL_GUIDANCE` | `core:control` | Resolves `officialIntent`; `startActivity` → `NavigatedToSettings` (never `Success`). Unresolved activity → `Failed(UNSUPPORTED_DEVICE)`. |
| `WRITE_SETTINGS_KEY` | `core:control-writesettings` | `Settings.System.putInt` after `canWrite`; **read-after-write** required for Success; key must be in `WritableSettingsKeys`. |
| `SHIZUKU_RPC` | `core:control-shizuku` (full) | UserService runs `settings` CLI; bind mutex + unbind on timeout; same allowlist + read-after-write. |
| `ROOT_SHELL` | `core:control-root` (full) | Stub: `isAvailable=false`, always `ROOT_DENIED`. No libsu dependency while stub. |

## Engaged vs Active

- Strategies that write keys report **Engaged** when the int is non-zero (configuration on).
- They do **not** evaluate OEM preconditions (PD charger, battery ≥ N%, game foreground).
- `ControlStateBridge.fromEngaged` uses `PreconditionChecker`:
  - all met → `Active(sinceEpochMs)` (timestamp preserved across refresh when already Active)
  - otherwise → `PendingConditions(unmet)`

## Writable key allowlist

`com.chargepilot.core.model.WritableSettingsKeys` is code-side defense-in-depth. Loader validation fails the build if a write-capable registry entry references a non-allowlisted key. Today: `pass_through` only.

## Disclosure gate

First enable of a direct method navigates to `feature:disclosure` and requires two checkboxes. Acknowledgement is stored in `UserPreferencesDataSource`. Official guidance does not require disclosure.
