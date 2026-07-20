package com.chargepilot.feature.about.impl

import com.chargepilot.core.model.DeviceProfile
import com.chargepilot.core.model.Manufacturer
import com.chargepilot.core.model.RomFlavor
import com.chargepilot.core.model.RomVersion
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DeviceReportFormatterTest {

    @Test
    fun `markdown includes model and manufacturer`() {
        val profile = DeviceProfile(
            manufacturer = Manufacturer.SAMSUNG,
            model = "SM-S9280",
            codename = "e3q",
            androidApi = 36,
            androidVersion = "16",
            romVersion = RomVersion(RomFlavor.ONE_UI, "7.0", "70000"),
            buildFingerprint = "samsung/e3q/e3q:16/...",
        )
        val md = DeviceReportFormatter.format(profile, appVersionName = "0.1.0", appVersionCode = 1)
        assertThat(md).contains("SM-S9280")
        assertThat(md).contains("SAMSUNG")
        assertThat(md).contains("0.1.0")
        assertThat(md).contains("Build.MODEL")
    }
}
