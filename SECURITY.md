# Security policy

## Reporting a vulnerability

Charge Pilot can read and (with explicit user consent) write Android system settings. We take security reports seriously.

**Please do not open a public issue.** Instead, email the maintainers privately. We will acknowledge within 72 hours and aim to publish a fix within 14 days for issues with confirmed user impact.

## Threat model

Charge Pilot's threat model focuses on:

1. **Device safety.** A misconfigured registry rule that writes the wrong key on the wrong device could put the phone in an unexpected charging state. The capability registry is therefore code-reviewed; per-device evidence is required for `availableMethods` that include any non-`OFFICIAL_GUIDANCE` method.
2. **Play Store policy compliance.** Modifying device settings without clear, reversible user intent is forbidden by Google Play; we treat any drift toward that as a high-priority bug.
3. **OTA registry tampering.** The capability registry is loaded over HTTPS from a public GitHub raw URL. A future hardening step (planned) is to attach a detached signature signed by a project-owned key. Until then, the on-device behavior remains driven by `availableMethods` lists, never by remote code.
4. **Privileged-mode misuse.** The `full` flavor adds Shizuku and root strategies. Both are off by default; the user must explicitly enable them and pass a disclosure step.

## Out of scope

- Bypass of OEM-imposed preconditions (e.g., the app does *not* attempt to spoof a "game in foreground" signal to coerce Samsung's pause-PD feature on while the user is not playing a game).
- Anything outside the device's local state.
