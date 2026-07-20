# Plan 008: Read-after-write verification on settings control

> **Drift check**: `git diff --stat 1f293cf..HEAD -- core/control-writesettings core/control-shizuku`

## Status

- **Priority**: P1 · **Effort**: S · **Risk**: LOW · **Depends on**: 003 · **Category**: bug  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`WriteSettingsStrategy.setEnabled` treats `Settings.System.putInt` returning true as `ControlResult.Success`. Samsung S24U notes in registry: normal WRITE_SETTINGS often **does not** change `pass_through`. History then logs success while key stays off. Shizuku path trusts CLI exit code without re-read. Users and History must not lie.

## Current state

```kotlin
// WriteSettingsStrategy.kt:80-86
val ok = Settings.System.putInt(..., if (enabled) 1 else 0)
if (ok) ControlResult.Success else ControlResult.Failed(FailureReason.KEY_NOT_WRITABLE)
```

```kotlin
// ShizukuStrategy.kt:115-117
service.writeSystemSetting(key.key, if (enabled) "1" else "0")
ControlResult.Success
```

## Scope

**In**: `WriteSettingsStrategy.kt`, `ShizukuStrategy.kt` (+ tests).

**Out**: Changing FailureReason enum names unless needed; UI copy for KEY_NOT_WRITABLE (may already map to ControlFailed).

## Steps

1. After successful put/write, re-read state (`getCurrentState` or direct read).
2. Expected: enabled → Engaged (or non-zero int); disabled → Inactive (0).
3. Mismatch → `ControlResult.Failed(FailureReason.KEY_NOT_WRITABLE)`.
4. Unit/Robolectric test: mock/fake read returns old value after put → Failed.

**Verify**: `./gradlew :core:control-writesettings:test` (+ shizuku tests if added)

## Done criteria

- [ ] Both strategies verify readback before Success
- [ ] Tests cover mismatch path
- [ ] README 008 DONE

## STOP

- Read immediately after write races on some OEMs → report; consider short retry once (max 1) only with evidence.
