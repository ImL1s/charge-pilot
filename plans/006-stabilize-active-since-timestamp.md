# Plan 006: Stabilize ControlState.Active.sinceEpochMs

> **Drift check**: `git diff --stat 1f293cf..HEAD -- feature/home/impl/src/main/kotlin/com/chargepilot/feature/home/impl/HomeViewModel.kt core/domain`

## Status

- **Priority**: P1 · **Effort**: S · **Risk**: LOW · **Depends on**: none (pairs with 003 bridge extract if present) · **Category**: bug | perf  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`bridgeEngagedToActive` always does `Active(sinceEpochMs = System.currentTimeMillis())`. Every Home refresh mints a new Active instance even when conditions never changed → false “since when”, and StateFlow always emits new rows → full recomposition (amplifies the 2s poll in plan 007).

## Current state

```kotlin
// HomeViewModel.kt:325-335
private suspend fun bridgeEngagedToActive(...): ControlState {
    if (rawState !is ControlState.Engaged) return rawState
    val evaluation = preconditionChecker.check(descriptor.preconditions)
    return if (evaluation.allMet) {
        ControlState.Active(sinceEpochMs = System.currentTimeMillis())
    } else {
        ControlState.PendingConditions(unmet = evaluation.unmet)
    }
}
```

`readDisplayState` / `buildCapabilityRow` call this without previous row state.

## Scope

**In**: `HomeViewModel.kt` (and bridge helper if extracted in 003); tests for “preserve since”.

**Out**: Persisting since across process death (nice-to-have later with DataStore); UI display of since text.

## Steps

1. Change `buildCapabilityRow` / bridge to accept `previousState: ControlState?`.
2. When transitioning to Active:
   - if `previousState is Active` → reuse `previousState.sinceEpochMs`
   - else → `System.currentTimeMillis()`
3. When leaving Active (Pending/Inactive/Unknown) → next Active gets new timestamp.
4. Unit test: two sequential bridge calls with allMet true preserve since.

**Verify**: `./gradlew :feature:home:impl:test :core:domain:test`

## Done criteria

- [ ] Active timestamp stable across refresh when still Active
- [ ] Tests cover preserve + reset on re-engage
- [ ] README 006 DONE

## STOP

- If UI later needs process-durable since, extend in plan 011 prefs — do not invent Proto schema here.
