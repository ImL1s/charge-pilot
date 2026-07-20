# Plan 011: Implement UserPreferences DataStore (consent + theme hooks)

> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/datastore`

## Status

- **Priority**: P1 · **Effort**: M · **Risk**: MED · **Depends on**: none · **Category**: tech-debt  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`UserPreferencesDataSource` is an empty stub. Disclosure ack (plan 012), theme/dynamic color (design), and future flags have nowhere to persist. Design asked for Proto DataStore; history already uses **Preferences** DataStore + kotlinx.serialization JSON successfully (`OperationHistoryDataSource`). Prefer **matching history’s Preferences pattern** unless you deliberately introduce protobuf (larger). Document the choice in a short comment/ADR line.

## Current state

```kotlin
// UserPreferencesDataSource.kt
/**
 * Stub. The full implementation will use Proto DataStore. For now this returns
 * defaults so the rest of the app can compile and run end-to-end.
 */
@Singleton
class UserPreferencesDataSource @Inject constructor()
```

History exemplar: `OperationHistoryDataSource.kt` — `preferencesDataStore`, `@IoDispatcher`, `Json` encode/decode.

## Scope

**In**:
- `core/datastore/.../UserPreferencesDataSource.kt` — real implementation
- Optional `UserPreferences` data class in `core/model` or datastore module
- Hilt: already injectable as concrete class; no interface required unless clean
- Unit tests with DataStore testing or in-memory if practical; else skip heavy Robolectric and test pure encode helpers

**Minimum fields**:
- `acknowledgedCapabilityIds: Set<String>` (for plan 012)
- Optional: `useDynamicColor: Boolean = true`, `themeMode: SYSTEM|LIGHT|DARK` (can default-only if UI not wired yet)

**Out**: Full theme UI wiring (can be follow-up); Proto migration; history schema (013).

## Steps

1. Define serializable prefs model.
2. Implement DataStore name `user_preferences` with JSON or typed preferences keys.
3. API:
   - `val preferences: Flow<UserPreferences>`
   - `suspend fun acknowledgeDisclosure(capabilityId: String)`
   - `suspend fun hasAcknowledged(capabilityId: String): Boolean`
4. Keep defaults when empty.
5. Add tests if DataStore test rules available; otherwise pure serialization tests.

**Verify**: `./gradlew :core:datastore:compileDebugKotlin` (+ test if added); app still assembles.

## Done criteria

- [ ] Stub replaced with working persistence
- [ ] Acknowledge/hasAcknowledged APIs exist for plan 012
- [ ] Assemble debug succeeds
- [ ] README 011 DONE

## STOP

- You choose Proto and need new plugins → OK only if catalog + build-logic updated cleanly; else Preferences.
- Do not block on perfect theme UI.
