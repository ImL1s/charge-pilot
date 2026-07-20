# Spike: signed OTA capability registry (design only)

**Status**: design / spike. **Not implemented.** Do not re-add `INTERNET` or a network fetch until a follow-up PR ships signature verification in the same change.

## Goals

- Grow the brand matrix without forcing an APK release for every evidence update.
- Keep device control policy integrity: remote JSON must not become an unsigned remote root for system settings keys.

## Non-goals

- Remote code, DEX loading, or dynamic strategy plugins.
- Expanding the writable-key allowlist via remote data alone (allowlist stays in app code; plan 009).
- Telemetry or device identifiers in fetch headers beyond a stock User-Agent.
- Claiming OTA is live in PRIVACY/SECURITY while code is offline-only.

## Hosting

Prefer **GitHub Releases assets** (immutable per tag) over raw `main` URLs:

- `capabilities-v1.json`
- `capabilities-v1.json.minisig` (or `.sig`) detached signature

Pinned base URL pattern (illustrative):  
`https://github.com/<org>/<repo>/releases/download/registry-<version>/…`

## Metadata (JSON fields to add when shipping)

| Field | Purpose |
|-------|---------|
| `schemaVersion` | Reject incompatible schemas |
| `minAppVersion` | Do not apply registry newer than app can interpret |
| `publishedAt` | Human audit |
| `contentHash` | SHA-256 of canonical JSON bytes |
| `rules` | Existing rule list |

## Signature scheme

1. Project holds an offline **ed25519 / minisign** private key; public key is embedded in the app (BuildConfig or assets).
2. CI or a release operator signs the release asset.
3. On device: download JSON + signature to a temp file → verify → atomic rename into app-private cache.
4. Failure (network, bad sig, schema, minAppVersion) → keep previous cache or bundled asset; never apply.

## Apply policy

- **Apply next launch** after successful verify (simplest atomicity).
- Bundled APK asset remains the hard fallback and the Play-review baseline.
- Loader order when OTA lands: verified cache → bundled.

## Threat notes

- Without signature, an MITM or compromised host could enable dangerous keys if the app trusted registry-only allowlists. Code-side `WritableSettingsKeys` remains mandatory.
- Disclosure consent (plan 012) still applies per capability id even for OTA-added rules.
- Do not log full fingerprints or user identifiers during fetch diagnostics.

## Implementation checklist (future PR)

1. Re-add INTERNET + ACCESS_NETWORK_STATE **with** call sites.
2. Update PRIVACY.md + SECURITY.md in the same PR.
3. Signature verify path + unit tests with fixed vectors.
4. `CapabilityRegistry.clearCache()` after applying new snapshot.
5. No production fetch of unsigned JSON under any feature flag.
