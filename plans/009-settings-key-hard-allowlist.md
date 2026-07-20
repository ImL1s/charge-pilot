# Plan 009: Hard allowlist for writable settings keys

> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/control-writesettings core/control-shizuku core/capability core/model`

## Status

- **Priority**: P1 · **Effort**: M · **Risk**: MED · **Depends on**: 003 · **Category**: security  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

Strategies accept any registry key that is `system`/`int` and matches `[A-Za-z0-9_.:-]+`. Regex stops shell injection, **not** semantic damage. A bad PR or future OTA could write arbitrary system ints as 0/1. SECURITY.md already lists wrong key as a threat. Defense-in-depth: code allowlist independent of JSON.

## Current state

- Bundled key today: `pass_through` only (`capabilities-v1.json`).
- `ShizukuStrategy.descriptorAllowsShizuku` / `WriteSettingsStrategy.descriptorAllowsWrite` — no name allowlist.
- `ShizukuSettingsUserService.requireSafeKey` — charset only.

## Scope

**In**:
- New shared allowlist (prefer `core/model` or `core/control` object `WritableSettingsKeys`)
- Both strategies + UserService
- Loader validate: if settingsKey present and method is write-capable, key must be allowlisted
- Tests: unknown key → reject

**Out**: Implementing OTA; expanding allowlist to speculative OEM keys without evidence.

## Steps

1. Define `object AllowedSystemSettingsKeys { val WRITABLE_INT = setOf("pass_through") }` (or map namespace→keys).
2. Strategies: reject if `key.key !in allowlist`.
3. UserService: same reject (RemoteException).
4. Loader `validate`: fail fast on bundled asset with unknown key (keeps CI honest).
5. Tests: descriptor with key `airplane_mode_on` → Failed/UNSUPPORTED; `pass_through` still allowed by allowlist check (write may still fail without permission).

**Verify**: `./gradlew :core:capability:test :core:control-writesettings:test` (+ shizuku pure tests)

## Done criteria

- [ ] Unknown keys cannot be written even if registry lists them
- [ ] `pass_through` still accepted by allowlist
- [ ] Loader rejects bad fixtures in tests
- [ ] README 009 DONE

## STOP

- Product needs a new key → add to allowlist **and** registry in same PR with evidence; do not weaken to “any key”.

## Maintenance

- Plan 025 (new OEM key) must extend this set.
