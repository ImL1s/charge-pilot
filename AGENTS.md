# Repository Guidelines

## Project Structure & Module Organization
Charge Pilot is a Kotlin/Android multi-module project. The app entry point lives in `app/`, with `play` and `full` distribution flavors. Shared logic is split under `core/`:

| Module | Role |
|--------|------|
| `core:model` | Domain models |
| `core:capability` | Device capability registry (`assets/capabilities-v1.json`) |
| `core:control` | Orchestrator + official guidance |
| `core:control-writesettings` | WRITE_SETTINGS strategy |
| `core:control-shizuku` | Shizuku strategy (**full** only) |
| `core:control-root` | Root stub (**full** only; no libsu while stubbed) |
| `core:domain` | Precondition checker, ControlStateBridge |
| `core:data` | Thin repositories |
| `core:datastore` | Preferences + operation history |
| `core:device` | DeviceDetector / system props |
| `core:battery` / `core:foreground` | Runtime precondition inputs |
| `core:ui` / `core:designsystem` | Reusable Compose UI |
| `core:common` / `core:testing` | Shared utilities / test helpers |

Feature code follows `feature/<name>/api` and `feature/<name>/impl` for: home, brands, advanced, history, about, disclosure. Keep public navigation contracts in `api` and implementation details in `impl`. Build conventions are in `build-logic/convention`.

The capability registry is **bundled only** (no OTA network fetch). See `docs/PRIVACY.md`.

## Build, Test, and Development Commands
- `./gradlew :app:assemblePlayDebug` — Google Play-compatible debug APK.
- `./gradlew :app:assembleFullDebug` — full debug APK with privileged-mode modules.
- `./gradlew test` — all JVM/unit tests (CI quality job).
- `./gradlew lintPlayDebug lintFullDebug` — Android lint for both flavors (CI).
- `./gradlew :core:capability:test` — registry parsing / matching.
- `./gradlew :core:model:test :core:capability:test :core:control:test :core:device:test :core:battery:test :core:domain:test` — focused local set.

Use **JDK 17**; Gradle wrapper is committed. Convention plugins enable `useJUnitPlatform()` for unit tests.

## Coding Style & Naming Conventions
Use Kotlin official style (`kotlin.code.style=official`) with 4-space indentation. See `.editorconfig`. Convention plugins set JVM target 17 and opt in to `kotlin.RequiresOptIn`. Package names follow `com.chargepilot.<layer>.<module>`. Name tests after the subject and behavior, for example `ControlOrchestratorTest` or `DeviceDetectorTest`. Prefer Hilt `@Binds` modules for interfaces, and keep Compose UI in feature or UI modules rather than core domain modules.

Writable system keys must be listed in `WritableSettingsKeys` **and** the registry in the same change with evidence.

## Testing Guidelines
Use JUnit 5 and Truth for unit tests; use coroutine test utilities when testing suspend or Flow behavior. Put JVM tests in `src/test/kotlin`. Any change under `core/control*` must include a unit test for the new path. Registry changes must run `./gradlew :core:capability:test` and include evidence for device behavior.

## Commit & Pull Request Guidelines
Conventional commits are encouraged, e.g. `feat(capability): add OnePlus rule` or `fix(write-settings): keep inactive when charger absent`. Keep one feature or fix per PR. PRs must include a summary, tests run, and linked evidence for capability registry changes. UI changes need light and dark screenshots. Do not add telemetry or silent settings writes; all device-setting changes must be explicit, reversible, and user initiated.
