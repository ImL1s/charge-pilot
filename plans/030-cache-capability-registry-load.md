# Plan 030: Cache capability registry load in memory

> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/capability/src/main`

## Status

- **Priority**: P3 · **Effort**: S · **Risk**: LOW · **Depends on**: 003 · **Category**: perf  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`CapabilityRegistry` comments say “loaded once” but `resolve`/`rules` always call `loader.load()` → assets open + JSON decode + validate. Home and Brands each pay full cost. Small today; contradicts architecture and hurts as JSON grows.

## Current state

```kotlin
// CapabilityRegistry.kt:19-26
suspend fun resolve(profile: DeviceProfile): List<CapabilityDescriptor> {
    val registry = loader.load()
    return registry.rules.filter { ... }.flatMap { it.capabilities }
}
```

`BundledCapabilityRegistryLoader.load()` always reads asset.

## Scope

**In**: `CapabilityRegistry` and/or loader memoization with Mutex; test with counting fake loader.

**Out**: Disk OTA cache (026); changing JSON schema.

## Steps

1. Cache `CapabilityRegistrySnapshot` after first successful load in `@Singleton` registry.
2. Provide `clearCache()` internal/visible for tests only if needed.
3. Test: two `resolve` calls → loader.load invoked once.
4. `./gradlew :core:capability:test`

## Done criteria

- [ ] Second resolve does not re-read assets (proven by test)
- [ ] README 030 DONE

## STOP

- Future OTA apply needs invalidation → add clearCache when OTA lands; document in 026.
