package com.chargepilot.core.control

import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.CapabilityType
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.Evidence
import com.chargepilot.core.model.IntentSpec
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Pure characterization of official-intent building rules used by
 * [OfficialGuidanceStrategy]. Full resolveActivity + startActivity needs a Context
 * (Robolectric); here we document the IntentSpec → Intent field mapping contract.
 */
class OfficialGuidanceIntentTest {

    @Test
    fun `descriptor without officialIntent is not guidance-available`() {
        val descriptor = CapabilityDescriptor(
            id = "x::BATTERY_LIMIT_80",
            type = CapabilityType.BATTERY_LIMIT_80,
            availableMethods = listOf(ControlMethod.OFFICIAL_GUIDANCE),
            evidence = Evidence.OFFICIAL_DOC,
        )
        assertThat(descriptor.officialIntent).isNull()
    }

    @Test
    fun `intent spec carries action component and fallback`() {
        val spec = IntentSpec(
            action = "android.settings.BATTERY_SAVER_SETTINGS",
            categories = listOf("android.intent.category.DEFAULT"),
            component = "com.android.settings/.Settings\$BatterySaverSettingsActivity",
            fallbackPath = "Settings → Battery → Adaptive charging",
        )
        assertThat(spec.action).isEqualTo("android.settings.BATTERY_SAVER_SETTINGS")
        assertThat(spec.component).contains("BatterySaver")
        assertThat(spec.fallbackPath).isNotEmpty()
    }

    @Test
    fun `guidance method is never write settings`() {
        assertThat(ControlMethod.OFFICIAL_GUIDANCE).isNotEqualTo(ControlMethod.WRITE_SETTINGS_KEY)
    }
}
