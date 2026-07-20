# Plan 025: Spike — next project-verified writable key (Play Layer-2)

> **Drift check**: registry/allowlist may change only after verification.

## Status

- **Priority**: P2 · **Effort**: L · **Risk**: HIGH if keys ship cold · **Depends on**: 009, 003 · **Category**: direction  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

README Layer 2 claims WRITE_SETTINGS control on whitelisted devices in **both** flavors. Live `capabilities-v1.json` has **no** `WRITE_SETTINGS_KEY` methods — only Samsung `SHIZUKU_RPC` (full). `WriteSettingsStrategy` is implemented but unused. Play APK is guidance-only for control. Brand-methods explicitly waits for verified keys on OnePlus/Xiaomi.

## Non-goals

- Shipping speculative keys without before/after ADB evidence
- Accessibility automation

## Spike process (mandatory)

1. Pick 1–2 high-volume models from brand-methods candidates.
2. On device with WRITE_SETTINGS granted:
   - `adb shell settings list system|secure|global` baselines
   - Toggle OEM UI feature; diff settings
   - Attempt third-party write of candidate key; read back (plan 008 semantics)
3. Record evidence video/log per CONTRIBUTING.
4. Only then: add registry rule with `WRITE_SETTINGS_KEY`, allowlist entry (009), tests, brand-methods row, evidence PROJECT_VERIFIED.

## Deliverables

- `docs/research/writable-key-<oem>-<model>.md` with commands + results (no secrets)
- Optional PR with JSON + allowlist + tests **only if** verified

## Done criteria

- [ ] Research artifact committed or attached
- [ ] If code ships: allowlist + registry + tests + evidence
- [ ] README 025 DONE or BLOCKED (no device)

## STOP

- Key write works only with shell/Shizuku, not WRITE_SETTINGS → document; do not mark as Layer-2 Play path.
- Any key affects charging in unsafe ways without clear OFF → do not ship.
