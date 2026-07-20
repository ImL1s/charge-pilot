# Plan 001: Align privacy/security docs with bundled-only registry

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report — do not improvise. When done, update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 1f293cf..HEAD -- docs/PRIVACY.md SECURITY.md CONTRIBUTING.md app/src/main/AndroidManifest.xml core/capability/src/main/kotlin/com/chargepilot/core/capability/CapabilityRegistryLoader.kt`
> If any in-scope file changed since this plan was written, compare the "Current state" excerpts against the live code before proceeding; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: security | docs
- **Planned at**: commit `1f293cf`, 2026-07-21

## Why this matters

`docs/PRIVACY.md`, `SECURITY.md`, and `CONTRIBUTING.md` claim Charge Pilot performs a non-blocking HTTPS OTA fetch of `capabilities-v1.json` on startup. The code only loads the bundled asset. The app still declares `INTERNET` / `ACCESS_NETWORK_STATE` for that fictional fetch. This is actively wrong documentation (worse than missing docs): users, auditors, and Play reviewers will trust a network story that does not exist. Fix truth first; OTA is a later spike (plan 026) and must not ship while docs lie.

## Current state

- `core/capability/.../CapabilityRegistryLoader.kt` — only reads assets:
  - Comment says “cached OTA → bundled” but implementation opens `capabilities-v1.json` from assets only (lines 12–36).
- `app/src/main/AndroidManifest.xml:10-12`:
  ```xml
  <!-- Network only used for non-blocking capability registry OTA fetch. -->
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  ```
- `docs/PRIVACY.md:13-24` — describes “Exactly one HTTPS request” to GitHub raw.
- `SECURITY.md:15` — threat model item “OTA registry tampering” as if OTA is live.
- `CONTRIBUTING.md` — claims only network access is OTA fetch.
- Repo-wide search shows no OkHttp/URLConnection fetch of the registry.
- Design non-goal: no telemetry. Keeping unused INTERNET is pure surface.

**Convention**: Documentation must match code. Prefer removing unused permissions until a feature needs them.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Grep network fetch | `rg -n 'HttpURLConnection|OkHttp|raw.githubusercontent|capabilities-v1' --glob '!**/build/**' --glob '!**/plans/**'` | No live HTTP client for registry (only docs/plans/assets path) |
| Assemble both flavors | `./gradlew :app:assemblePlayDebug :app:assembleFullDebug` | BUILD SUCCESSFUL |
| Unit tests | `./gradlew test` | BUILD SUCCESSFUL |

## Scope

**In scope**:
- `docs/PRIVACY.md`
- `SECURITY.md`
- `CONTRIBUTING.md` (network / OTA sentences only)
- `app/src/main/AndroidManifest.xml` (remove INTERNET + ACCESS_NETWORK_STATE + OTA comment **unless** another feature needs them — none does today)
- `core/capability/.../CapabilityRegistryLoader.kt` and `CapabilityRegistry.kt` (comment-only: remove false OTA claims)
- Optionally `README.md` one-line note if it implies live network updates (check; only edit if false claim present)

**Out of scope**:
- Implementing OTA (plan 026 only)
- Signing, caching, or remote JSON validation
- Changing capability JSON content

## Git workflow

- Branch: `advisor/001-align-privacy-docs-bundled-registry`
- Commit style: `docs(privacy): state bundled-only registry and drop unused INTERNET`
- Do NOT push unless asked.

## Steps

### Step 1: Confirm no registry network code

Run the grep in Commands. Confirm no production fetch implementation.

**Verify**: No Kotlin production source opens a network URL for the registry.

### Step 2: Rewrite PRIVACY.md network section

Replace “What the app fetches from the network” with explicit:

- Charge Pilot does **not** perform network fetches of the capability registry in the current release.
- Registry data ships **bundled** in the APK (`assets/capabilities-v1.json`).
- Device profile and operation history never leave the device.
- Remove or rewrite the INTERNET permission bullet to say those permissions are **not** used (or omit them after removal).

**Verify**: `rg -n 'HTTPS|OTA|GitHub raw' docs/PRIVACY.md` — no claim of live fetch remaining (future-tense “may add later” is OK only if clearly not current behavior).

### Step 3: Rewrite SECURITY.md threat model item 3

Change OTA item to: OTA is **not implemented**; remote registry is not a live threat surface; if OTA is added later it will require integrity (signature) — see plan 026. Keep device-safety and Play policy items. Soften “pass a disclosure step” only if still false after plan 012; until then either note “disclosure UI planned” or leave if you also land 012 in same branch (prefer leave accurate: if disclosure still missing, say “user-initiated Home action; multi-step disclosure tracked separately”).

**Verify**: `SECURITY.md` does not claim live OTA over HTTPS as current behavior.

### Step 4: Fix CONTRIBUTING.md

One-sentence network claim → bundled-only / no network required for normal use.

### Step 5: Remove unused network permissions from manifest

Delete INTERNET and ACCESS_NETWORK_STATE and the OTA comment from `app/src/main/AndroidManifest.xml`.

**Verify**:
```bash
rg -n 'INTERNET|ACCESS_NETWORK_STATE' app/src/main/AndroidManifest.xml
```
→ no matches.

### Step 6: Fix misleading loader/registry comments

In `CapabilityRegistryLoader.kt` and `CapabilityRegistry.kt`, replace OTA comments with “bundled asset only; OTA not implemented.”

**Verify**: `./gradlew :app:assemblePlayDebug :app:assembleFullDebug` → SUCCESS.

## Test plan

- No new unit tests required (docs + permissions).
- Manual: open PRIVACY/SECURITY side-by-side with `BundledCapabilityRegistryLoader` — statements match.

## Done criteria

- [ ] PRIVACY/SECURITY/CONTRIBUTING do not claim a live registry HTTPS fetch as current behavior
- [ ] `app` manifest has no INTERNET / ACCESS_NETWORK_STATE
- [ ] Loader comments do not claim OTA cache path is implemented
- [ ] `./gradlew :app:assemblePlayDebug :app:assembleFullDebug` exits 0
- [ ] No files outside scope modified
- [ ] `plans/README.md` row 001 → DONE

## STOP conditions

- You find an actual network registry fetch in production code → stop and report (plan scope changes).
- Play Console or F-Droid listing text elsewhere still claims OTA and is in-repo → include those files or report them.
- Removing INTERNET breaks a real dependency you discover → stop and report the dependency.

## Maintenance notes

- When OTA ships (plan 026+), re-add permissions **and** rewrite PRIVACY/SECURITY in the same PR as the fetch code.
- Reviewers: refuse any PR that re-adds INTERNET without a call site.
