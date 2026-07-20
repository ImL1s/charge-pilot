# Plan 007: Replace Home 2s poll with lifecycle-safe refresh

> **Drift check**: `git diff --stat 1f293cf..HEAD -- feature/home/impl/src/main/kotlin/com/chargepilot/feature/home/impl/HomeScreen.kt feature/home/impl/src/main/kotlin/com/chargepilot/feature/home/impl/HomeViewModel.kt`

## Status

- **Priority**: P1 · **Effort**: M · **Risk**: MED · **Depends on**: 006 · **Category**: perf | bug  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`HomeScreen` runs an infinite `delay(2_000)` → `refresh()` with **no** lifecycle stop, plus `ON_RESUME` refresh. Each tick rebuilds every capability row (Shizuku `settings` shell, UsageStats, six package probes). Concurrent `viewModelScope.launch` jobs overwrite `_state` without single-flight → status messages vanish; wasted binder/process work.

## Current state

```kotlin
// HomeScreen.kt:34-41
LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
LaunchedEffect(viewModel) {
    while (true) {
        delay(2_000L)
        viewModel.refresh()
    }
}
```

```kotlin
// HomeViewModel.kt:54-68 — refresh launches without cancelling prior job
viewModelScope.launch {
    val updatedRows = withContext(ioDispatcher) { ... }
    _state.value = ready.copy(...)  // uses stale `ready` snapshot
}
```

## Scope

**In**: `HomeScreen.kt`, `HomeViewModel.kt` (refresh/load/refreshCapabilityState/refreshHome coordination).

**Out**: Battery BroadcastReceiver product redesign (optional enhancement); Shizuku cache (010); package-change broadcast (optional).

## Target behavior

1. **ON_RESUME** (or `Lifecycle.State.STARTED`): one full refresh.
2. **While STARTED and at least one row is Engaged/Pending/Active**: optional slower live poll (e.g. 5–10s) **or** battery sticky re-read only — **not** unconditional 2s forever including when app is STOPPED.
3. **Single-flight**: `private var refreshJob: Job?`; cancel previous before new full refresh; or `Mutex`.
4. **State merge**: when writing Ready, preserve newer `statusMessage` if the completing job is older than a newer action (use generation counter or only clear status on explicit dismiss).
5. Setup method readiness (Shizuku installed/running) need **not** re-run every poll — at minimum separate “live conditions” (battery/game/key state) from “setup probes” (packages).

## Steps

### Step 1: Single-flight in ViewModel

Add job cancel pattern for `refresh`, `refreshHome`, `refreshCapabilityState`. Document generation id if needed for status.

**Verify**: unit test or manual reasoning + compile; prefer a small test with fake delay if feasible.

### Step 2: Replace LaunchedEffect loop

Use `androidx.lifecycle.compose.LifecycleResumeEffect` / `repeatOnLifecycle(STARTED)` pattern:

- On start: refresh once
- Optional: while STARTED, if any capability needs live updates, delay 5s and refresh **conditions only**

Remove unbounded 2s loop.

**Verify**: `./gradlew :feature:home:impl:compilePlayDebugKotlin` (or assembleFullDebug) SUCCESS.

### Step 3: Manual checklist (document in PR)

- Open Home, background app 30s → logcat should not keep refreshing every 2s
- Resume → one refresh
- Toggle control → status message remains until next user action or timed clear (≥ few seconds)

## Done criteria

- [ ] No unconditional infinite 2s poll without lifecycle
- [ ] Overlapping refreshes cancelled or serialized
- [ ] Assemble + home unit tests pass
- [ ] README 007 DONE

## STOP

- Product owner insists on hard real-time Active without any polling → stop and propose Flow-based battery only.
- Changes require new libraries not in catalog → stop.

## Maintenance

- Pair with plan 010 (Shizuku) and 015 (usage access) so remaining polls are cheaper.
