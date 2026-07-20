# Plan 024: Spike — deeper official intents for non-Samsung OEMs

> **Drift check**: N/A for research spike; code changes only if intent verified.

## Status

- **Priority**: P3 · **Effort**: M · **Risk**: LOW · **Depends on**: none · **Category**: direction  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

Layer 1 (official guidance) is the universal value. Pixel/OnePlus/HONOR often use generic `SETTINGS` / `BATTERY_SAVER_SETTINGS` while guidance text describes deeper Battery health paths. Samsung already has component-level investment. Improving `officialIntent` + `guidanceSteps` in `capabilities-v1.json` helps the non-Samsung majority without privileged writes.

## Current state

- `OfficialGuidanceStrategy` resolves intent or fails cleanly (good).
- Registry: HONOR `SMART_CHARGING` + SETTINGS; Pixel BATTERY_SAVER_SETTINGS; OnePlus SETTINGS.
- Spec mentioned HONOR Game Manager deep links historically — may not match current OEM UI.

## Scope

**Spike deliverable** (write to `docs/research/official-intents-YYYY-MM-DD.md` or `plans/024-notes.md`):

- Per OEM (Pixel, OnePlus, Xiaomi, HONOR, Huawei): candidate intent action/component, `adb shell cmd package resolve-activity` evidence, fallbackPath accuracy
- Only after device verification: PR updating `capabilities-v1.json` + `docs/brand-methods.md`

**Out**: Inventing deep links without device evidence; WRITE_SETTINGS keys (025).

## Steps

1. List current officialIntent per rule from JSON.
2. On real devices or public OEM docs, find better activities.
3. Validate with resolveActivity behavior matching OfficialGuidanceStrategy.
4. Propose JSON patches; run `:core:capability:test`.

## Done criteria

- [ ] Research note with evidence links/commands
- [ ] Any JSON change is evidence-backed
- [ ] README 024 DONE or BLOCKED with “needs devices”

## STOP

- No device access → complete research-from-docs only; mark BLOCKED for JSON changes.
