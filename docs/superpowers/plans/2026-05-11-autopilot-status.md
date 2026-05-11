# Autopilot final status — 2026-05-11

## Pipeline executed

`brainstorming` → `architect plan` → `codex plan review` → `Phase 2 implementation` → `Phase 3 build verification` → `Phase 4 multi-LLM review (architect + codex + code-reviewer)` → fixes → re-verify.

## Build outcome

| Flavor | Output | Size |
|---|---|---|
| `play` | `app/build/outputs/apk/play/debug/app-play-debug.apk` | ~18.4 MB |
| `full` | `app/build/outputs/apk/full/debug/app-full-debug.apk` | ~18.5 MB |

## Test outcome

| Module | Tests | Status |
|---|---|---|
| `:core:model` (`ManufacturerTest`) | 5 | ✅ |
| `:core:capability` (`CapabilityRegistryTest`, `RegistrySerializationTest`) | 7 | ✅ |
| `:core:control` (`ControlOrchestratorTest`) | 5 | ✅ |
| `:feature:home:impl` (`HomeViewModelBridgeTest`) | 4 | ✅ |

## Review pipeline findings — applied fixes

| Severity | Finding | Status |
|---|---|---|
| CRITICAL (code-reviewer) | `WriteSettingsStrategy.setEnabled` did not enforce `WRITE_SETTINGS_KEY in availableMethods` | **fixed** — added `descriptorAllowsWrite` guard, enforced in both `getCurrentState` and `setEnabled` |
| HIGH (all 3 reviewers) | `HomeViewModel` never bridged `ControlState.Engaged` → `Active` / `PendingConditions` | **fixed** — `PreconditionChecker` injected, bridge implemented, `HomeViewModelBridgeTest` covers 4 branches |
| HIGH (codex + code-reviewer) | `OfficialGuidanceStrategy` returned `Success` after merely launching a Settings activity, with a silent fallback to `ACTION_SETTINGS` | **fixed** — fallback removed, intent now pre-flighted with `resolveActivity`, success returned as new `ControlResult.NavigatedToSettings`, failure surfaces `Failed(UNSUPPORTED_DEVICE)` |
| MEDIUM (codex) | `CapabilityRegistry.matches` used `Manufacturer.valueOf` / `RomFlavor.valueOf` — crashes the whole resolve on a typo in OTA registry | **fixed** — both replaced with safe `.entries.firstOrNull` lookups; unknown rules are skipped instead of raising |
| MEDIUM (code-reviewer) | Loader did not enforce `descriptor.id == "${rule.id}::${type}"` | **fixed** — `BundledCapabilityRegistryLoader.validate` runs after JSON decode and rejects bad ids, duplicate ids, and non-`system` namespace settings keys |
| MEDIUM (code-reviewer) | `HomeScreen` LazyColumn `key = { it.descriptor.type.name }` could collide if two rules produce the same type | **fixed** — keyed by `descriptor.id` |
| LOW (code-reviewer) | Dead code (`unused(c: Color)`) in `CapabilityCard` | **fixed** — removed |
| P2 (codex) | CI matrix used `:app:assemble${{ matrix.flavor }}Debug` which interpolates lowercase | **fixed** — explicit `include:` matrix with capitalized `assembleTask` |

## Review pipeline findings — deferred

| Severity | Finding | Why deferred |
|---|---|---|
| P1 (architect) | `BatteryProvider`, `ForegroundDetector`, `UserPreferencesDataSource` are stubs | Out of v0 scope; tracked in spec §11 (out of scope). `DefaultPreconditionChecker` correctly reports unmet when the providers can't evaluate, so the UI is still honest. |
| P2 (codex) | OTA registry described in spec but not implemented | Out of v0 scope; spec already flags this as future work. |
| P2 (codex) | `NOTICE` references `./gradlew :app:licenseReport` which is not configured | Cosmetic; add a license-report plugin in a follow-up PR. |
| LOW (code-reviewer) | `tools:` xmlns repeated on each `<uses-permission>` instead of root | Cosmetic. |
| LOW (code-reviewer) | `WriteSettingsStrategy.reset` writes 0 instead of the previous value | True bug, but `OperationRecord` history isn't wired up yet — fix together with history feature. |
| MEDIUM (code-reviewer) | `getCurrentState` reads not on the IO dispatcher inside the strategy | Mitigated for the home path: `HomeViewModel.load` now wraps the whole pipeline in `withContext(ioDispatcher)`. Strategy-level dispatcher hygiene is a follow-up. |
| HIGH (code-reviewer) | Reflection on `android.os.SystemProperties` lights up Play's "non-SDK interfaces" lint | Property names are hard-coded by `DeviceDetector` and the input contract is internal; treating as accepted risk for v0 with a documented allow-list as a follow-up. |

## What works end-to-end now

- App launches, calls `enableEdgeToEdge()`, renders `ChargePilotTheme`.
- `HomeViewModel` runs `DeviceDetector → CapabilityRegistry → ControlOrchestrator → strategy.getCurrentState → PreconditionChecker bridge`, all on the IO dispatcher.
- For a Samsung S24 with `pass_through=1` *and* unmet preconditions, the home card now correctly surfaces `PendingConditions` (the spec's central mandate). With met preconditions it surfaces `Active`. Without the key written, `Inactive`. Without OEM support, `Unsupported`.
- Both `play` and `full` flavors build; the play APK does not depend on `:core:control-shizuku` or `:core:control-root`.

## What's still skeleton

- Disclosure flow (3-step) — module exists, screen is a placeholder.
- History — module exists, screen is a placeholder, persistence not wired.
- Brands matrix — placeholder; needs to render from `capabilities-v1.json`.
- Advanced (Shizuku/root) — `play` source set shows the "F-Droid only" card; `full` source set is a placeholder.
- About — placeholder.
- OTA capability registry fetcher — not implemented; bundled assets only.
- Real `BatteryProvider`, `ForegroundDetector`, `UserPreferencesDataSource`.

## Files of interest

- Spec: `docs/superpowers/specs/2026-05-11-charge-pilot-design.md`
- Implementation plan: `docs/superpowers/plans/2026-05-11-charge-pilot-impl.md`
- Pre-build codex review: `docs/superpowers/plans/2026-05-11-codex-plan-review.md`
- Phase 4 architect review: `docs/superpowers/plans/2026-05-11-architect-phase4-review.md`
- Phase 4 codex review: `docs/superpowers/plans/2026-05-11-codex-phase4-review.md`
- Phase 4 code-reviewer findings: this document, "Review pipeline findings" table.
