package com.chargepilot.feature.about.impl

import com.chargepilot.core.model.DeviceProfile

/**
 * Builds a markdown device report suitable for pasting into GitHub issue templates.
 * Pure function — unit-tested without Android.
 */
object DeviceReportFormatter {
    fun format(
        profile: DeviceProfile,
        appVersionName: String,
        appVersionCode: Long,
    ): String = buildString {
        appendLine("### Device report (Charge Pilot)")
        appendLine()
        appendLine("| Field | Value |")
        appendLine("|---|---|")
        appendLine("| App version | $appVersionName ($appVersionCode) |")
        appendLine("| Manufacturer | ${profile.manufacturer.name} |")
        appendLine("| Build.MODEL | ${profile.model} |")
        appendLine("| Codename | ${profile.codename} |")
        appendLine("| Android | ${profile.androidVersion} (API ${profile.androidApi}) |")
        val rom = profile.romVersion
        if (rom != null) {
            val label = rom.displayLabel ?: "${rom.flavor.name} ${rom.version}"
            appendLine("| ROM | $label |")
            appendLine("| ROM raw | ${rom.raw} |")
        } else {
            appendLine("| ROM | unknown |")
        }
        appendLine("| Fingerprint | `${profile.buildFingerprint}` |")
        appendLine()
        appendLine("### Capability notes")
        appendLine()
        appendLine("_Describe which capability you tested and the result._")
    }
}
