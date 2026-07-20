# Plan 029: Unit tests for OfficialGuidanceStrategy

> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/control`

## Status

- **Priority**: P2 · **Effort**: S · **Risk**: LOW · **Depends on**: 002 · **Category**: tests  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

Official guidance is the universal path. Strategy must return `NavigatedToSettings` (not Success) and `Failed(UNSUPPORTED_DEVICE)` when activity does not resolve. Untested regressions could open wrong screens or claim Success.

## Current state

`OfficialGuidanceStrategy.kt:36-68` — builds Intent from `officialIntent`, resolveActivity null → Failed, startActivity → NavigatedToSettings.

Optional hardening: if `openOfficialSettings` fallthrough risk remains in HomeViewModel, change Home to `strategyFor(OFFICIAL_GUIDANCE)` only (small fix allowed here).

## Scope

**In**: `core/control/src/test/.../OfficialGuidanceStrategyTest.kt`; optionally HomeViewModel openOfficialSettings fix.

**Out**: OEM-specific intent research (024).

## Steps

1. Robolectric or shadow PackageManager: unresolved → Failed; resolved → NavigatedToSettings (may need Activity context flags).
2. If Robolectric heavy: extract Intent-building pure function and unit-test that + mock resolve.
3. Fix openOfficialSettings fallthrough if still present.

**Verify**: `./gradlew :core:control:test`

## Done criteria

- [ ] Navigated vs Failed covered
- [ ] openOfficialSettings cannot fall through to write strategies
- [ ] README 029 DONE
