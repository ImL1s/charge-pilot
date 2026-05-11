package com.chargepilot.core.capability

import com.chargepilot.core.model.CapabilityType
import com.chargepilot.core.model.DeviceProfile
import com.chargepilot.core.model.Manufacturer
import com.chargepilot.core.model.RomFlavor
import com.chargepilot.core.model.RomVersion
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CapabilityRegistryTest {

    private val samsungS24 = DeviceProfile(
        manufacturer = Manufacturer.SAMSUNG,
        model = "SM-S921B",
        codename = "e1q",
        androidApi = 35,
        androidVersion = "15",
        romVersion = RomVersion(RomFlavor.ONE_UI, "7.0", "70000"),
        buildFingerprint = "samsung/e1q/...",
    )

    private val pixel9 = DeviceProfile(
        manufacturer = Manufacturer.GOOGLE,
        model = "Pixel 9",
        codename = "tokay",
        androidApi = 35,
        androidVersion = "15",
        romVersion = RomVersion(RomFlavor.AOSP, "15", "15"),
        buildFingerprint = "google/tokay/...",
    )

    private val pocoF7 = DeviceProfile(
        manufacturer = Manufacturer.XIAOMI,
        model = "2503CXC0",
        codename = "munch",
        androidApi = 36,
        androidVersion = "16",
        romVersion = RomVersion(RomFlavor.HYPER_OS, "2.0", "V2.0"),
        buildFingerprint = "xiaomi/munch/...",
    )

    private fun registry(rules: List<CapabilityRule>): CapabilityRegistry {
        val loader = object : CapabilityRegistryLoader {
            override suspend fun load() = CapabilityRegistrySnapshot(
                schemaVersion = 1,
                lastUpdated = "2026-05-11",
                rules = rules,
            )
        }
        return CapabilityRegistry(loader)
    }

    @Test
    fun `samsung S24 matches galaxy-s2x rule and resolves PAUSE_PD descriptor`() = runTest {
        val rule = CapabilityRule(
            id = "samsung-galaxy-s2x-pause-pd",
            matchers = Matchers(
                manufacturer = "SAMSUNG",
                modelRegex = "SM-S(91|92|93|94)[0-9].*",
                minRomVersion = RomVersionRequirement("ONE_UI", "6.0"),
            ),
            capabilities = listOf(
                com.chargepilot.core.model.CapabilityDescriptor(
                    id = "samsung-galaxy-s2x-pause-pd::PAUSE_PD_DURING_GAMING",
                    type = CapabilityType.PAUSE_PD_DURING_GAMING,
                    availableMethods = emptyList(),
                    evidence = com.chargepilot.core.model.Evidence.PROJECT_VERIFIED,
                ),
            ),
        )
        val descriptors = registry(listOf(rule)).resolve(samsungS24)
        assertThat(descriptors).hasSize(1)
        assertThat(descriptors[0].type).isEqualTo(CapabilityType.PAUSE_PD_DURING_GAMING)
    }

    @Test
    fun `pixel does not match samsung rule`() = runTest {
        val rule = CapabilityRule(
            id = "samsung-only",
            matchers = Matchers(manufacturer = "SAMSUNG"),
            capabilities = emptyList(),
        )
        assertThat(registry(listOf(rule)).resolve(pixel9)).isEmpty()
    }

    @Test
    fun `One UI 5_x is below minimum required 6_0 and does not match`() = runTest {
        val olderS24 = samsungS24.copy(
            romVersion = RomVersion(RomFlavor.ONE_UI, "5.1", "50100"),
        )
        val rule = CapabilityRule(
            id = "samsung-min-6",
            matchers = Matchers(
                manufacturer = "SAMSUNG",
                minRomVersion = RomVersionRequirement("ONE_UI", "6.0"),
            ),
            capabilities = emptyList(),
        )
        assertThat(registry(listOf(rule)).resolve(olderS24)).isEmpty()
    }

    @Test
    fun `negative-list rule still resolves but with empty availableMethods`() = runTest {
        val rule = CapabilityRule(
            id = "xiaomi-poco-f7-negative",
            matchers = Matchers(manufacturer = "XIAOMI", modelRegex = "(2503CXC0).*"),
            capabilities = listOf(
                com.chargepilot.core.model.CapabilityDescriptor(
                    id = "xiaomi-poco-f7-negative::BYPASS_CHARGING",
                    type = CapabilityType.BYPASS_CHARGING,
                    availableMethods = emptyList(),
                    evidence = com.chargepilot.core.model.Evidence.OFFICIAL_DOC,
                ),
            ),
        )
        val descriptors = registry(listOf(rule)).resolve(pocoF7)
        assertThat(descriptors).hasSize(1)
        assertThat(descriptors[0].availableMethods).isEmpty()
    }
}
