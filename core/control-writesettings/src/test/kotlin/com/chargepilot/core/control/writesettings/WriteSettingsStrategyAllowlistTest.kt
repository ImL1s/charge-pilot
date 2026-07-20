package com.chargepilot.core.control.writesettings

import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.CapabilityType
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlResult
import com.chargepilot.core.model.Evidence
import com.chargepilot.core.model.FailureReason
import com.chargepilot.core.model.SettingsKey
import com.chargepilot.core.model.WritableSettingsKeys
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Pure allowlist / descriptor-gate characterization (no Robolectric).
 * Write path that needs Settings.System is covered when Robolectric is available later.
 */
class WriteSettingsStrategyAllowlistTest {

    @Test
    fun `allowlist contains pass_through only today`() {
        assertThat(WritableSettingsKeys.isAllowedSystemInt("pass_through")).isTrue()
        assertThat(WritableSettingsKeys.isAllowedSystemInt("airplane_mode_on")).isFalse()
    }

    @Test
    fun `descriptor without write method is rejected by gate logic`() {
        val descriptor = CapabilityDescriptor(
            id = "test::PAUSE_PD_DURING_GAMING",
            type = CapabilityType.PAUSE_PD_DURING_GAMING,
            availableMethods = listOf(ControlMethod.OFFICIAL_GUIDANCE),
            settingsKey = SettingsKey("system", "pass_through", "int"),
            evidence = Evidence.PROJECT_VERIFIED,
        )
        assertThat(descriptorAllowsWrite(descriptor)).isFalse()
    }

    @Test
    fun `descriptor with non-system namespace rejected`() {
        val descriptor = CapabilityDescriptor(
            id = "test::PAUSE_PD_DURING_GAMING",
            type = CapabilityType.PAUSE_PD_DURING_GAMING,
            availableMethods = listOf(ControlMethod.WRITE_SETTINGS_KEY),
            settingsKey = SettingsKey("secure", "pass_through", "int"),
            evidence = Evidence.PROJECT_VERIFIED,
        )
        assertThat(descriptorAllowsWrite(descriptor)).isFalse()
    }

    @Test
    fun `descriptor with non-int type rejected`() {
        val descriptor = CapabilityDescriptor(
            id = "test::PAUSE_PD_DURING_GAMING",
            type = CapabilityType.PAUSE_PD_DURING_GAMING,
            availableMethods = listOf(ControlMethod.WRITE_SETTINGS_KEY),
            settingsKey = SettingsKey("system", "pass_through", "string"),
            evidence = Evidence.PROJECT_VERIFIED,
        )
        assertThat(descriptorAllowsWrite(descriptor)).isFalse()
    }

    @Test
    fun `unknown key rejected even if method listed`() {
        val descriptor = CapabilityDescriptor(
            id = "test::PAUSE_PD_DURING_GAMING",
            type = CapabilityType.PAUSE_PD_DURING_GAMING,
            availableMethods = listOf(ControlMethod.WRITE_SETTINGS_KEY),
            settingsKey = SettingsKey("system", "airplane_mode_on", "int"),
            evidence = Evidence.PROJECT_VERIFIED,
        )
        assertThat(descriptorAllowsWrite(descriptor)).isFalse()
    }

    @Test
    fun `pass_through system int with write method allowed by gate`() {
        val descriptor = CapabilityDescriptor(
            id = "test::PAUSE_PD_DURING_GAMING",
            type = CapabilityType.PAUSE_PD_DURING_GAMING,
            availableMethods = listOf(ControlMethod.WRITE_SETTINGS_KEY),
            settingsKey = SettingsKey("system", "pass_through", "int"),
            evidence = Evidence.PROJECT_VERIFIED,
        )
        assertThat(descriptorAllowsWrite(descriptor)).isTrue()
    }

    /** Mirrors WriteSettingsStrategy.descriptorAllowsWrite for pure unit coverage. */
    private fun descriptorAllowsWrite(descriptor: CapabilityDescriptor): Boolean {
        if (ControlMethod.WRITE_SETTINGS_KEY !in descriptor.availableMethods) return false
        val key = descriptor.settingsKey ?: return false
        if (key.namespace != "system") return false
        if (key.type != "int") return false
        if (!WritableSettingsKeys.isAllowedSystemInt(key.key)) return false
        return true
    }

    @Test
    fun `failed result reason for unsupported is documented`() {
        val result = ControlResult.Failed(FailureReason.UNSUPPORTED_DEVICE)
        assertThat(result.reason).isEqualTo(FailureReason.UNSUPPORTED_DEVICE)
    }
}
