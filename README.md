# Charge Pilot

Cross-brand Android **charging-capability detector and safe controller**.

> **Not** a "bypass charging cracker". Charge Pilot detects what your device officially supports, surfaces the manufacturer's documented entry points, and — only on whitelisted devices and after explicit user consent — offers experimental control over charging-related system settings. Every change is reversible. Nothing is modified in the background.

## Status

Pre-alpha. Architecture and brand support matrix are public; runtime control is being implemented brand by brand.

## Three layers

1. **Capability detection + official guidance** — works on every device. Tells you what your phone supports and opens the manufacturer's settings page.
2. **Experimental write-settings control** — on whitelisted devices, with user consent, writes documented community-known keys via Android's `WRITE_SETTINGS` special access. Available in both `play` and `full` flavors.
3. **Privileged advanced mode** — Shizuku and root paths for devices without a public `WRITE_SETTINGS` route. **`full` flavor only — not on Google Play.**

## Flavors

| Flavor | Distribution | Layers | Notes |
|---|---|---|---|
| `play` | Google Play Store | 1 + 2 | No Shizuku, no root, no `QUERY_ALL_PACKAGES`. |
| `full` | F-Droid, GitHub Releases | 1 + 2 + 3 | Adds Shizuku and root strategies. |

## Brand support (v0)

| Brand | Tier | Methods | Source |
|---|---|---|---|
| Samsung Galaxy S/Z | S2 | Official Game Booster guide in-app; full flavor Shizuku only for verified `pass_through` devices | Samsung official; S24U direct Game Booster activity is signature-gated |
| Google Pixel | S2 | Charging optimization / Adaptive Charging / 80% limit guidance | Pixel official |
| OnePlus | S2 | OxygenOS 16 bypass charging guidance on supported models | OnePlus official community OTA notes |
| Xiaomi/Redmi/POCO | S2 | Battery protection / optimized charging guidance; POCO F7 bypass explicitly unsupported | Xiaomi official FAQs |
| HONOR | S2 | Smart Charge / Smart Battery Capacity guidance | HONOR official |
| Huawei | S2 | Smart Charge / Custom limit guidance | Huawei official |

S1 = direct strategy fully verified · S2 = official guidance or privileged path only · S3 = unverified.

See [docs/brand-methods.md](docs/brand-methods.md) for the current per-brand evidence table. Direct control is hidden unless the bundled registry has both a direct method and a verified writable key.

## License

[AGPL-3.0-or-later](LICENSE). Forks, including those distributed as a network service, must publish source code.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Device-matrix PRs require attached evidence (video, ADB log, or screen recording).

## Privacy

No telemetry. No analytics. No personal data leaves the device. See [docs/PRIVACY.md](docs/PRIVACY.md).
