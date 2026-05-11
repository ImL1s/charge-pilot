# Charge Pilot Implementation Plan — Codex Second-Opinion Review

Inputs reviewed completely:
- Spec: `docs/superpowers/specs/2026-05-11-charge-pilot-design.md`
- Plan: `docs/superpowers/plans/2026-05-11-charge-pilot-impl.md`

Review posture: adversarial architectural review against the two explicit threat models: Play Store policy compliance and accidental device damage from unsafe system-settings writes.

## 1. BLIND-SPOT SCAN

### P1 — Registry descriptors have no stable capability ID, but navigation and disclosure require one.

One-sentence finding: The plan routes disclosure by `capabilityId`, but `CapabilityRegistry.resolve()` returns `CapabilityDescriptor` objects that do not contain an ID, so Home cannot produce a stable disclosure route without an unstated side channel.

Reference: Spec `CapabilityDescriptor` omits an ID at lines 196-205 while the registry rule ID is outside `capabilities` at lines 248-272; Plan Phase 1c returns `List<CapabilityDescriptor>` at lines 192-195; Plan Phase 4b defines `DisclosureNavKey(val capabilityId: String)` at lines 405-408.

Concrete fix: Patch Phase 1b/1c to add a stable `id: String` to `CapabilityDescriptor` or introduce `ResolvedCapability(ruleId: String, capabilityId: String, descriptor: CapabilityDescriptor)` and make `CapabilityRegistry.resolve()` return `List<ResolvedCapability>`; patch Phase 4a/4b so `HomeUiState.cards` and `DisclosureNavKey` use that exact ID and tests assert round-trip lookup from Home click to DisclosureViewModel.

### P1 — The control contract cannot distinguish "settings key written" from "capability actually active."

One-sentence finding: `ControlResult.Success` only proves that a strategy operation returned success, but the spec requires `Active` only when both the write and runtime preconditions are true.

Reference: Spec requires separate pending and active states at lines 84-92 and states the risk mitigation explicitly at lines 349-353; Plan Phase 2d keeps `ControlResult.Success` unqualified at lines 263-275; Plan Phase 2e only says `WriteSettingsStrategy` wraps `putInt/getInt` and captures before-value at lines 307-314; Plan Phase 3b renders `Active(since)` from `CapabilityCardState` at lines 360-368.

Concrete fix: Replace `ControlResult.Success` with a richer result such as `ControlExecutionResult.WriteAcknowledged(snapshot: ControlSnapshot)` where `ControlSnapshot` includes `keyValueState`, `unmetPreconditions`, and `effectiveControlState`; require `ControlOrchestrator.execute()` to re-run `PreconditionEvaluator` after the write and return `PendingConditions` unless PD charger, battery threshold, and foreground-game checks are satisfied.

### P1 — HomeViewModel duplicates control-state logic instead of consuming the orchestrator's state contract.

One-sentence finding: The plan has `HomeViewModel` collect battery and foreground flows directly to compute card states, bypassing `ControlStrategy.getCurrentState()` and risking UI state drift from the strategy implementation.

Reference: Spec defines `ControlStrategy.getCurrentState()` at lines 208-217; Plan Phase 2d defines `PreconditionEvaluator` and `ControlOrchestrator` at lines 277-290; Plan Phase 4a makes `HomeViewModel` collect `DeviceProfileSource`, `CapabilityRegistry`, `BatteryStateSource`, and `ForegroundAppSource` directly at lines 394-400.

Concrete fix: Patch Phase 2d to expose `observeControlStates(profile): Flow<Map<ResolvedCapabilityId, ControlSnapshot>>` from `ControlOrchestrator`, and patch Phase 4a so `HomeViewModel` maps orchestrator snapshots to `CapabilityCardState` instead of recomputing preconditions independently.

### P1 — The WRITE_SETTINGS tests would mostly verify Robolectric behavior, not OEM charging behavior.

One-sentence finding: The plan's `WriteSettingsStrategyTest` can prove only framework wrapper behavior, while the first vertical slice claims Samsung `pass_through` behavior that requires real Samsung Game Booster, charger, battery, and foreground-game conditions.

Reference: Spec identifies Samsung preconditions at lines 264-268 and line 284; Plan Phase 2e uses a Robolectric read/write/revert test at line 314; Plan Phase 5 smoke test incorrectly names a "Galaxy S24 emulator" at lines 501-505.

Concrete fix: Keep the Robolectric wrapper test, but patch Phase 2e/5 verification to add a real-device acceptance test on Galaxy S24/S25: capture `settings get system pass_through` before/after, verify disclosure and history, then record UI state as `PendingConditions` unless the device is on PPS PD >=25 W, battery >20%, and a known game is foregrounded.

### P1 — Shizuku/root tests are explicitly fake but the plan still calls the scaffolds "binder check works" and "libsu probe works."

One-sentence finding: Phase 2 claims Shizuku binder and root probes work, but the planned tests fake the clients and never define an executable integration probe.

Reference: Plan phase table promises "binder check works" and "libsu probe works" at lines 74-76; Phase 2f/2g tests use fake `ShizukuClient` and fake `Shell` at lines 316-332; Phase 2 verification only runs unit tests and assemble at lines 334-337.

Concrete fix: Patch Phase 2f/2g verification to downgrade the unit-test claim to "contract tests only" and add quarantined manual/instrumented probes for `fullDebug`: `Shizuku.pingBinder()` on a Shizuku-enabled device and `Shell.getShell().isRoot` on a rooted test device, with skipped status documented when the devices are unavailable.

### P1 — `:core:data` is assigned ownership for OTA registry fetch but never implemented.

One-sentence finding: The architecture says `:core:data` owns OTA fetch and cache, but the phase plan never creates the module, its dependencies, or its tests.

Reference: Spec module layout includes `core/data` at line 130; Plan architecture assigns OTA fetch and cache to `:core:data` at lines 33-37; Phase 1 implements asset loading only at lines 187-198; Phase 2c DataStore has preferences/history only at lines 236-248.

Concrete fix: Add a Phase 1d or 2h `:core:data` task that owns `RegistryRepository`, HTTP fetch, cache read/write, integrity verification, and fallback-to-bundled behavior; add tests for bundled asset fallback, cached registry use, failed network, invalid schema, and invalid signature.

### P1 — OTA registry safety is underspecified for a data file that can enable system writes.

One-sentence finding: The plan includes `schemaVersion` but lacks compatibility rules, rollback rules, integrity checks, and signature verification for an OTA registry that controls writeable settings keys.

Reference: Spec says startup fetches a GitHub raw JSON and applies it next launch at lines 242-276; Spec risk table defers signing to optional v2 at lines 347-353; Plan Phase 1c defines only `RegistryFile(val schemaVersion: Int, val rules: List<Rule>)` at lines 187-198; Plan decision 3 hardcodes an OTA URL pattern at lines 557-560.

Concrete fix: Patch Phase 1c/2h to define `RegistryFile(schemaVersion, registryVersion, minAppVersion, generatedAt, sourceRevision, rules, signature)`; pin an offline public key in the app; verify canonical JSON signature and SHA-256 before caching; reject unsupported schema versions and unsigned downgrades; apply updates atomically on next launch only after validation.

### P1 — Flavor-only control modules have conflicting wiring ownership.

One-sentence finding: The plan simultaneously says Shizuku/root bindings live in `app/src/full`, self-register in the core modules, and are pulled by `feature:advanced:impl`, which leaves Hilt aggregation and dependency ownership ambiguous.

Reference: Plan dependency rules put `:core:control-shizuku` and `:core:control-root` into `ControlOrchestrator` only via `app/src/full` at lines 33-36; Phase 2f/2g create Hilt modules inside the core modules at lines 316-332; Phase 4f adds them to `feature:advanced:impl` `fullImplementation` at lines 448-461; Phase 5a only wires all feature impls plus `:core:control-writesettings` at lines 472-475; Phase 5b says app full re-exports the self-registering bindings at lines 486-493.

Concrete fix: Make `:app` the flavor boundary: patch Phase 5a to add `fullImplementation(project(":core:control-shizuku"))` and `fullImplementation(project(":core:control-root"))`, keep `playImplementation` free of those deps, and remove the "re-export" wording; if `feature:advanced:impl` needs status APIs, depend on a small flavor-safe interface in `:core:control` and bind full/play implementations in app source sets.

### P1 — Predictive back is named but not acceptance-tested.

One-sentence finding: The plan says `DisclosureSheet` uses `BackHandler`, but does not define expected back behavior per step or prove predictive-back cancellation keeps disclosure state intact.

Reference: Spec requires predictive back interception for disclosure at lines 109-113; Plan Phase 3b creates `DisclosureSheet` with `BackHandler` at line 364; Plan Phase 3b test only verifies first/subsequent disclosure step visibility at lines 367-368.

Concrete fix: Patch Phase 3b/4b tests to include a disclosure back contract: step 2 back returns to step 1, confirm-step back opens an explicit cancel confirmation or returns to step 2, and no back gesture records disclosure consent or executes `ControlOrchestrator.execute()`; use Activity/Compose integration tests rather than a pure canned-state composable test for this path.

### P1 — Material 3 Expressive experimental opt-in is not wired into the compile plan.

One-sentence finding: The plan checks whether M3E symbols exist but does not add `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` or module compiler opt-in where `MaterialExpressiveTheme` and `MotionScheme.expressive()` are used.

Reference: Spec selects Material 3 Expressive at lines 37-40 and line 82; Plan Phase 3a uses `MotionScheme.expressive()` and `MaterialExpressiveTheme` at lines 353-354; Plan decision 4 only checks availability at lines 557-560.

Concrete fix: Patch Phase 0a decision 0a.1 to include the exact Material3 artifact and experimental API status, and patch Phase 3a to annotate `Motion.kt` and `ChargePilotTheme.kt` with `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` or add `freeCompilerArgs += "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi"` in `chargepilot.android.compose`.

### P0 — Phase 0 lacks a toolchain quarantine for missing Gradle, Android SDK, and SDK 36.

One-sentence finding: Phase 0 depends on local `gradle`, Java, Android SDK, and compile SDK 36, but the plan has no preflight or fallback path when those tools are missing.

Reference: Plan Phase 0b requires running `gradle wrapper --gradle-version <X>` at lines 97-101; Phase 0d sets `compileSdk=36` at lines 123-133; Phase 0 verification immediately runs `./gradlew` at lines 135-140.

Concrete fix: Add Phase 0a.5 "Toolchain quarantine": run `command -v java gradle sdkmanager`, `java -version`, and `sdkmanager --list_installed | grep 'platforms;android-36'`; if any are missing, stop Phase 0b/0d and add explicit install tasks for JDK 17, Gradle wrapper generation, Android SDK platform 36, build-tools, and `ANDROID_HOME`, with verification rerun before creating modules.

### P1 — The "OfficialGuidanceStrategy" starts UI from a core control module.

One-sentence finding: A core strategy that launches Settings intents from `setEnabled()` mixes UI navigation and domain control, making lifecycle, ActivityResult, and testing behavior brittle.

Reference: Spec architecture separates core modules from feature UI at lines 118-150; Plan Phase 2d says `OfficialGuidanceStrategy.setEnabled` returns success after firing an intent at lines 292-294; Plan Phase 4b calls `ControlOrchestrator.execute(...)` from the disclosure screen at lines 403-414.

Concrete fix: Patch `ControlStrategy` so `OFFICIAL_GUIDANCE` returns `ControlResult.RequiresUserAction(OpenIntentRequest(intentSpec))` instead of launching; let `DisclosureScreen` or an app-level navigator launch the intent with ActivityResult/launcher APIs and record history only when the app regains focus and the state is re-read.

## 2. SEQUENCING PROBLEMS

### P1 — Phase 4a depends on `:feature:disclosure:api` before Phase 4b creates it.

One-sentence finding: Home is scheduled first, but its build file depends on disclosure API that is created in the next sub-phase.

Reference: Plan Phase 4 priority says `home` first and `disclosure` second at lines 385-389; Phase 4a adds `:feature:disclosure:api` to Home impl at line 394; Phase 4b creates `:feature:disclosure:api` at lines 403-408.

Concrete fix: Insert a Phase 4-pre "feature API skeletons" task that creates all six `feature/*/api` modules before any impl module, or reorder Phase 4b API creation before Phase 4a Home impl.

### P1 — Phase 4f says it depends on Phase 5, while Phase 5 says it depends on Phase 0-4.

One-sentence finding: `:feature:advanced` is declared to depend on flavor plumbing in Phase 5, but Phase 5 app wiring waits for Phase 4 to finish.

Reference: Plan Phase 4f says it depends on flavors plumbing in Phase 5 at line 448; Plan Phase 5a says app skeleton depends on Phase 0-4 at line 472.

Concrete fix: Move reusable flavor plumbing entirely into Phase 0d's `AndroidFlavorsConventionPlugin`, remove the Phase 5 dependency from Phase 4f, and make Phase 5 depend on a compiled `:feature:advanced:impl` instead of being a prerequisite for it.

### P1 — `:core:control` is marked as depending on 2a + 2c, but it imports 2b modules.

One-sentence finding: Phase 2d cannot start after only device and datastore because its build file and `PreconditionEvaluator` use battery and foreground modules.

Reference: Plan Phase 2d header says "depends on 2a + 2c" at line 250; its build file depends on `:core:battery` and `:core:foreground` at line 252; `PreconditionEvaluator` injects battery and foreground at line 277.

Concrete fix: Patch the Phase 2d header and parallel batch notes so 2d depends on 2a + 2b + 2c, and require `:core:battery:test` and `:core:foreground:test` green before starting control.

### P1 — `:core:domain` is referenced by the feature convention plugin but never created.

One-sentence finding: The feature convention plugin adds a dependency on `:core:domain`, but no phase creates a domain module build file or API.

Reference: Spec module layout lists `core/domain` at line 129; Plan module list includes `domain` at line 30; Phase 0d adds `:core:domain` to every feature at line 129; Phases 1-6 never define a `:core:domain` creation task.

Concrete fix: Either add Phase 1d `:core:domain` with explicit use cases and tests before feature modules, or remove `:core:domain` from `AndroidFeatureConventionPlugin` until a concrete domain API exists.

### P1 — The Proto DataStore schema as written would not compile.

One-sentence finding: The plan describes proto fields that are invalid or refer to Kotlin data classes instead of proto messages.

Reference: Plan Phase 2c defines `bool seen_disclosure_for_<capability>` at line 240 and `repeated OperationRecord records;` at line 241, while `OperationRecord` is a Kotlin `@Serializable data class` in Phase 1b at lines 184-185.

Concrete fix: Replace the preference field with `map<string, bool> seen_disclosure_by_capability = N;` and define a real `message OperationRecordProto` in `operation_history.proto`; add mapper functions between proto and `core:model` `OperationRecord`.

### P1 — The app manifest includes an invalid edge-to-edge attribute.

One-sentence finding: `android:enableEdgeToEdge="true"` is planned as a manifest attribute even though the spec's own Android 16 guidance is to call `enableEdgeToEdge()` in the activity.

Reference: Spec Android 16 implications require `enableEdgeToEdge()` calls at lines 52-55 and UX repeats it at lines 109-112; Plan Phase 5a adds `android:enableEdgeToEdge="true"` to `<application>` at line 476 and also calls `enableEdgeToEdge()` in `MainActivity` at line 478.

Concrete fix: Remove `android:enableEdgeToEdge="true"` from the manifest task and keep edge-to-edge exclusively in `MainActivity.enableEdgeToEdge()` plus WindowInsets handling in composables.

## 3. SCOPE HONESTY

### P1 — The "Phase 0-3 APK" claim is false.

One-sentence finding: The scope hint says Phase 0-3 deliver a play-flavor APK, but the app module, navigation, features, and APK assembly are not planned until Phases 4-5.

Reference: Plan scope hint says Phase 0-3 deliver a play-flavor APK at line 7; Phase table puts features in Phase 4 and app wiring in Phase 5 at lines 77-80; APK assembly occurs only in Phase 5 verification at lines 501-508.

Concrete fix: Patch the scope hint to say "Phase 0-5 deliver the first play-flavor APK" or move a minimal app/home/disclosure shell into Phase 3 and reserve Phase 4 for non-slice feature breadth.

### P1 — The fastest vertical slice omits History even though the goal requires one-tap revert.

One-sentence finding: The plan's first executable slice requires operation history with one-tap revert, but the fastest path excludes the history feature.

Reference: Plan goal includes "Persist operation history in Proto DataStore with a one-tap revert" at lines 13-19; fastest path includes "minimal 4 (home + disclosure)" at lines 81-82; Phase 4 priority defers history after about at lines 385-435; vertical-slice completion is declared after Phase 5 at lines 501-508.

Concrete fix: Include `:feature:history` in the minimal Phase 4 slice before Phase 5 verification, or explicitly downgrade the first slice goal to "history repository records writes; History UI/revert comes later" and stop calling it complete until that later task passes.

### P1 — The manual smoke target cannot verify the core Samsung capability as written.

One-sentence finding: A "Galaxy S24 emulator" cannot validate Samsung Game Booster, PPS PD charger state, or the real `pass_through` behavior required by the slice.

Reference: Spec requires real Samsung-specific preconditions at lines 264-268 and line 284; Plan Phase 5 manual smoke uses "Pixel + Galaxy S24 emulator" at line 505; Spec testing calls for manual APK installs on Galaxy S24/S25 and Pixel 8/9 at lines 322-328.

Concrete fix: Patch Phase 5 verification to require a physical Galaxy S24/S25 for Samsung write-state acceptance and a Pixel 8/9 for no-bypass UX; keep emulators only for generic render/navigation smoke.

### P1 — First-run and special-permission UX are deferred too late for a Play-compliant vertical slice.

One-sentence finding: `WRITE_SETTINGS` and `PACKAGE_USAGE_STATS` are declared in the app, but the permission education path is not part of the minimal home/disclosure slice.

Reference: Spec requires special-permission disclosure and no deceptive settings behavior at lines 93-108 and lines 315-318; Plan Phase 5 manifest declares `WRITE_SETTINGS` and `PACKAGE_USAGE_STATS` at line 476; Plan decision 7 says usage-stats UX is still undecided at lines 562-563; Phase 5c first-run primer is parallel polish at lines 495-500.

Concrete fix: Move special-access education into the Phase 4b disclosure acceptance criteria: `WRITE_SETTINGS` permission launcher, usage-stats launcher, denial state, and "why this permission is needed" copy must be present before Phase 5 marks the play APK smoke-testable.

## 4. OPEN-SOURCE PROJECT HYGIENE

### P1 — The AGPL project lacks explicit LICENSE/COPYING tasks.

One-sentence finding: The plan says the project is AGPL-3.0, but Phase 6 does not create a root license file.

Reference: Spec declares AGPL-3.0-or-later at lines 334-341; Plan goal repeats AGPL-3.0 at lines 11-20; Plan Phase 6a creates CONTRIBUTING, SECURITY, CODE_OF_CONDUCT, PRIVACY, compatibility matrix, and architecture docs at lines 514-523, but no `LICENSE` or `COPYING`.

Concrete fix: Add Phase 6a tasks to create root `LICENSE` containing AGPL-3.0-or-later text, `COPYING` if desired for AGPL convention, and a docs-license note for CC BY 4.0 documentation.

### P1 — NOTICE and third-party license reporting are mentioned but not implemented.

One-sentence finding: Font licensing is listed as a decision, but there is no task to generate or maintain NOTICE/third-party license artifacts for dependencies and bundled fonts.

Reference: Spec lists Fira fonts at lines 76-82 and license expectations at lines 334-341; Plan decision 6 says Fira fonts need a `NOTICE` entry at line 562; Plan Phase 3a downloads font files at lines 347-348; Phase 6a has no NOTICE or third-party license task at lines 514-523.

Concrete fix: Add Phase 6a `NOTICE`, `THIRD_PARTY_NOTICES.md`, and a Gradle dependency-license report task; make Phase 3a font tasks include upstream URL, version/commit, license file copy, and checksum.

### P1 — F-Droid/GitHub release hygiene is underplanned for the `full` flavor.

One-sentence finding: The product definition promises F-Droid/GitHub distribution, but the plan lacks F-Droid metadata, reproducible-build settings, and channel-specific package/signing decisions.

Reference: Spec ships `full` through F-Droid/GitHub at lines 15-17 and describes flavor behavior at lines 309-313; Plan Phase 6 release workflow only attaches AABs/APKs to GitHub Releases at lines 532-537; Plan decision 8 only asks about GitHub Actions keystore secrets at line 564.

Concrete fix: Add Phase 6 tasks for `metadata/io.chargepilot.app.full.yml` or the chosen package ID, F-Droid build flavor definition, dependency locking, Gradle wrapper validation, reproducible build notes, and explicit release-channel signing policy.

### P1 — Shared application ID across Play and full flavors will create signing and install-channel conflicts unless intentionally designed.

One-sentence finding: The plan assumes no flavor suffix while also naming a distinct "Full" app, which can make Play and F-Droid/GitHub builds mutually non-installable under different signing keys.

Reference: Spec states `play` has `applicationIdSuffix=""` at line 310 and `full` adds Shizuku/root at line 311; Plan Phase 5b changes only app name to "Charge Pilot (Full)" at lines 488-493; Plan decision 2 says no flavor suffix per spec at lines 557-558.

Concrete fix: Patch Phase 0a.3 and `AndroidFlavorsConventionPlugin` to choose one policy explicitly: either `full.applicationIdSuffix = ".full"` for parallel install and independent signing, or one shared package ID with one signing lineage and documented Play/F-Droid replacement constraints.

### P2 — Release signing files and secret handling need concrete repo-safe artifacts.

One-sentence finding: The plan says release signing reads `keystore.properties`, but does not add an example file, ignore rule, or validation that secrets are absent from source.

Reference: Spec says signing config is wired via `keystore.properties` at line 313; Plan Phase 5a reads `keystore.properties` at lines 474-475; Plan decision 8 mentions encrypted GitHub Actions secrets at line 564.

Concrete fix: Add Phase 6a tasks for `keystore.properties.example`, `.gitignore` entries for real keystores/properties, CI validation that no `.jks`/`.keystore` files are committed, and release workflow documentation for `KEYSTORE_BASE64`, alias, and passwords.

### P2 — Play policy documentation is not turned into release artifacts.

One-sentence finding: The plan has disclosure UI, but lacks Play Console declaration copy and a policy-review checklist for `WRITE_SETTINGS` and `PACKAGE_USAGE_STATS`.

Reference: Spec identifies Play Store deceptive-settings risk at lines 347-350 and declares special permissions at lines 315-318; Plan Phase 4 about screen includes disclosure/privacy/FAQ at line 422; Phase 6 docs omit Play policy artifacts at lines 514-537.

Concrete fix: Add `docs/release/play-policy-declarations.md` with exact permission purpose, user-facing disclosure screenshots to capture, special-access denial behavior, and "no background changes" evidence; require this document before any Play release candidate.

## 5. RECOMMENDED FIXES

### P0 — Insert a Phase 0 toolchain quarantine before wrapper/version work.

One-sentence finding: Phase 0 cannot be considered executable until the plan defines what happens when Gradle or Android SDK 36 is absent.

Reference: Plan Phase 0b/0d/verification at lines 97-140.

Concrete fix: Add this task after Decision 0a.4: `DECISION/TASK 0a.5 Toolchain quarantine — verify JDK 17, gradle or wrapper-generation path, Android SDK platform 36, build-tools, ANDROID_HOME, and sdkmanager; if missing, create install tasks and do not start Phase 0b.`

### P1 — Patch the domain model around `ResolvedCapability`.

One-sentence finding: Capability identity, registry source metadata, control state, and disclosure routing need one shared contract.

Reference: Spec domain model at lines 152-205; Plan Phase 1b/1c at lines 157-198; Plan Phase 4a/4b at lines 389-414.

Concrete fix: Add `ResolvedCapability(id, ruleId, descriptor, registryVersion, source)` in `:core:model`, make registry return it, use it for Home card keys, DisclosureNavKey, OperationRecord, and history revert.

### P1 — Patch control execution to return state snapshots, not Boolean-like success.

One-sentence finding: The current control result type cannot enforce the spec's "Active requires both" rule.

Reference: Spec risk mitigation at lines 349-353; Plan control/result tasks at lines 254-314.

Concrete fix: Replace `ControlResult.Success` with `ControlExecutionResult` variants: `WriteAcknowledged(snapshot)`, `RequiresUserAction(request)`, `Failed(reason)`, and `NoOpAlreadyInState(snapshot)`; update tests to assert `WriteAcknowledged(PendingConditions(...))` for Samsung when preconditions are absent.

### P1 — Reorder feature and flavor phases to remove cycles.

One-sentence finding: Current ordering creates missing-module and circular dependency failures before a vertical slice can assemble.

Reference: Plan Phase 4/5 ordering at lines 385-493.

Concrete fix: Add Phase 4-pre for all feature API modules; move flavor plumbing fully to Phase 0d; make Phase 4f independent of Phase 5; make Phase 5 depend on compiled features.

### P1 — Add real-device acceptance gates for the Samsung write path.

One-sentence finding: The current verification path cannot prove the product's riskiest behavior.

Reference: Spec Samsung preconditions at lines 264-284; Plan tests and smoke at lines 314 and 501-505.

Concrete fix: Add a `docs/testing/samsung-pause-pd-acceptance.md` checklist plus a Phase 5 verification item requiring physical device evidence: before/after settings value, permission state, precondition checklist state, history record, revert, and UI not showing Active unless conditions are met.

### P1 — Add OTA registry integrity and rollback requirements before any network fetch lands.

One-sentence finding: Data-only OTA still changes device-control behavior and must be treated as a signed policy artifact.

Reference: Spec OTA design and risk table at lines 242-276 and 347-353; Plan registry tasks at lines 187-198 and decision 3 at lines 557-560.

Concrete fix: Add signed registry metadata, public-key pinning, monotonic registry versions, unsupported-schema rejection, atomic cache writes, and bundled fallback tests to Phase 1c plus the missing `:core:data` phase.

### P1 — Make open-source release hygiene a release blocker, not optional polish.

One-sentence finding: AGPL/F-Droid/signing tasks are necessary for a credible public Android project, not post-slice niceties.

Reference: Spec open-source section at lines 334-341; Plan Phase 6 at lines 512-549.

Concrete fix: Expand Phase 6a/6c to include `LICENSE`, `NOTICE`, `THIRD_PARTY_NOTICES.md`, F-Droid metadata, dependency locking, wrapper validation, release signing docs, and channel-specific app ID/signing policy.

## Verdict

The implementation plan has a strong module outline and correctly recognizes the two distribution flavors, disclosure UX, and Samsung preconditions, but it is not yet safe to execute as a vertical-slice plan. The biggest architectural defect is that identity, state, and execution contracts do not line up: registry entries cannot be routed, `Success` cannot mean "active," and Home can drift from control strategy truth. The biggest sequencing defect is that feature APIs, flavor plumbing, and control dependencies are ordered in ways that will cause avoidable build failures or rework. Phase 0 is blocked only by the missing toolchain quarantine; the remaining blockers are Phase 1-6 issues that should be patched before implementation reaches those phases.
