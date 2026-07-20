# Plan 018: Drop Material3 alpha pin onto Compose BOM

> **Drift check**: `git diff --stat 1f293cf..HEAD -- gradle/libs.versions.toml build-logic core/designsystem`

## Status

- **Priority**: P2 · **Effort**: S · **Risk**: MED · **Depends on**: none · **Category**: migration  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`compose-material3 = "1.4.0-alpha05"` overrides Compose BOM `2024.12.01`. UI uses stable Material3 APIs only (`MaterialTheme`, Scaffold, NavigationBar) — no `MaterialExpressiveTheme`. Shipping alpha without using alpha features adds churn/Play risk.

## Current state

`gradle/libs.versions.toml`:
```
compose-bom = "2024.12.01"
compose-material3 = "1.4.0-alpha05"
```
Library: `compose-material3 = { ..., version.ref = "compose-material3" }`

## Scope

**In**: `libs.versions.toml`, any direct material3 version pins, visual smoke of all tabs after resolve.

**Out**: Implementing full M3 Expressive redesign (separate product work).

## Steps

1. Remove version from material3 library coordinate so BOM manages it **or** pin to the BOM’s stable material3 version explicitly.
2. `./gradlew :app:assemblePlayDebug :app:assembleFullDebug`
3. Fix any API breakages (unlikely if only stable APIs used).
4. Quick manual UI check list: Home, Brands, History, Advanced, About — light/dark if easy.

**Verify**: assemble SUCCESS; `rg '1.4.0-alpha' gradle/` → no matches.

## Done criteria

- [ ] No material3 alpha pin
- [ ] Both flavors assemble
- [ ] README 018 DONE

## STOP

- Removing alpha breaks compile because something used experimental APIs → report file:line; either OptIn intentionally or replace API.
