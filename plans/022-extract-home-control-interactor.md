# Plan 022: Extract control interactor from HomeViewModel

> **Drift check**: `git diff --stat 1f293cf..HEAD -- feature/home/impl core/domain core/control`

## Status

- **Priority**: P3 · **Effort**: L · **Risk**: MED · **Depends on**: 012, 007 · **Category**: tech-debt  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`HomeViewModel` (~487 lines) owns device load, strategy pick, write path, Shizuku setup, Samsung game stack, history append, Engaged→Active bridge, and status/history label maps. Any consent/history/control change is high blast radius. Design expected use cases in `:core:domain`; `CapabilityRepository` is unused.

## Current state

- `HomeViewModel.kt` — god object
- `CapabilityRepository` — unused thin wrapper
- Domain has only PreconditionChecker

## Scope

**In**:
- Extract `CapabilityControlInteractor` (or use cases) in `core/domain` or `core/control`:
  - resolve rows for profile
  - toggle enable/disable with orchestrator
  - setup privileged method
- Move Samsung package setup status mapping toward `PrivilegedSetupNavigator` consumers or `SamsungGameSetupController`
- HomeViewModel becomes state holder + effects
- Tests move to interactor
- Optionally wire or delete `CapabilityRepository`

**Out**: Full NiA pure layering rewrite of all features; redesign DataStore.

## Steps

1. Identify pure functions already extracted (bridge from 003/006).
2. Create interactor with constructor injection of orchestrator, registry, history, preferences, precondition checker.
3. Move `runCapabilityAction` / `buildCapabilityRow` logic.
4. Keep UI state mapping in VM or thin mapper.
5. Ensure tests cover toggle + disclosure gate at interactor level.
6. `./gradlew :feature:home:impl:test :app:assemblePlayDebug :app:assembleFullDebug`

## Done criteria

- [ ] HomeViewModel substantially thinner (target <250 lines or clear split files)
- [ ] Control write path unit-tested without Compose
- [ ] Both flavors assemble; tests pass
- [ ] README 022 DONE

## STOP

- Extraction breaks Hilt graph — fix modules carefully; do not introduce service locator.
- Scope creeps into Advanced/History refactors — stop after Home path.
