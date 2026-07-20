# Plan 019: Remove unused libsu/JitPack until Root ships

> **Drift check**: `git diff --stat 1f293cf..HEAD -- settings.gradle.kts gradle/libs.versions.toml core/control-root app/build.gradle.kts`

## Status

- **Priority**: P2 · **Effort**: S · **Risk**: LOW · **Depends on**: none · **Category**: dependencies  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`RootStrategy` is a skeleton (`isAvailable = false`, always ROOT_DENIED) with **no** libsu imports. Full APK still depends on JitPack `libsu` and the whole project enables `maven("https://jitpack.io")` for this dead path. Supply-chain cost for zero behavior. Advanced full UI is Shizuku-centric.

## Current state

- `core/control-root/.../RootStrategy.kt` — stub
- `core/control-root/build.gradle.kts` — `implementation(libs.libsu.core)`
- `settings.gradle.kts` — jitpack.io
- `app` fullImplementation control-root

## Scope

**In**: Remove libsu dependency from control-root; remove jitpack repo **if** no other coordinate needs it (verify with `rg jitpack|com.github` in gradle files); keep RootStrategy stub OR hide ROOT from UI — do not delete full flavor module without care (Hilt map may expect binding).

**Out**: Implementing real root shell control.

## Steps

1. `rg -n 'libsu|jitpack|com.github' --glob '*.gradle.kts' --glob 'libs.versions.toml'`
2. Remove libsu from control-root and catalog entry if unused.
3. Remove jitpack repository if unused.
4. Keep RootModule binding stub so DI map stable, or remove ROOT from multibinding if safe — prefer keep stub without libsu.
5. `./gradlew :app:assembleFullDebug :app:assembleFullRelease`

**Verify**: Full assemble works; no jitpack resolution in build if removed.

## Done criteria

- [ ] No libsu dependency while Root is stub
- [ ] JitPack removed if unused
- [ ] Full flavor builds
- [ ] README 019 DONE

## STOP

- Another dependency needs JitPack → remove only libsu; leave repo.
- Product wants root next week → implement properly instead of remove (report).
