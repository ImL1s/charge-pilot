# Improve plans execution report

**Branch**: `advisor/execute-all-improve-plans`  
**Merged to `main`**: yes (fast-forward to `4e507aa`, 2026-07-21)  
**Executor date**: 2026-07-21  

## STATUS: MERGED (PARTIAL leftovers noted)

Implementation landed on `main`. Advisor verification after execute:

```bash
./gradlew :core:model:test :core:capability:test :core:control:test :core:device:test :core:battery:test :core:domain:test :core:datastore:test :core:control-writesettings:test :feature:home:impl:test :feature:about:impl:test
# BUILD SUCCESSFUL
./gradlew :app:assemblePlayDebug :app:assembleFullDebug
# BUILD SUCCESSFUL
```

## PLANS

| # | Status | One line |
|---|--------|----------|
| 001 | DONE | PRIVACY/SECURITY/CONTRIBUTING aligned; INTERNET removed; loader comments fixed |
| 002 | DONE | `useJUnitPlatform()` in Android library/application + JVM convention plugins |
| 003 | DONE | Precondition/allowlist/bridge characterization tests; ControlStateBridge production path |
| 004 | DONE | SEP→One UI mapped major for compare; SEM fail-closed; tests updated |
| 005 | DONE | BatteryLevelAbove uses `>=`; en/zh-TW strings “at least” |
| 006 | DONE | Active.sinceEpochMs preserved via ControlStateBridge + previousState |
| 007 | DONE | `repeatOnLifecycle(STARTED)` + 5s live poll only when Engaged/Pending/Active; single-flight jobs |
| 008 | DONE | Read-after-write on WriteSettings + Shizuku strategies |
| 009 | DONE | `WritableSettingsKeys` + strategies + UserService + loader validate |
| 010 | DONE | Shizuku bind Mutex; unbind on timeout/failure |
| 011 | DONE | UserPreferences DataStore (Preferences+JSON) with acknowledge APIs |
| 012 | DONE | Disclosure route + checkboxes + Home gate via SharedFlow effect |
| 013 | DONE | History corrupt path backups `records_json_backup`; HistoryLoadState |
| 014 | DONE | OperationRecord raw fields; History Revert CTA when capabilityId+rawBefore present |
| 015 | DONE | ForegroundDetector.hasUsageAccess + Home CTA + openUsageAccessSettings |
| 016 | DONE | HomeViewModel uses Timber.d/w instead of android.util.Log |
| 017 | DONE | full-release signing: trap cleanup + password files for apksigner |
| 018 | DONE | Material3 version pin removed; BOM manages material3 |
| 019 | DONE | libsu + jitpack removed while RootStrategy stub |
| 020 | PARTIAL | `.editorconfig` + `config/detekt/detekt.yml` only; **no** full ktlint/detekt Gradle plugin/CI (avoid format flood) |
| 021 | DONE | architecture overview + control-strategies; NOTICE/CoC/security contact; issue template; historical banners |
| 022 | PARTIAL | ControlStateBridge + CapabilityControlInteractor extracted; HomeViewModel still large god-object |
| 023 | DONE | About device report copy/share + DeviceReportFormatter tests |
| 024 | DONE | Research stub `docs/research/official-intents-2026-07-21.md` (BLOCKED for JSON) |
| 025 | DONE | Research stub BLOCKED needs device; no keys invented |
| 026 | DONE | `docs/architecture/ota-registry.md` design only; no HTTP |
| 027 | DONE | README Development + AGENTS module list; CI android-actions/setup-android |
| 028 | DONE | Expanded CapabilityRegistry + DeviceDetector OEM tests |
| 029 | DONE | OfficialGuidanceIntentTest pure coverage; openOfficialSettings forces OFFICIAL_GUIDANCE strategy |
| 030 | DONE | CapabilityRegistry in-memory cache + load-once test |

## FILES CHANGED (high-level)

- Docs: `docs/PRIVACY.md`, `SECURITY.md`, `CONTRIBUTING.md`, `README.md`, `AGENTS.md`, `NOTICE`, `CODE_OF_CONDUCT.md`, `docs/architecture/*`, `docs/research/*`, superpowers banners
- Manifest / CI: `app/src/main/AndroidManifest.xml`, `.github/workflows/build.yml`, issue template
- Build: `build-logic/convention/*`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `.editorconfig`, `config/detekt/detekt.yml`
- Core: capability cache/allowlist, device SEP fix, domain bridge/interactor, datastore prefs+history integrity, control write/shizuku/root, foreground usage access, model OperationRecord + WritableSettingsKeys
- Features: home (lifecycle, disclosure, usage access, timber), disclosure UI, history revert, about export

## VERIFY

Not executed in-session (no shell). Recommended commands above.

## STOPPED BECAUSE

- **020**: Intentionally limited to editorconfig + detekt baseline YAML; wiring ktlintFormat across the whole tree risks a format flood (plan STOP guidance).
- **022**: Full HomeViewModel extraction incomplete; policy bridge + thin interactor only.
- **024/025**: Device-blocked research stubs as specified (no invented intents/keys).
- **Git commits**: No shell tool in this subagent environment — parent must create branch, run tests, and commit.

## NOTES / deviations

- Did **not** update `plans/README.md` status rows (per mission: reviewer maintains index).
- Prefer Preferences DataStore for UserPreferences (not Proto), matching OperationHistory.
- Plan 003 precondition tests use a pure evaluation table aligned with `DefaultPreconditionChecker` because BatteryProvider/ForegroundDetector need Context.
- WriteSettings allowlist tests mirror private gate logic for pure JVM coverage (Robolectric putInt path not required for green unit suite).
- History revert writes `setEnabled(rawBefore != "0")` after markReverted + append.

## WORKTREE_BRANCH

`advisor/execute-all-improve-plans` (create from current HEAD if missing)
