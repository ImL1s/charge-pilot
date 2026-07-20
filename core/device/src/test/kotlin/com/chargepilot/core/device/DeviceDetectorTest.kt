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
    fun `samsung falls back to sep with mapped One UI major not SEP major`() {
        val reader = FakeReader(mapOf("ro.build.version.sep" to "110500"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.SAMSUNG)
        assertThat(rom).isNotNull()
        assertThat(rom!!.flavor).isEqualTo(RomFlavor.ONE_UI)
        assertThat(rom.raw).isEqualTo("110500")
        // Comparable version is One UI ~2.x, never SEP major 11 (would false-match One UI 6+).
        assertThat(rom.version).isEqualTo("2.0")
        assertThat(rom.displayLabel).isEqualTo("OneUI 2.x · SEP 11.05")
    }

    @Test
    fun `samsung SEP only must not satisfy One UI 6 minRom when mapped to 2_x`() {
        val reader = FakeReader(mapOf("ro.build.version.sep" to "110500"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.SAMSUNG)!!
        // Hand-rolled compare mirrors CapabilityRegistry.compareVersions.
        fun cmp(a: String, b: String): Int {
            val pa = a.split(".").map { it.toIntOrNull() ?: 0 }
            val pb = b.split(".").map { it.toIntOrNull() ?: 0 }
            val n = maxOf(pa.size, pb.size)
            for (i in 0 until n) {
                val d = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
                if (d != 0) return d
            }
            return 0
        }
        assertThat(cmp(rom.version, "6.0")).isLessThan(0)
    }

    @Test
    fun `samsung oneui 60101 satisfies One UI 6_0`() {
        val reader = FakeReader(mapOf("ro.build.version.oneui" to "60101"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.SAMSUNG)!!
        assertThat(rom.version).isEqualTo("6.1")
    }

    @Test
    fun `samsung falls back to sem with displayLabel and fail-closed version`() {
        val reader = FakeReader(mapOf("ro.build.version.sem" to "2903"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.SAMSUNG)
        assertThat(rom).isNotNull()
        assertThat(rom!!.flavor).isEqualTo(RomFlavor.ONE_UI)
        assertThat(rom.raw).isEqualTo("2903")
        assertThat(rom.version).isEqualTo("0")
        assertThat(rom.displayLabel).contains("SEM 2903")
    }

    @Test
    fun `samsung returns null when no version property is present`() {
        val reader = FakeReader(emptyMap())
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.SAMSUNG)
        assertThat(rom).isNull()
    }

    @Test
    fun `unknown sep major fails closed for compare and keeps SEP displayLabel`() {
        val reader = FakeReader(mapOf("ro.build.version.sep" to "990000"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.SAMSUNG)
        assertThat(rom).isNotNull()
        assertThat(rom!!.flavor).isEqualTo(RomFlavor.ONE_UI)
        assertThat(rom.version).isEqualTo("0")
        assertThat(rom.displayLabel).isEqualTo("SEP 99.00")
    }

    @Test
    fun `oneplus oxygen version is read`() {
        val reader = FakeReader(mapOf("ro.oxygen.version" to "15.0.1"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.ONEPLUS)
        assertThat(rom).isNotNull()
        assertThat(rom!!.flavor).isEqualTo(RomFlavor.OXYGEN_OS)
        assertThat(rom.version).isEqualTo("15.0.1")
    }

    @Test
    fun `honor magic version is read`() {
        val reader = FakeReader(mapOf("ro.build.version.magic" to "9.0"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.HONOR)
        assertThat(rom).isNotNull()
        assertThat(rom!!.flavor).isEqualTo(RomFlavor.MAGIC_OS)
        assertThat(rom.version).isEqualTo("9.0")
    }

    @Test
    fun `huawei emui version is read`() {
        val reader = FakeReader(mapOf("ro.build.version.emui" to "EmotionUI_14.0.0"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.HUAWEI)
        assertThat(rom).isNotNull()
        assertThat(rom!!.flavor).isEqualTo(RomFlavor.EMUI)
        assertThat(rom.version).isEqualTo("EmotionUI_14.0.0")
    }

    @Test
    fun `google uses release property as AOSP flavor`() {
        val reader = FakeReader(mapOf("ro.build.version.release" to "15"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.GOOGLE)
        assertThat(rom).isNotNull()
        assertThat(rom!!.flavor).isEqualTo(RomFlavor.AOSP)
        assertThat(rom.version).isEqualTo("15")
    }

    @Test
    fun `oneui short code below 10000 is raw passthrough`() {
        val reader = FakeReader(mapOf("ro.build.version.oneui" to "601"))
        val rom = DeviceDetector(reader).readRomVersion(Manufacturer.SAMSUNG)
        assertThat(rom!!.version).isEqualTo("601")
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
