# Plan 005: Align BatteryLevelAbove with ≥ semantics

> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/domain feature/home core/ui`

## Status

- **Priority**: P2 · **Effort**: S · **Risk**: LOW · **Depends on**: 002 · **Category**: bug  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

Design §4.3 and Samsung guidance use **battery ≥ 20%**. `DefaultPreconditionChecker` uses strict `level > precondition.percent`, so **exactly 20%** is unmet forever. UI strings say “above” in some places — pick one semantics and make code + copy match OEM ≥.

## Current state

```kotlin
// DefaultPreconditionChecker.kt:28-30
is Precondition.BatteryLevelAbove -> {
    val level = battery.batteryLevelPercent() ?: return false
    level > precondition.percent
}
```

Registry: `"percent": 20`. Design: `battery ≥ 20%`.

## Scope

**In**: `DefaultPreconditionChecker.kt`, its tests (plan 003), any user-facing “above 20%” strings that contradict ≥ (`core/ui`, `feature/home` strings).

**Out**: Changing registry percent values; PD detection algorithm.

## Steps

1. Change comparison to `level >= precondition.percent`.
2. Update unit tests: level 20 + threshold 20 → met; 19 → unmet.
3. Align strings to “battery at least N%” / “≥ N%” (en + zh-rTW if present).

**Verify**: `./gradlew :core:domain:test` + string grep for contradictory “above” only if still wrong.

## Done criteria

- [ ] `>=` used; tests cover 19/20/21
- [ ] User-facing copy consistent
- [ ] README 005 DONE

## STOP

- OEM official docs for a specific capability require strict `>` only → report with URL; do not change blindly.
