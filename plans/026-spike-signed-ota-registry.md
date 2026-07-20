# Plan 026: Spike only — signed OTA registry design (do not ship fetch)

> **Drift check**: ensure plan 001 landed so docs are not lying about live OTA.

## Status

- **Priority**: P3 · **Effort**: M · **Risk**: HIGH if implemented carelessly · **Depends on**: 001 · **Category**: direction | security  
- **Planned at**: `1f293cf`, 2026-07-21

## Why this matters

Design + old PRIVACY text envisioned non-blocking OTA of `capabilities-v1.json` so brand matrix grows without APK releases. Loader comments anticipated cache. Implementing fetch **without** integrity turns remote JSON into a privileged control policy surface (especially with write methods). This plan is a **design/spike only**: produce a threat model and API sketch; **do not** enable network fetch in production code until a follow-up plan with signature verification is approved.

## Constraints from repo

- Allowlist for keys (009) must remain code-side.
- Disclosure (012) still required for writes.
- Bundled asset is always fallback.
- No telemetry / no device identifiers in fetch headers beyond stock User-Agent.

## Spike deliverable

Write `docs/architecture/ota-registry.md` covering:

1. URL hosting (GitHub Releases asset preferred over raw main)
2. Metadata: schemaVersion, minAppVersion, publishedAt, content hash
3. Signature scheme (minisign/ed25519 detached signature; public key embedded in app)
4. Apply-next-launch atomic file replace
5. Failure modes: network fail, bad sig, schema mismatch → keep bundled
6. Explicit **non-goals**: remote code, dynamic DEX, remote allowlist of arbitrary keys

## Out of scope for this plan

- Implementing HTTP client
- Re-adding INTERNET without the verification code in the **same** future implementation PR
- Changing SECURITY.md to claim OTA is live

## Done criteria

- [ ] Architecture doc reviewed-quality exists
- [ ] No production network fetch added
- [ ] README 026 DONE

## STOP

- Pressure to “just fetch unsigned JSON” → refuse; remain spike.
