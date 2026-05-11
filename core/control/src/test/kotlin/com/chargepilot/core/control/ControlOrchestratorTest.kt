package com.chargepilot.core.control

import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.CapabilityType
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlResult
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.DeviceProfile
import com.chargepilot.core.model.Evidence
import com.chargepilot.core.model.Manufacturer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ControlOrchestratorTest {

    private val profile = DeviceProfile(
        manufacturer = Manufacturer.SAMSUNG,
        model = "SM-S921B",
        codename = "e1q",
        androidApi = 35,
        androidVersion = "15",
        romVersion = null,
        buildFingerprint = "",
    )

    private val descriptor = CapabilityDescriptor(
        id = "test::PAUSE_PD_DURING_GAMING",
        type = CapabilityType.PAUSE_PD_DURING_GAMING,
        availableMethods = listOf(ControlMethod.OFFICIAL_GUIDANCE, ControlMethod.WRITE_SETTINGS_KEY),
        evidence = Evidence.PROJECT_VERIFIED,
    )

    private fun fakeStrategy(method: ControlMethod, available: Boolean) = object : ControlStrategy {
        override val method: ControlMethod = method
        override suspend fun isAvailable(p: DeviceProfile, d: CapabilityDescriptor) = available
        override suspend fun getCurrentState(d: CapabilityDescriptor) = ControlState.Unknown
        override suspend fun setEnabled(d: CapabilityDescriptor, e: Boolean): ControlResult =
            ControlResult.Success
        override suspend fun reset(d: CapabilityDescriptor): ControlResult = ControlResult.Success
    }

    @Test
    fun `picks first available strategy in descriptor order`() = runTest {
        val orchestrator = ControlOrchestrator(
            mapOf(
                ControlMethod.OFFICIAL_GUIDANCE to fakeStrategy(ControlMethod.OFFICIAL_GUIDANCE, true),
                ControlMethod.WRITE_SETTINGS_KEY to fakeStrategy(ControlMethod.WRITE_SETTINGS_KEY, true),
            )
        )
        val pick = orchestrator.pickStrategy(profile, descriptor)
        assertThat(pick?.method).isEqualTo(ControlMethod.OFFICIAL_GUIDANCE)
    }

    @Test
    fun `falls through to second strategy when first unavailable`() = runTest {
        val orchestrator = ControlOrchestrator(
            mapOf(
                ControlMethod.OFFICIAL_GUIDANCE to fakeStrategy(ControlMethod.OFFICIAL_GUIDANCE, false),
                ControlMethod.WRITE_SETTINGS_KEY to fakeStrategy(ControlMethod.WRITE_SETTINGS_KEY, true),
            )
        )
        val pick = orchestrator.pickStrategy(profile, descriptor)
        assertThat(pick?.method).isEqualTo(ControlMethod.WRITE_SETTINGS_KEY)
    }

    @Test
    fun `userPreference overrides descriptor order`() = runTest {
        val orchestrator = ControlOrchestrator(
            mapOf(
                ControlMethod.OFFICIAL_GUIDANCE to fakeStrategy(ControlMethod.OFFICIAL_GUIDANCE, true),
                ControlMethod.WRITE_SETTINGS_KEY to fakeStrategy(ControlMethod.WRITE_SETTINGS_KEY, true),
            )
        )
        val pick = orchestrator.pickStrategy(
            profile,
            descriptor,
            userPreference = ControlMethod.WRITE_SETTINGS_KEY,
        )
        assertThat(pick?.method).isEqualTo(ControlMethod.WRITE_SETTINGS_KEY)
    }

    @Test
    fun `returns null when no strategy available`() = runTest {
        val orchestrator = ControlOrchestrator(
            mapOf(
                ControlMethod.OFFICIAL_GUIDANCE to fakeStrategy(ControlMethod.OFFICIAL_GUIDANCE, false),
            )
        )
        assertThat(orchestrator.pickStrategy(profile, descriptor)).isNull()
    }

    @Test
    fun `availableMethods reflects registered strategies`() {
        val orchestrator = ControlOrchestrator(
            mapOf(
                ControlMethod.OFFICIAL_GUIDANCE to fakeStrategy(ControlMethod.OFFICIAL_GUIDANCE, true),
            )
        )
        assertThat(orchestrator.availableMethods())
            .containsExactly(ControlMethod.OFFICIAL_GUIDANCE)
    }
}
