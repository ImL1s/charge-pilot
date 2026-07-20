# Plan 004: Fix SEP/SEM vs One UI minRomVersion matching

> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/device core/capability core/model`

## Status

- **Priority**: P1 · **Effort**: M · **Risk**: MED · **Depends on**: 003 preferred · **Category**: bug  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

Samsung rules require `minRomVersion` flavor `ONE_UI` version `6.0`. When `ro.build.version.oneui` is missing, `DeviceDetector` stores SEP numeric majors like `"11.05"` (SEP, ~One UI 2.x) as `RomVersion.version`. `CapabilityRegistry.compareVersions("11.05", "6.0")` returns positive → **false capability match** on old Samsung devices. Users see unsupported controls/guidance as if they had One UI 6+.

## Current state

`DeviceDetector.kt:60-74` — SEP path:

```kotlin
systemPropertyReader.read("ro.build.version.sep")?.let { raw ->
    val numericVersion = parseSepCodeToNumericVersion(raw) // e.g. "11.05"
    return RomVersion(RomFlavor.ONE_UI, numericVersion, raw, displayLabel)
}
```

`CapabilityRegistry.kt:37-42` — compares `rom.version` to required One UI version.

`DeviceDetectorTest` covers oneui + display labels; **no** test that SEP-only fails One UI 6.0 rule match.

## Scope

**In**:
- `core/device/.../DeviceDetector.kt` (+ tests)
- `core/capability/.../CapabilityRegistry.kt` (+ tests) if match policy needs registry-side fix
- Optionally `core/model` if RomVersion gains `comparableOneUiVersion: String?`

**Out**:
- Changing JSON brand matrix content except tests
- Xiaomi HyperOS flavor fix (LOW confidence; separate if needed)

## Recommended fix (pick one, document in commit)

**Preferred**: Only treat `ro.build.version.oneui` (parsed MMmmpp → `major.minor`) as comparable for `minRomVersion` with flavor ONE_UI. For SEP/SEM-only devices:

- Still show human `displayLabel` on DeviceProfileCard
- Set a flag or use `version` that **fails** `minRomVersion` One UI comparisons (e.g. leave `version` as `"0"` for compare, keep raw in `raw`/`displayLabel`) **or** map SEP→approximate One UI major via existing `SEP_MAJOR_TO_ONEUI_APPROX` **only if** you also compare using the **mapped** One UI major (2.x not 11.x)

**Do not** leave SEP major `11` comparable as One UI 11.

## Steps

### Step 1: Failing tests first

Add `DeviceDetectorTest` / `CapabilityRegistryTest`:

- Fake props: only `ro.build.version.sep=110500` → detect profile → resolve against a rule requiring ONE_UI 6.0 → **must not match**
- Fake props: `ro.build.version.oneui=60101` → must match ONE_UI 6.0

**Verify**: tests fail on current code.

### Step 2: Implement fix

Apply preferred approach; keep `displayLabel` useful for UI.

**Verify**: tests pass; `./gradlew :core:device:test :core:capability:test`

### Step 3: Regression matrix

Hand-built profile SM-S91xx with romVersion One UI 6.1 still matches samsung-galaxy-s2x rule.

## Done criteria

- [ ] SEP-only device does not satisfy minRomVersion ONE_UI 6.0
- [ ] Real oneui code still satisfies correctly
- [ ] Tests document both cases
- [ ] `plans/README.md` 004 → DONE

## STOP

- Unclear whether product wants SEP-mapped One UI estimates for gating → report options; do not ship silent over-match.

## Maintenance

- Reviewers: check no double-counting SEP major as One UI major.
