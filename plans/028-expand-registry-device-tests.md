# Plan 028: Expand CapabilityRegistry matching + DeviceDetector OEM tests

> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/capability/src/test core/device/src/test`

## Status

- **Priority**: P2 · **Effort**: S · **Risk**: LOW · **Depends on**: 002, 004 · **Category**: tests  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

Registry matching is the product differentiator. Current tests miss unknown manufacturer strings, flavor mismatch, modelRegex miss, multi-rule merge, non-numeric version parts. DeviceDetector tests are Samsung-heavy; OnePlus/Honor/Huawei/Google paths untested.

## Scope

**In**: `CapabilityRegistryTest.kt`, `DeviceDetectorTest.kt` (+ FakeReader patterns already used).

**Out**: Changing production match logic except bugs found by tests (then fix under 004 rules).

## Steps

1. Table-driven CapabilityRegistry cases for each branch in `matches()`.
2. DeviceDetector: oxygen / magic / emui / google release props; oneui short code `< 10000` raw passthrough.
3. `./gradlew :core:capability:test :core:device:test`

## Done criteria

- [ ] New cases for listed branches
- [ ] Tests pass
- [ ] README 028 DONE
