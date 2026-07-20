# Plan 027: README/AGENTS onboarding + CI Android SDK setup

> **Drift check**: `git diff --stat 1f293cf..HEAD -- README.md AGENTS.md .github/workflows/build.yml`

## Status

- **Priority**: P2 · **Effort**: S · **Risk**: LOW · **Depends on**: none · **Category**: dx  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

README is product-only; build commands live in AGENTS.md. AGENTS module list is incomplete vs `settings.gradle.kts`. CI quality job may lack explicit Android SDK setup (only JDK + setup-gradle). New contributors and agents fail first builds.

## Current state

- README: no JDK/SDK/gradle instructions
- AGENTS: partial core module list; says focused CI set while workflow runs full `./gradlew test`
- `.github/workflows/build.yml` quality job: checkout, setup-java 17, setup-gradle, test, lint — no `android-actions/setup-android` / sdkmanager

## Scope

**In**: README Development section; AGENTS.md accuracy; workflow SDK install if needed.

**Out**: Full Dependabot matrix; ktlint (020).

## Steps

1. README: JDK 17, Android SDK platform 36, `local.properties` sdk.dir, commands for play/full debug, test, lint.
2. AGENTS: sync modules with settings.gradle.kts; document actual CI commands; note no OTA (001).
3. CI: add Android SDK setup action or cmdline-tools install before Gradle if clean runners fail — **verify** by checking recent Actions or running in a clean environment; if already green via cached SDK, document why.
4. Optional: `keystore.properties.example` keys only (no secrets) for local release signing docs.

**Verify**: markdown renders; workflow YAML valid.

## Done criteria

- [ ] Clone→build path documented in README
- [ ] AGENTS matches repo
- [ ] CI SDK gap addressed or documented as N/A with evidence
- [ ] README 027 DONE
