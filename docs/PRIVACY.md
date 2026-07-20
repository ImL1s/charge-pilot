# Privacy

Charge Pilot does not collect, transmit, or store any personal information. There is no analytics SDK. There is no crash reporter. There is no telemetry.

## What stays on your device

- The detected device profile (manufacturer, model, Android version, ROM version).
- Operation history (which capabilities you have toggled and when).
- Your preferences (theme, dynamic-color toggle, advanced-mode opt-ins).

These are stored only in the app's private storage on your phone. They never leave the device.

## Network

Charge Pilot does **not** perform network fetches of the capability registry in the current release.

- Registry data ships **bundled** in the APK (`assets/capabilities-v1.json`).
- Device profile and operation history never leave the device.
- There is no analytics, crash reporting, or "phone home" channel.

A future, optional signed OTA update of the registry may be added later; if it is, permissions and this document will be updated in the same change as the fetch code. Until then the app requires no network access for normal use.

## Permissions and what we use them for

- `WRITE_SETTINGS` (special access, user-grantable) — only when you explicitly press a control action after understanding the disclosure, and only for allowlisted keys on matching devices.
- `PACKAGE_USAGE_STATS` (special access, user-grantable) — only used to evaluate the "game in foreground" precondition. Never reported off-device.

The app does **not** declare or use `INTERNET` / `ACCESS_NETWORK_STATE` in the current release.

## What we never do

- No accessibility-service automation.
- No `QUERY_ALL_PACKAGES`.
- No background settings changes.
- No reading SMS, contacts, location, microphone, or camera.

## Source of truth

The source code is public under AGPL-3.0-or-later. Any deviation from the above is a bug; please file one.
