# Plan 012: Wire disclosure consent gate before settings writes

> **Drift check**: `git diff --stat 1f293cf..HEAD -- app/src/main/kotlin/com/chargepilot/app/navigation feature/disclosure feature/home core/datastore`

## Status

- **Priority**: P1 · **Effort**: M · **Risk**: MED · **Depends on**: 011 · **Category**: security | direction  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

Design §4.4, `SECURITY.md`, and `docs/PRIVACY.md` require a multi-step disclosure before experimental settings writes. `feature/disclosure` exists as a static placeholder and is **not** on the NavHost. `HomeViewModel.tryEnable` calls `strategy.setEnabled` immediately. Full flavor can write Samsung `pass_through` via Shizuku without any acknowledgement checkboxes. This is the top trust/Play-policy gap.

## Current state

- `ChargePilotNavHost.kt:96-106` — routes: home, brands, history, advanced, about — **no disclosure**.
- `DisclosureRoute(capabilityId: String)` + `navigateToDisclosure` exist in `feature/disclosure/api`.
- `DisclosureScreen` — static InfoCards only; no Confirm, no checkboxes, no callback.
- `HomeViewModel.tryEnable` / `runCapabilityAction` — direct write.
- `feature/home/impl` depends on `disclosure:api` but does not import it.

**Design requirement (inline)**: First write shows (1) what changes — namespace/key/before/after + evidence (2) meaning — devices/preconditions/reversibility (3) confirm — two checkboxes + continue. Subsequent invocations of same capability may show confirm only.

## Scope

**In**:
- `app/.../ChargePilotNavHost.kt` — register `disclosureRoute`
- `feature/disclosure/impl` — real UI + ViewModel
- `feature/home/impl` — intercept tryEnable for direct methods; navigate or show sheet
- `core/datastore` prefs (from 011) for acknowledged ids
- Strings en + zh-rTW for disclosure feature
- Unit tests: gate blocks write without ack; allows after ack

**Out**:
- Changing strategy internals
- History revert (014)
- Full Material3 Expressive redesign of the sheet

## UX decision (execute this unless product overrides)

Use a **full-screen route** already scaffolded (`DisclosureRoute`) rather than inventing a new bottom sheet component in core:ui:

1. User taps Try/Turn on on Home for a **direct control** method (`WRITE_SETTINGS_KEY` | `SHIZUKU_RPC` | `ROOT_SHELL`).
2. If `!hasAcknowledged(capabilityId)` → `navController.navigateToDisclosure(capabilityId)` (Home needs NavController or a callback from `HomeScreen` → NavHost).
3. Disclosure loads descriptor by id from registry (inject CapabilityRegistry + DeviceDetector or pass args).
4. User checks required boxes → Continue → save ack → `popBackStack` → Home performs pending enable **or** disclosure ViewModel performs write then pops (prefer: disclosure confirms only; Home retries enable after resume).

Simpler reliable flow:

- Home sets `pendingEnableId` in VM when gate fails navigation.
- On `ON_RESUME`, if pending and now acknowledged, run action once and clear pending.

Official guidance / open settings paths **do not** require disclosure (no write).

## Steps

### Step 1: NavHost registration

```kotlin
disclosureRoute { capabilityId ->
    DisclosureScreen(capabilityId = capabilityId, onFinished = { navController.popBackStack() })
}
```

Home must receive `onNeedDisclosure: (String) -> Unit` from NavHost via homeRoute lambda — update `homeRoute { HomeScreen(onNeedDisclosure = …) }` if HomeScreen signature must change.

### Step 2: DisclosureScreen content

Show for capability:
- type/name, evidence, sourceUrl if any
- settingsKey namespace + key + intended value 0/1
- preconditions list
- method that will be used (from orchestrator pick if possible)
- Checkbox A: “I understand this is experimental and may not work on my firmware”
- Checkbox B: “I understand Charge Pilot will write a system setting only when I confirm”
- Button enabled only when both checked → `preferences.acknowledgeDisclosure(id)` → `onFinished()`

### Step 3: Gate in HomeViewModel.tryEnable

```kotlin
fun tryEnable(capabilityId: String) {
  // if direct control would run:
  viewModelScope.launch {
    if (!userPreferences.hasAcknowledged(capabilityId)) {
      _effects.emit(HomeEffect.OpenDisclosure(capabilityId)) // or callback
      return@launch
    }
    // existing runCapabilityAction
  }
}
```

Prefer SharedFlow one-shot effects for navigation; keep Compose free of business rules.

### Step 4: Tests

- hasAcknowledged false → no strategy.setEnabled invoked (fake orchestrator)
- after acknowledge → setEnabled called

### Step 5: Update SECURITY.md one line if plan 001 left a caveat about disclosure missing

**Verify**:
```bash
./gradlew :feature:disclosure:impl:compilePlayDebugKotlin :feature:home:impl:compilePlayDebugKotlin :app:assemblePlayDebug :app:assembleFullDebug
./gradlew :feature:home:impl:test :core:datastore:test
```

## Done criteria

- [ ] Disclosure route reachable from Home before first direct write
- [ ] No direct `setEnabled` without ack in prefs
- [ ] Subsequent toggles skip full disclosure (ack present)
- [ ] Official guidance still works without disclosure
- [ ] Both flavors assemble
- [ ] README 012 DONE

## STOP

- Navigation type-safe routes break compile — fix with existing kotlin serialization navigation pattern used by other routes.
- Cannot resolve capability id from registry — stop and report id format mismatch.

## Maintenance

- New capabilities automatically gated once they use tryEnable.
- Reviewers: confirm Play flavor cannot skip gate if WRITE_SETTINGS ever re-enters registry.
