# Plan 021: Architecture docs + stale design/NOTICE/security contact fixes

> **Drift check**: `git diff --stat 1f293cf..HEAD -- docs SECURITY.md NOTICE CODE_OF_CONDUCT.md .github/ISSUE_TEMPLATE README.md`

## Status

- **Priority**: P3 · **Effort**: M · **Risk**: LOW · **Depends on**: 001 for privacy truth · **Category**: docs  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`docs/architecture/` is empty. Design matrix still claims Samsung WRITE_SETTINGS S1 while registry is Shizuku-first S2. NOTICE points to missing `:app:licenseReport`. SECURITY/CoC say “email maintainers” with no address. Superpowers plans look like open checklists for shipped work — agents re-execute obsolete paths.

## Scope

**In**:
- `docs/architecture/overview.md` — module graph from `settings.gradle.kts`, flavor deps (`fullImplementation` shizuku/root)
- `docs/architecture/control-strategies.md` — orchestrator + four strategies + Engaged vs Active
- Banner on `docs/superpowers/plans/*.md` and design: **historical snapshot; live truth = README + brand-methods + capabilities-v1.json**
- Fix `NOTICE` licenseReport line → list major deps or “see gradle/libs.versions.toml”
- `SECURITY.md` / `CODE_OF_CONDUCT.md` — GitHub private vulnerability reporting link and/or real contact the maintainer provides (**do not invent email** — if unknown, use GitHub Security Advisories instructions only)
- `.github/ISSUE_TEMPLATE/device_report.yml` — add `SMART_CHARGING`, `CUSTOM_CHARGE_LIMIT`
- Optional short README “What works today” section

**Out**: Regenerating full DeviceCompatibilityMatrix automation (can describe manual process); rewriting entire design doc.

## Steps

1. Write architecture overview + control strategies from **current** code (read settings.gradle.kts, app build.gradle.kts, ControlOrchestrator, strategies).
2. Mark superpowers plans archived.
3. Fix NOTICE, issue template, security contact.
4. Cross-link brand-methods.

**Verify**: files exist; no false OTA claims reintroduced (respect plan 001).

## Done criteria

- [ ] docs/architecture has ≥2 useful markdown files
- [ ] Historical plans labeled
- [ ] device_report capability list matches enum
- [ ] SECURITY has actionable reporting path
- [ ] README 021 DONE

## STOP

- Maintainer contact unknown and Security Advisories cannot be enabled by you → document “open private GH security advisory” as the path only.
