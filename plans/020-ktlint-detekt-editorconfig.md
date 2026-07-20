# Plan 020: Add ktlint + detekt + editorconfig

> **Drift check**: `git diff --stat 1f293cf..HEAD -- build-logic gradle .github/workflows .editorconfig config`

## Status

- **Priority**: P3 · **Effort**: M · **Risk**: MED · **Depends on**: none · **Category**: dx  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

Design and impl plan promised PR-blocking ktlint + detekt; neither exists. Multi-module Kotlin style is prose-only (`kotlin.code.style=official`). Agents and humans get format failures only after human review.

## Current state

- No `.editorconfig`, no `config/detekt`, no ktlint/detekt in `libs.versions.toml`
- CI quality job: `test` + Android lint only

## Scope

**In**:
- `.editorconfig` (indent 4, max_line_length 120, kotlin)
- Version catalog plugins for ktlint (or Spotless) + detekt
- Convention plugin wiring for Android/JVM modules
- `config/detekt/detekt.yml` baseline (start default; generate baseline if noise)
- CI step: `./gradlew ktlintCheck detekt` (or spotlessCheck) in quality job **after** first green local format

**Out**: Reformatting unrelated markdown; enabling every detekt rule at max severity day one.

## Steps

1. Add `.editorconfig`.
2. Add plugins to catalog + root/build-logic.
3. Run format once (`ktlintFormat` / spotlessApply) in a dedicated commit if large.
4. Add detekt with baseline file committed if needed.
5. Wire CI.
6. Document commands in AGENTS.md.

**Verify**: `./gradlew ktlintCheck detekt test` SUCCESS.

## Done criteria

- [ ] Local + CI static analysis commands exist and pass
- [ ] `.editorconfig` present
- [ ] README 020 DONE

## STOP

- First format touch >50% of files → commit format separately; do not mix with behavior changes.
- Plugin versions incompatible with AGP 8.7 / Kotlin 2.0 → pick known-good versions; report matrix.
