# Plan 014: History one-tap revert with raw before/after

> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/model core/datastore feature/history feature/home/impl`

## Status

- **Priority**: P2 · **Effort**: M · **Risk**: MED · **Depends on**: 013, 008 · **Category**: direction | bug  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

Design History destination promises one-tap revert. `OperationRecord` KDoc says “audit and revert”. UI is read-only; records store human labels only (`"Inactive"`, `"Requested ON succeeded"`), not capability id / raw settings value. `reset()` on strategies forces `0`, not previous value.

## Current state

```kotlin
// Control.kt:74-82
data class OperationRecord(
    val id: String,
    val capability: CapabilityType, // not descriptor id
    val method: ControlMethod,
    val before: String?,
    val after: String?,
    val success: Boolean,
    val timestampMs: Long,
)
```

`HistoryScreen` — LazyColumn cards only.

## Scope

**In**:
- Extend `OperationRecord` with optional fields (keep serialization backward compatible with `ignoreUnknownKeys` / defaults):
  - `capabilityId: String? = null`
  - `settingsKeyName: String? = null`
  - `rawBefore: String? = null`
  - `rawAfter: String? = null`
  - `reverted: Boolean = false`
- Home `actionRecord` populates raw via strategy read before/after write
- History UI: Revert button when `success && method is direct && !reverted && rawBefore != null`
- HistoryViewModel.revert(id) → pick strategy → write rawBefore (or setEnabled false if rawBefore is 0/Inactive)
- Append a new history record for the revert attempt

**Out**: Reverting OFFICIAL_GUIDANCE navigations; multi-key transactions.

## Steps

1. Schema + migration: old records deserialize with nulls; Revert hidden when null.
2. Capture raw before write in HomeViewModel runCapabilityAction.
3. History CTA + ViewModel.
4. Tests: serialize new fields; revert invokes strategy with expected value (fake).

**Verify**: `./gradlew :core:model:test :feature:history:impl:test :feature:home:impl:test :app:assembleFullDebug`

## Done criteria

- [ ] Successful direct writes store raw before/after when available
- [ ] User can revert from History when data present
- [ ] Old records still display
- [ ] README 014 DONE

## STOP

- Cannot map CapabilityType-only old records to a strategy safely → only enable revert when capabilityId present.
- Revert would need Shizuku not running → show Failed status, do not crash.
