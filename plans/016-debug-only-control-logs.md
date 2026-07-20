# Plan 016: Gate control operation logs to debug only

> **Drift check**: `git diff --stat 1f293cf..HEAD -- feature/home/impl/src/main/kotlin/com/chargepilot/feature/home/impl/HomeViewModel.kt app/src/main/kotlin/com/chargepilot/app/ChargePilotApp.kt`

## Status

- **Priority**: P2 · **Effort**: S · **Risk**: LOW · **Depends on**: none · **Category**: security  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`HomeViewModel` uses `android.util.Log.i/w` for capability actions and history failures. Timber is only planted in DEBUG (`ChargePilotApp`). Release builds still emit control method/id/result to logcat.

## Current state

```kotlin
Log.i("ChargePilotHome", "Capability action method=$method id=$capabilityId result=$result")
```

## Scope

**In**: `HomeViewModel.kt` (and any similar Log in feature control paths you touch). Prefer `Timber.d` / `Timber.w`.

**Out**: Rewriting all device SystemPropertyReader logs unless trivial.

## Steps

1. Replace `Log.i/w` with Timber.
2. Ensure no success-path info logs without DEBUG tree (Timber no-ops without tree — OK).
3. Keep failures as Timber.w.

**Verify**: `rg -n 'android.util.Log' feature/home/impl/src` → none (or only justified). `./gradlew :feature:home:impl:compilePlayReleaseKotlin` or assemble.

## Done criteria

- [ ] No unconditional Log.i for capability actions in home
- [ ] README 016 DONE
