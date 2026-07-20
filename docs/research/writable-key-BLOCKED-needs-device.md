# Spike: next WRITE_SETTINGS key (Play Layer-2)

**Date**: 2026-07-21  
**Status**: **BLOCKED — needs real OEM device evidence**  
**Plan**: 025

## Why blocked

Shipping a new system settings key without before/after ADB proof can brick charging behavior or fail silently (Samsung `pass_through` already requires Shizuku on verified devices). This executor session has **no** attached OEM device for:

```bash
adb shell settings list system
adb shell settings list secure
adb shell settings list global
# toggle OEM UI feature, then diff
```

## Current state

- Bundled registry has **no** `WRITE_SETTINGS_KEY` methods for Play control.
- Allowlist (`WritableSettingsKeys`) contains only `pass_through` (Shizuku path on Samsung full flavor).
- `WriteSettingsStrategy` is implemented with read-after-write + allowlist gates.

## Process when a device is available

1. Grant WRITE_SETTINGS to Charge Pilot.
2. Baseline `settings list *`.
3. Toggle OEM feature in official UI; diff keys.
4. Attempt third-party write of candidate; re-read (plan 008).
5. If WRITE_SETTINGS cannot change the key (shell/Shizuku only) → document; do **not** mark Layer-2 Play.
6. Only then: registry + allowlist + tests + brand-methods + evidence in one PR.

## Code changes in this spike

**None** (no invented keys).
