# Repository Guidelines

## Project Structure & Module Organization
Charge Pilot is a Kotlin/Android multi-module project. The app entry point lives in `app/`, with `play` and `full` distribution flavors. Shared logic is split under `core/`: `core:model` for domain models, `core:capability` for the device capability registry, `core:control*` for control strategies, and `core:ui` / `core:designsystem` for reusable UI. Feature code follows `feature/<name>/api` and `feature/<name>/impl`; keep public navigation contracts in `api` and implementation details in `impl`. Build conventions are in `build-logic/convention`. Device capability data is stored at `core/capability/src/main/assets/capabilities-v1.json`.

## Build, Test, and Development Commands
- `./gradlew :app:assemblePlayDebug` — build the Google Play-compatible debug APK.
- `./gradlew :app:assembleFullDebug` — build the full debug APK with privileged-mode modules.
- `./gradlew test` — run all JVM/unit tests.
- `./gradlew :core:capability:test` — validate capability registry parsing and schema behavior.
- `./gradlew :core:model:test :core:capability:test :core:control:test` — mirrors the focused CI unit-test set.
- `./gradlew clean` — remove generated build outputs when local Gradle state is stale.

Use JDK 17; Gradle wrapper files are committed, so prefer `./gradlew` over a system Gradle.

## Coding Style & Naming Conventions
Use Kotlin official style (`kotlin.code.style=official`) with 4-space indentation. Convention plugins set JVM target 17 and opt in to `kotlin.RequiresOptIn`. Package names follow `com.chargepilot.<layer>.<module>`. Name tests after the subject and behavior, for example `ControlOrchestratorTest` or `DeviceDetectorTest`. Prefer Hilt `@Binds` modules for interfaces, and keep Compose UI in feature or UI modules rather than core domain modules.

## Testing Guidelines
Use JUnit 5 and Truth for unit tests; use coroutine test utilities when testing suspend or Flow behavior. Put JVM tests in `src/test/kotlin`. Any change under `core/control*` must include a unit test for the new path. Registry changes must run `./gradlew :core:capability:test` and include evidence for device behavior.

## Commit & Pull Request Guidelines
Conventional commits are encouraged, e.g. `feat(capability): add OnePlus rule` or `fix(write-settings): keep inactive when charger absent`. Keep one feature or fix per PR. PRs must include a summary, tests run, and linked evidence for capability registry changes. UI changes need light and dark screenshots. Do not add telemetry or silent settings writes; all device-setting changes must be explicit, reversible, and user initiated.
