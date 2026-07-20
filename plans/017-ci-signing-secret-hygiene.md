# Plan 017: Harden CI release signing secret handling

> **Drift check**: `git diff --stat 1f293cf..HEAD -- .github/workflows/build.yml`

## Status

- **Priority**: P2 · **Effort**: S · **Risk**: LOW · **Depends on**: none · **Category**: security  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

`full-release` job decodes keystore to disk and passes passwords on `apksigner` argv (`--ks-pass pass:…`). Failed apksigner can exit before `rm -f release-keystore.jks` under `set -e`. Passwords appear in process listings on the runner.

## Current state

`.github/workflows/build.yml` Stage Full release APK step (~125–149): base64 decode → apksigner sign with pass on CLI → rm keystore only on success path.

## Scope

**In**: `.github/workflows/build.yml` full-release signing shell only.

**Out**: Changing secret names; Android app signingConfigs; rotating secrets (recommend in PR notes if exposure suspected — **never print secret values**).

## Steps

1. Wrap keystore handling:
   ```bash
   cleanup() { rm -f release-keystore.jks ks-pass.txt key-pass.txt; }
   trap cleanup EXIT
   ```
2. Prefer password files with restrictive umask, e.g. write passes to temp files and use `apksigner` `--ks-pass file:…` / `--key-pass file:…` if supported by build-tools on runner.
3. If file pass unsupported, still use trap + avoid echoing secrets; document limitation.
4. Do not log passwords.

**Verify**: workflow YAML validates (`actionlint` if available); dry-read the script; optional `workflow_dispatch` on a fork.

## Done criteria

- [ ] `trap` ensures keystore deletion
- [ ] Passwords not left in script debug prints
- [ ] Prefer file-based pass over argv when available
- [ ] README 017 DONE

## STOP

- apksigner on runner lacks file: pass → keep trap + document; do not invent custom crypto.
