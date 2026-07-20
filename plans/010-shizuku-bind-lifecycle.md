# Plan 010: Fix Shizuku UserService bind/unbind lifecycle

> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/control-shizuku`

## Status

- **Priority**: P1 · **Effort**: M · **Risk**: MED · **Depends on**: 007 recommended · **Category**: bug  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`ShizukuStrategy.bindService` calls `Shizuku.bindUserService` then `withTimeoutOrNull(5s)`. On timeout it returns null **without** `unbindUserService`. Concurrent binds overwrite `cachedConnection`. Home refresh (historically every 2s) multiplies zombies. Leads to flaky SHIZUKU_NOT_RUNNING and resource leaks.

## Current state

```kotlin
// ShizukuStrategy.kt:174-203
Shizuku.bindUserService(userServiceArgs(), connection)
withTimeoutOrNull(BIND_TIMEOUT_MS) { deferred.await() }
// no unbind on timeout; no Mutex
```

Repo has zero `unbindUserService` call sites.

## Scope

**In**: `ShizukuStrategy.kt` primarily; optional unit-testable bind coordinator extraction.

**Out**: Changing AIDL API; RootStrategy; Advanced UI copy.

## Steps

1. Add `Mutex` (or single-flight) around bind.
2. On timeout/failure: `Shizuku.unbindUserService(args, connection, true)` if API allows; clear caches.
3. On `onServiceDisconnected`: clear cache (already partial).
4. Reuse ping-live cached service; do not stack connections.
5. Consider `@Application` lifecycle: unbind when process idle is optional; at least unbind on timeout.
6. Manual test notes for Full flavor + Shizuku running.

**Verify**: `./gradlew :core:control-shizuku:compileFullDebugKotlin` or `:app:assembleFullDebug` SUCCESS. If pure unit tests hard, document manual steps.

## Done criteria

- [ ] Timeout path unbinds / clears connection
- [ ] Concurrent bind serialized
- [ ] Full debug assemble succeeds
- [ ] README 010 DONE

## STOP

- Shizuku API version lacks unbind overload expected → consult Shizuku 13.1.5 docs; do not guess flags.
- Cannot test without device → implement carefully + leave manual checklist; do not claim device-verified.
