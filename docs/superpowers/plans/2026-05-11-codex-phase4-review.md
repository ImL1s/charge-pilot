# Phase 4 Codex Review

## Security/Safety

**P1 - Generic Settings fallback is reported as success.**  
Description: `OfficialGuidanceStrategy` returns `ControlResult.Success` after opening either the OEM intent or generic `ACTION_SETTINGS`; the fallback can land users on the wrong screen and is also a success without a write.  
File path: `/Users/iml1s/Documents/mine/charge_pilot/core/control/src/main/kotlin/com/chargepilot/core/control/OfficialGuidanceStrategy.kt`  
Patch suggestion: replace generic fallback success with a distinct result such as `GuidanceOpened(intentSpecific: Boolean)` or `Failed(UNSUPPORTED_DEVICE)`, preflight with `resolveActivity`, and show `fallbackPath` as manual copy instead of treating top-level Settings as success.

**P1 - Registry loading accepts unsafe broad write rules.**  
Description: current bundled Samsung write rule is gated by `modelRegex` and `minRomVersion`, and the orchestrator/strategy chain will not pick `WRITE_SETTINGS_KEY` unless it is in `availableMethods`. However, loader validation is decode-only: `modelRegex` and `minRomVersion` are nullable, and the comment that descriptor id format is enforced is not true beyond required JSON presence. A future registry typo could widen a write-capable rule.  
File path: `/Users/iml1s/Documents/mine/charge_pilot/core/capability/src/main/kotlin/com/chargepilot/core/capability/CapabilityRegistryLoader.kt`  
Patch suggestion: validate after decode: unique `CapabilityDescriptor.id`, exact `${rule.id}::${descriptor.type}` format, known enum values, `settingsKey.namespace == "system"`, and require `modelRegex + minRomVersion + PROJECT_VERIFIED` whenever `WRITE_SETTINGS_KEY` appears.

**P2 - OTA registry is described but not implemented.**  
Description: manifest comments and loader docs describe OTA/cache behavior, but the loader only reads bundled assets and `core/data` has no registry source implementation. The current failure mode is safe/offline, but privacy/security docs overstate live behavior.  
File path: `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/AndroidManifest.xml`  
Patch suggestion: either implement signed/cached OTA behind a repository, or remove `INTERNET`/`ACCESS_NETWORK_STATE` and OTA wording until the feature exists.

## Architecture

**P1 - `Engaged` is never resolved by preconditions.**  
Description: `WriteSettingsStrategy` correctly returns `ControlState.Engaged`, but `HomeViewModel` never injects or runs `PreconditionChecker`, so Home can only display "configured, not verified" and never maps to `Active` or `PendingConditions`.  
File path: `/Users/iml1s/Documents/mine/charge_pilot/feature/home/impl/src/main/kotlin/com/chargepilot/feature/home/impl/HomeViewModel.kt`  
Patch suggestion: inject a checker implementation or nullable/stub provider; when state is `Engaged`, evaluate `descriptor.preconditions` and map to `Active(now)` or `PendingConditions(unmet)`, with unit tests.

## Compose/Android 16

No P0/P1 finding. `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/kotlin/com/chargepilot/app/MainActivity.kt` calls `enableEdgeToEdge()`, `/Users/iml1s/Documents/mine/charge_pilot/app/src/main/kotlin/com/chargepilot/app/navigation/ChargePilotNavHost.kt` has no custom back handling, and `/Users/iml1s/Documents/mine/charge_pilot/core/designsystem/src/main/kotlin/com/chargepilot/core/designsystem/theme/Theme.kt` deliberately uses plain `MaterialTheme`.

## OSS Hygiene

**P2 - NOTICE references a missing license-report task.**  
Description: `NOTICE` says the full third-party list is available via `./gradlew :app:licenseReport`, but no license-report plugin or task is declared in the reviewed Gradle files.  
File path: `/Users/iml1s/Documents/mine/charge_pilot/NOTICE`  
Patch suggestion: add a license-report plugin and CI check, or change `NOTICE` to reference a generated artifact that actually exists.

**P2 - CI assemble task interpolation is likely wrong.**  
Description: the workflow runs `:app:assemble${{ matrix.flavor }}Debug` with lowercase `play/full`; existing APK outputs are under `app/build/outputs/apk/play/debug/` and `full/debug/`, but AGP task names are conventionally `assemblePlayDebug` and `assembleFullDebug`.  
File path: `/Users/iml1s/Documents/mine/charge_pilot/.github/workflows/build.yml`  
Patch suggestion: use matrix entries with explicit `assembleTask: assemblePlayDebug/assembleFullDebug`; keep the current artifact paths.

## Summary

No P0 issue found. The main risks are semantic success without real control, decode-only registry trust around write-capable rules, and an incomplete `Engaged` precondition pipeline. Play-flavor strategy absence is handled by nullable map lookup, and current system-property failures degrade to no ROM match rather than wider capability matching.
