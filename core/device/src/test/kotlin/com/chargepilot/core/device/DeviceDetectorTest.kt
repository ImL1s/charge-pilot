package com.chargepilot.core.device

import com.chargepilot.core.model.Manufacturer
import com.chargepilot.core.model.RomFlavor
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Pure unit tests for the Samsung ROM detection fallback chain.
 * Build.MANUFACTURER cannot be mocked without Robolectric, so we test the private
 * mapping helpers indirectly by driving a fake [SystemPropertyReader] and inspecting
 * the produced [com.chargepilot.core.model.RomVersion].
 */
class DeviceDetectorTest {

    private class FakeReader(private val props: Map<String, String>) : SystemPropertyReader {
        override fun read(name: String): String? = props[name]?.takeIf { it.isNotEmpty() }
    }

    @Test
    fun `samsung prefers oneui property when present`() {
        val reader = FakeReader(
            mapOf(
                "ro.build.version.oneui" to "40101",
                "ro.build.version.sep" to "140500",
            ),
        )
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.SAMSUNG)
        assertThat(rom).isNotNull()
        assertThat(rom!!.flavor).isEqualTo(RomFlavor.ONE_UI)
        assertThat(rom.version).isEqualTo("4.1")
        assertThat(rom.raw).isEqualTo("40101")
        assertThat(rom.displayLabel).isNull()
    }

    @Test
    fun `samsung falls back to sep with numeric version and friendly displayLabel`() {
        val reader = FakeReader(mapOf("ro.build.version.sep" to "110500"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.SAMSUNG)
        assertThat(rom).isNotNull()
        assertThat(rom!!.flavor).isEqualTo(RomFlavor.ONE_UI)
        assertThat(rom.raw).isEqualTo("110500")
        // numeric version must stay dot-parseable so CapabilityRegistry can compare it
        assertThat(rom.version).isEqualTo("11.05")
        assertThat(rom.displayLabel).isEqualTo("OneUI 2.x · SEP 11.05")
    }

    @Test
    fun `samsung falls back to sem with displayLabel`() {
        val reader = FakeReader(mapOf("ro.build.version.sem" to "2903"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.SAMSUNG)
        assertThat(rom).isNotNull()
        assertThat(rom!!.flavor).isEqualTo(RomFlavor.ONE_UI)
        assertThat(rom.raw).isEqualTo("2903")
        assertThat(rom.displayLabel).contains("SEM 2903")
    }

    @Test
    fun `samsung returns null when no version property is present`() {
        val reader = FakeReader(emptyMap())
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.SAMSUNG)
        assertThat(rom).isNull()
    }

    @Test
    fun `unknown sep major still reports numeric version and SEP displayLabel`() {
        val reader = FakeReader(mapOf("ro.build.version.sep" to "990000"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.SAMSUNG)
        assertThat(rom).isNotNull()
        assertThat(rom!!.flavor).isEqualTo(RomFlavor.ONE_UI)
        assertThat(rom.version).isEqualTo("99.00")
        assertThat(rom.displayLabel).isEqualTo("SEP 99.00")
    }

    @Test
    fun `xiaomi strips V prefix from miui name`() {
        val reader = FakeReader(mapOf("ro.miui.ui.version.name" to "V14"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.XIAOMI)
        assertThat(rom).isNotNull()
        assertThat(rom!!.flavor).isEqualTo(RomFlavor.HYPER_OS)
        assertThat(rom.version).isEqualTo("14")
    }

    @Test
    fun `other manufacturer returns null`() {
        val reader = FakeReader(mapOf("anything" to "x"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.OTHER)
        assertThat(rom).isNull()
    }
}
