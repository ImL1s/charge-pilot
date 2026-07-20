# Research: official intents for non-Samsung OEMs

**Date**: 2026-07-21  
**Status**: BLOCKED for JSON changes — no physical device validation in this executor worktree.  
**Plan**: 024

## Current registry intents (bundled)

Summarized from `capabilities-v1.json` (guidance-first):

| OEM | Capability (examples) | Current intent | Notes |
|-----|----------------------|----------------|-------|
| Google Pixel | BATTERY_LIMIT_80 / ADAPTIVE | `BATTERY_SAVER_SETTINGS` or similar | Prefer Battery / Adaptive Charging deep links when verified |
| OnePlus | BYPASS / battery protection | generic `SETTINGS` | Community OTA notes describe Game / high-load paths — need resolveActivity proof |
| Xiaomi | Battery protection | OEM battery settings / SETTINGS | HyperOS renames often; negative list for POCO F7 bypass |
| HONOR | SMART_CHARGING | SETTINGS | Spec once mentioned Game Manager deep links — unverified |
| Huawei | SMART_CHARGING / CUSTOM limit | SETTINGS / EMUI battery | HMS / region variants |

## Validation commands (for maintainers with devices)

```bash
adb shell cmd package resolve-activity --brief -a android.settings.BATTERY_SAVER_SETTINGS
adb shell cmd package resolve-activity --brief -a android.settings.SETTINGS
# OEM-specific components once known:
adb shell cmd package resolve-activity --brief -n com.example/.BatteryHealthActivity
```

Match `OfficialGuidanceStrategy` behavior: unresolved → `Failed(UNSUPPORTED_DEVICE)`; resolved → `NavigatedToSettings`.

## Proposed next steps

1. On each OEM device, capture `resolve-activity` output for candidate actions/components.
2. Cross-check with current official support pages (link in PR).
3. Only then patch `officialIntent` + `guidanceSteps` + `docs/brand-methods.md` with `PROJECT_VERIFIED` or `OFFICIAL_DOC` evidence.

## JSON changes in this spike

**None.** Inventing deep links without devices would violate CONTRIBUTING evidence rules.
