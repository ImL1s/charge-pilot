# Contributing to Charge Pilot

Thanks for considering a contribution. Charge Pilot is a small open-source utility with strict goals around safety, transparency, and user trust. Please read this in full before opening a PR.

## Ground rules

1. **No silent system changes.** Never propose code that modifies device settings without an explicit, dismissable user action.
2. **Reversible by default.** Any state that the app can put the device into, the app must also be able to revert.
3. **Distinguish "wrote a key" from "feature is active."** A successful settings write does not imply the OEM-defined preconditions (charger, battery, foreground app) are satisfied.
4. **No telemetry.** Charge Pilot does not call home. Normal use requires no network; the capability registry ships bundled in the APK.
5. **AGPL-3.0-or-later.** By contributing, you agree your contribution is licensed under [AGPL-3.0-or-later](LICENSE).

## Adding a device or capability

The most useful contribution is widening the support matrix. To add a new device or capability rule:

1. Open `core/capability/src/main/assets/capabilities-v1.json`.
2. Add a new rule with a descriptive `id` and matchers that uniquely identify the device(s).
3. For each capability, set `availableMethods` to only what you have **personally verified**. `OFFICIAL_GUIDANCE` should always come first when the OEM exposes a Settings entry point.
4. Set `evidence` honestly:
   - `OFFICIAL_DOC` — there is a current OEM support page, manual, or official video.
   - `OFFICIAL_COMMUNITY` — official community moderator or customer-service post.
   - `COMMUNITY_TESTED` — XDA / Reddit / GitHub thread; multiple corroborations.
   - `PROJECT_VERIFIED` — *you* tested it on a physical device or emulator and have evidence.
   - `UNVERIFIED` — placeholder; do not ship.
5. Attach evidence in your PR description: a screen recording, ADB output, or photo. PRs that change the registry without evidence will be asked for it.
6. Run `./gradlew :core:capability:test` to make sure the schema validator still passes.

## Code style

- Kotlin official style. The convention plugins enforce JVM target 17 and `RequiresOptIn`.
- Compose Material 3: prefer `MaterialExpressiveTheme` shapes / motion only behind `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.
- Hilt: every interface gets a `@Module` `@Binds` registration. Don't `@Provides` instances when `@Binds` is enough.
- Tests: pure unit where possible; Robolectric for `Build.*` / `Settings.System` interaction; instrumentation for Compose.

## Commit messages

Conventional commits encouraged but not required:

```
feat(capability): add OnePlus 13 bypass charging rule
fix(write-settings): don't claim Active when key=1 but charger absent
docs(privacy): clarify bundled-only registry
```

## Pull requests

1. One feature/fix per PR. Avoid bundling unrelated changes.
2. PRs that add or change any code under `core/control*` MUST include a unit test exercising the new path.
3. UI-changing PRs must include screenshots in light + dark mode.
4. Keep PRs draft until tests pass locally.

## Security

If you find a vulnerability, do **not** open a public issue. See [SECURITY.md](SECURITY.md).
