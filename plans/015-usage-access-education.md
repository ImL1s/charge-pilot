# Plan 015: Usage-access education for GameInForeground precondition

> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/foreground core/domain feature/home app/src/main`

## Status

- **Priority**: P2 · **Effort**: M · **Risk**: MED · **Depends on**: 007 · **Category**: correctness | direction  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

Samsung verified path includes `GameInForeground`. `ForegroundDetector` uses `UsageStatsManager`; without PACKAGE_USAGE_STATS grant, queries return empty → checker reports unmet. There is **no** education UI / deep link (unlike WRITE_SETTINGS / Shizuku). Users staring at Charge Pilot also cannot satisfy game-in-foreground — Active may be rare. At minimum distinguish **permission missing** vs **game not foreground**.

## Current state

- Manifest: `PACKAGE_USAGE_STATS` declared.
- `ForegroundDetector` — no grant check.
- `DefaultPreconditionChecker` — false when cannot evaluate (conservative).
- PrivilegedSetupNavigator has WRITE_SETTINGS + Shizuku openers only.

## Scope

**In**:
- Detect usage access granted (AppOps / `UsageStatsManager` query emptiness heuristic documented carefully)
- UI on Home/Samsung setup card: “Grant usage access to verify game precondition”
- Intent: `Settings.ACTION_USAGE_ACCESS_SETTINGS` (and package-specific if available)
- Optional model: `PreconditionEvaluation` with Unmet vs Unknown — only if small; else show separate setup chip

**Out**: Spoofing game foreground (explicit non-goal in SECURITY.md); accessibility hacks.

## Steps

1. Add `ForegroundDetector.hasUsageAccess(): Boolean` (AppOps `OPSTR_GET_USAGE_STATS` mode allowed).
2. Home UI when Samsung PAUSE_PD and !hasUsageAccess → CTA open usage settings.
3. Copy: Engaged key can still show “configured on; waiting for game” vs “cannot verify game without usage access”.
4. Do **not** treat missing permission as met.

**Verify**: assemble + manual steps on device; unit test hasUsageAccess false path if faked.

## Done criteria

- [ ] User can open usage access settings from app
- [ ] UI explains why Active may not show without grant / without game
- [ ] No spoofing
- [ ] README 015 DONE

## STOP

- AppOps API differences by OEM block detection → use best-effort + always offer settings link.
