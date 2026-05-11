## Summary

<!-- 1–3 sentences. What does this PR change and why? -->

## Type of change

- [ ] Capability registry update (add/modify a device or capability rule)
- [ ] Bug fix
- [ ] Feature
- [ ] Documentation only
- [ ] Refactoring / housekeeping

## Evidence (required for any registry change)

<!-- Screen recording, ADB log, or screenshots. PRs without evidence will be asked for one. -->

## Tests

- [ ] Existing tests pass (`./gradlew test`)
- [ ] New tests cover the new behavior

## Checklist

- [ ] All system-settings writes remain reversible.
- [ ] No code added that modifies settings without explicit user action.
- [ ] No new analytics, telemetry, or off-device data flow.
- [ ] AGPL-3.0-or-later compatible.
