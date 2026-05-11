package com.chargepilot.core.model

/**
 * Snapshot of the running device's identity. Built from `android.os.Build.*` and per-OEM
 * ROM-version probes (One UI, OxygenOS, HyperOS, MagicOS, EMUI). All fields are
 * best-effort; missing data is `null` rather than guessed.
 */
data class DeviceProfile(
    val manufacturer: Manufacturer,
    val model: String,
    val codename: String,
    val androidApi: Int,
    val androidVersion: String,
    val romVersion: RomVersion?,
    val buildFingerprint: String,
)

/**
 * ROM-specific version, e.g. One UI 7.0, OxygenOS 15, HyperOS 2, MagicOS 10.
 *
 * [version] must remain numerically parseable (dot-separated integers) so that
 * [CapabilityRegistry] can compare against `minRomVersion` rules in the registry.
 * [displayLabel] is the optional, human-friendly string the UI should render when
 * the canonical [version] alone is not informative (e.g. SEP-derived OneUI codes
 * where the SEP-to-OneUI mapping is approximate).
 */
data class RomVersion(
    val flavor: RomFlavor,
    val version: String,
    val raw: String,
    val displayLabel: String? = null,
)

enum class RomFlavor { ONE_UI, OXYGEN_OS, COLOR_OS, HYPER_OS, MIUI, MAGIC_OS, EMUI, HONOR_MAGIC, AOSP, OTHER }
