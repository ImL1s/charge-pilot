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
     *    5–6 digit MMmmpp code (e.g. `40101` = 4.01.01). **Only this path is used for
     *    `minRomVersion` comparisons** against ONE_UI requirements.
     *  - `ro.build.version.sep` / `sem` are still read for UI labels, but their comparable
     *    [RomVersion.version] is the **mapped One UI major** (not the SEP major like 11.x),
     *    so old SEP builds never falsely satisfy `minRomVersion` ONE_UI 6.0.
     */
    private fun readSamsungRom(): RomVersion? {
        systemPropertyReader.read("ro.build.version.oneui")?.let { raw ->
            return RomVersion(RomFlavor.ONE_UI, parseOneUiCode(raw), raw)
        }
        systemPropertyReader.read("ro.build.version.sep")?.let { raw ->
            val comparable = sepCodeToComparableOneUi(raw)
            val displayLabel = sepCodeToDisplayLabel(raw)
            return RomVersion(RomFlavor.ONE_UI, comparable, raw, displayLabel)
        }
        systemPropertyReader.read("ro.build.version.sem")?.let { raw ->
            // SEM cannot be mapped reliably to One UI; use "0" so minRomVersion fails closed.
            return RomVersion(
                RomFlavor.ONE_UI,
                version = "0",
                raw = raw,
                displayLabel = "OneUI (SEM $raw)",
            )
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

    /**
     * Maps a SEP code to a comparable One UI major.minor for registry matching.
     * Example: SEP `110500` → One UI ~2.x → `"2.0"` (never `"11.05"`).
     * Unknown SEP majors fail closed with `"0"`.
     */
    private fun sepCodeToComparableOneUi(raw: String): String {
        val intCode = raw.toIntOrNull() ?: return "0"
        val sepMajor = intCode / 10_000
        val oneUiMajor = SEP_MAJOR_TO_ONEUI_MAJOR[sepMajor] ?: return "0"
        return "$oneUiMajor.0"
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

        /** Integer major used for minRomVersion comparisons (not display). */
        val SEP_MAJOR_TO_ONEUI_MAJOR: Map<Int, Int> = mapOf(
            9 to 1,
            10 to 2,
            11 to 2,
            12 to 3,
            13 to 4,
            14 to 5,
            15 to 6,
            16 to 7,
        )
    }
}
