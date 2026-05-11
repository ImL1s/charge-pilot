package com.chargepilot.core.device

import android.os.Build
import com.chargepilot.core.model.DeviceProfile
import com.chargepilot.core.model.Manufacturer
import com.chargepilot.core.model.RomFlavor
import com.chargepilot.core.model.RomVersion
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads `android.os.Build` and OEM-specific system properties to produce a
 * [DeviceProfile]. Pure, no IO outside `Build.*` and `getprop` invocations.
 */
@Singleton
class DeviceDetector @Inject constructor(
    private val systemPropertyReader: SystemPropertyReader,
) {
    fun detect(): DeviceProfile {
        val manufacturer = Manufacturer.fromBuild(Build.MANUFACTURER)
        return DeviceProfile(
            manufacturer = manufacturer,
            model = Build.MODEL.orEmpty(),
            codename = Build.DEVICE.orEmpty(),
            androidApi = Build.VERSION.SDK_INT,
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            romVersion = readRomVersion(manufacturer),
            buildFingerprint = Build.FINGERPRINT.orEmpty(),
        )
    }

    @androidx.annotation.VisibleForTesting
    internal fun readRomVersion(manufacturer: Manufacturer): RomVersion? = when (manufacturer) {
        Manufacturer.SAMSUNG -> readSamsungRom()
        Manufacturer.GOOGLE -> readSingle(RomFlavor.AOSP, "ro.build.version.release")
        Manufacturer.ONEPLUS -> readSingle(RomFlavor.OXYGEN_OS, "ro.oxygen.version")
        Manufacturer.XIAOMI -> readSingle(RomFlavor.HYPER_OS, "ro.miui.ui.version.name")
        Manufacturer.HONOR -> readSingle(RomFlavor.MAGIC_OS, "ro.build.version.magic")
        Manufacturer.HUAWEI -> readSingle(RomFlavor.EMUI, "ro.build.version.emui")
        Manufacturer.OTHER -> null
    }

    private fun readSingle(flavor: RomFlavor, prop: String): RomVersion? {
        val raw = systemPropertyReader.read(prop) ?: return null
        return RomVersion(flavor = flavor, version = parseVersionFromRaw(raw, flavor), raw = raw)
    }

    /**
     * Samsung uses different system properties depending on OneUI generation:
     *  - `ro.build.version.oneui` exists on newer devices (OneUI 4.0+ era) and stores a
     *    5–6 digit MMmmpp code (e.g. `40101` = 4.01.01).
     *  - `ro.build.version.sep` (Samsung Experience Platform) is present on most OneUI
     *    1.x–3.x devices and is the closest proxy to the OneUI version.
     *  - `ro.build.version.sem` is the legacy Samsung Experience marker on older builds.
     *
     * For SEP/SEM the canonical [RomVersion.version] is kept numeric (so registry
     * `minRomVersion` comparisons still work); the OneUI-approximated human-readable
     * label is attached via [RomVersion.displayLabel] for the UI.
     */
    private fun readSamsungRom(): RomVersion? {
        systemPropertyReader.read("ro.build.version.oneui")?.let { raw ->
            return RomVersion(RomFlavor.ONE_UI, parseOneUiCode(raw), raw)
        }
        systemPropertyReader.read("ro.build.version.sep")?.let { raw ->
            val numericVersion = parseSepCodeToNumericVersion(raw)
            val displayLabel = sepCodeToDisplayLabel(raw)
            return RomVersion(RomFlavor.ONE_UI, numericVersion, raw, displayLabel)
        }
        systemPropertyReader.read("ro.build.version.sem")?.let { raw ->
            // Legacy: keep raw as version so it is at least dot-parseable to 0 in registry,
            // and surface the raw value in displayLabel so users still see what was found.
            return RomVersion(RomFlavor.ONE_UI, raw, raw, displayLabel = "OneUI (SEM $raw)")
        }
        return null
    }

    private fun parseVersionFromRaw(raw: String, flavor: RomFlavor): String = when (flavor) {
        RomFlavor.ONE_UI -> parseOneUiCode(raw)
        RomFlavor.HYPER_OS -> raw.removePrefix("V").trim()
        else -> raw
    }

    private fun parseOneUiCode(raw: String): String {
        val intCode = raw.toIntOrNull() ?: return raw
        // Samsung OneUI version is encoded as MMmmpp (5–6 digits): e.g. 40101 = 4.01.01,
        // 60101 = 6.01.01. Reject anything below 10_000 as too short to be a valid code.
        if (intCode < 10_000) return raw
        val major = intCode / 10_000
        val minor = (intCode / 100) % 100
        return "$major.$minor"
    }

    /** Returns a numeric, dot-parseable version derived from a SEP code (e.g. `110500` -> `"11.05"`). */
    private fun parseSepCodeToNumericVersion(raw: String): String {
        val intCode = raw.toIntOrNull() ?: return raw
        val sepMajor = intCode / 10_000
        val sepMinor = (intCode / 100) % 100
        return "$sepMajor.${sepMinor.pad2()}"
    }

    /** Returns a human-friendly label like `"OneUI 2.x · SEP 11.05"`. Display only. */
    private fun sepCodeToDisplayLabel(raw: String): String {
        val intCode = raw.toIntOrNull() ?: return "SEP $raw"
        val sepMajor = intCode / 10_000
        val sepMinor = (intCode / 100) % 100
        val oneUi = SEP_MAJOR_TO_ONEUI_APPROX[sepMajor]
            ?: return "SEP $sepMajor.${sepMinor.pad2()}"
        return "$oneUi · SEP $sepMajor.${sepMinor.pad2()}"
    }

    private fun Int.pad2(): String = toString().padStart(2, '0')

    private companion object {
        /**
         * Approximate SEP-major → OneUI-major mapping. Same SEP major can ship across
         * multiple OneUI minor revisions (Samsung does not publish a strict mapping),
         * so the value is intentionally fuzzy ("2.x", "3.x").
         */
        val SEP_MAJOR_TO_ONEUI_APPROX: Map<Int, String> = mapOf(
            9 to "OneUI 1.x",
            10 to "OneUI 2.x",
            11 to "OneUI 2.x",
            12 to "OneUI 3.x",
            13 to "OneUI 4.x",
            14 to "OneUI 5.x",
            15 to "OneUI 6.x",
            16 to "OneUI 7.x",
        )
    }
}
