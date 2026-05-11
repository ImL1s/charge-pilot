package com.chargepilot.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ManufacturerTest {
    @Test
    fun `samsung lowercase maps to SAMSUNG`() {
        assertThat(Manufacturer.fromBuild("samsung")).isEqualTo(Manufacturer.SAMSUNG)
    }

    @Test
    fun `Samsung mixed case still maps to SAMSUNG`() {
        assertThat(Manufacturer.fromBuild("Samsung")).isEqualTo(Manufacturer.SAMSUNG)
    }

    @Test
    fun `redmi and poco both map to XIAOMI family`() {
        assertThat(Manufacturer.fromBuild("Redmi")).isEqualTo(Manufacturer.XIAOMI)
        assertThat(Manufacturer.fromBuild("POCO")).isEqualTo(Manufacturer.XIAOMI)
    }

    @Test
    fun `unknown vendor maps to OTHER not crashing`() {
        assertThat(Manufacturer.fromBuild("Fairphone")).isEqualTo(Manufacturer.OTHER)
        assertThat(Manufacturer.fromBuild(null)).isEqualTo(Manufacturer.OTHER)
        assertThat(Manufacturer.fromBuild("")).isEqualTo(Manufacturer.OTHER)
    }

    @Test
    fun `hihonor and HONOR both map to HONOR`() {
        assertThat(Manufacturer.fromBuild("HONOR")).isEqualTo(Manufacturer.HONOR)
        assertThat(Manufacturer.fromBuild("hihonor")).isEqualTo(Manufacturer.HONOR)
    }
}
