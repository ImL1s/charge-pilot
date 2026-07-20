# Security policy

## Reporting a vulnerability

Charge Pilot can read and (with explicit user consent) write Android system settings. We take security reports seriously.

**Please do not open a public issue.** Prefer GitHub's private [Security Advisories](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability) flow on this repository when available. We aim to acknowledge within 72 hours and publish a fix within 14 days for issues with confirmed user impact.

## Threat model

Charge Pilot's threat model focuses on:

1. **Device safety.** A misconfigured registry rule that writes the wrong key on the wrong device could put the phone in an unexpected charging state. The capability registry is therefore code-reviewed; per-device evidence is required for `availableMethods` that include any non-`OFFICIAL_GUIDANCE` method.
2. **Play Store policy compliance.** Modifying device settings without clear, reversible user intent is forbidden by Google Play; we treat any drift toward that as a high-priority bug.
3. **Remote registry (OTA) — not implemented.** The app currently loads only the bundled `assets/capabilities-v1.json`. There is no live HTTPS registry fetch, so remote tampering is not a live threat surface. If OTA is added later it must require integrity (detached signature / verified public key) before any network path is re-enabled — see plan 026.
4. **Privileged-mode misuse.** The `full` flavor adds Shizuku and root strategies. Both are off by default; the user must explicitly enable a Home action. Multi-step disclosure consent is tracked separately from the basic user-initiated control path.

## Out of scope

- Bypass of OEM-imposed preconditions (e.g., the app does *not* attempt to spoof a "game in foreground" signal to coerce Samsung's pause-PD feature on while the user is not playing a game).
- Anything outside the device's local state.
