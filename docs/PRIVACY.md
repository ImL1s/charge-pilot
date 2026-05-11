# Privacy

Charge Pilot does not collect, transmit, or store any personal information. There is no analytics SDK. There is no crash reporter. There is no telemetry.

## What stays on your device

- The detected device profile (manufacturer, model, Android version, ROM version).
- Operation history (which capabilities you have toggled and when).
- Your preferences (theme, dynamic-color toggle, advanced-mode opt-ins).

These are stored only in the app's private storage on your phone. They never leave the device.

## What the app fetches from the network

Exactly one HTTPS request, made non-blocking on app startup:

- The latest `capabilities-v1.json` registry from a public GitHub raw URL. This is the same data file that ships bundled in the APK; the network fetch only updates the local cache so newly added device support reaches you faster than a full app update.

The fetch sends the standard HTTP request headers (User-Agent, Accept). It does not include any device identifier, location, or user identifier.

## Permissions and what we use them for

- `INTERNET`, `ACCESS_NETWORK_STATE` — only for the registry fetch above.
- `WRITE_SETTINGS` (special access, user-grantable) — only when you explicitly press a "Try it" button in the disclosure flow, and only on whitelisted devices.
- `PACKAGE_USAGE_STATS` (special access, user-grantable) — only used to evaluate the "game in foreground" precondition. Never reported off-device.

## What we never do

- No accessibility-service automation.
- No `QUERY_ALL_PACKAGES`.
- No background settings changes.
- No reading SMS, contacts, location, microphone, or camera.

## Source of truth

The source code is public under AGPL-3.0-or-later. Any deviation from the above is a bug; please file one.
