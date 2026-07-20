# Plan 023: In-app device report export (About)

> **Drift check**: `git diff --stat 1f293cf..HEAD -- feature/about core/device .github/ISSUE_TEMPLATE`

## Status

- **Priority**: P2 · **Effort**: S · **Risk**: LOW · **Depends on**: none · **Category**: direction  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

Growth loop is community device reports (`device_report.yml`, CONTRIBUTING evidence). About screen only shows privacy/license. `DeviceDetector` already builds full `DeviceProfile`. Export-to-clipboard is adjacent-cheap and privacy-safe (local only).

## Current state

- `AboutScreen.kt` — static InfoCards
- `DeviceDetector.detect()` — manufacturer, model, codename, API, ROM, fingerprint
- Issue template fields for manual fill

## Scope

**In**: About feature: “Copy device report” button; format markdown matching issue template fields; optional Share intent. Inject DeviceDetector via ViewModel.

**Out**: Auto-filing GitHub issues with network (needs INTERNET — only after plan 001 policy); uploading profile off-device.

## Steps

1. `AboutViewModel` → detect profile, build markdown string.
2. Button copies to clipboard; snackbar “Copied”.
3. Include app version from BuildConfig if available via app module callback or about.impl dependency on app is wrong — use `PackageManager` version.
4. Strings en + zh-rTW.
5. Unit test: markdown contains model and manufacturer fields for a fake profile (extract pure formatter).

**Verify**: `./gradlew :feature:about:impl:test :app:assemblePlayDebug`

## Done criteria

- [ ] User can copy a device report from About
- [ ] No network upload
- [ ] README 023 DONE
