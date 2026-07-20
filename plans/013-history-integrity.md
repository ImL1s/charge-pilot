# Plan 013: Harden operation history against silent wipe

> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/datastore/src/main/kotlin/com/chargepilot/core/datastore/OperationHistoryDataSource.kt`

## Status

- **Priority**: P1 · **Effort**: S · **Risk**: LOW · **Depends on**: none · **Category**: bug  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`OperationHistoryDataSource.decode` returns `emptyList()` on any JSON failure. `append` then re-encodes empty + new record, **permanently destroying** prior audit log. Flow `.catch { emit(emptyList()) }` hides read errors. History is the product’s audit promise.

## Current state

```kotlin
// OperationHistoryDataSource.kt:42-56
suspend fun append(record: OperationRecord) {
    dataStore.edit { preferences ->
        val current = decode(preferences[RecordsJson].orEmpty())
        preferences[RecordsJson] = json.encodeToString((listOf(record) + current).take(MaxRecords))
    }
}
private fun decode(raw: String): List<OperationRecord> {
    if (raw.isBlank()) return emptyList()
    return runCatching { json.decodeFromString<List<OperationRecord>>(raw) }.getOrDefault(emptyList())
}
```

## Scope

**In**: `OperationHistoryDataSource.kt` + tests under `core/datastore`.

**Out**: Revert UI (014); changing MaxRecords unless needed.

## Steps

1. On decode failure: **do not** treat as empty for append. Options:
   - Keep previous raw under `records_json_backup` before overwrite
   - Or refuse append and log error; surface empty UI with “history corrupted” flag via sealed read result
2. Preferred: `decodeOrNull`; if null, `append` writes **only** if raw blank; if raw non-blank corrupt → store new record under quarantine key **or** backup old raw then start fresh **once** while Timber.e
3. Flow should not silently look healthy if corrupt — expose `HistoryLoadState.Corrupted` if easy; minimum Timber.e + preserve backup key
4. Tests: corrupt string → append does not drop ability to recover backup; or documents single backup write

**Verify**: `./gradlew :core:datastore:test` (add junit deps if missing)

## Done criteria

- [ ] Corrupt history cannot be clobbered without backup/quarantine
- [ ] Tests cover corrupt path
- [ ] README 013 DONE

## STOP

- DataStore migration complexity explodes → implement backup key only and report.
