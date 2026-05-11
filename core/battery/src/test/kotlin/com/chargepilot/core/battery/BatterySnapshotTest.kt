package com.chargepilot.core.battery

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BatterySnapshotTest {

    @Test
    fun `Samsung HVC charger is treated as PD connected`() {
        val snapshot = snapshot(plugged = 2, hvc = true)

        assertThat(snapshot.isPdChargerConnected()).isTrue()
    }

    @Test
    fun `Samsung online 38 charger is treated as PD connected`() {
        val snapshot = snapshot(plugged = 1, online = 38)

        assertThat(snapshot.isPdChargerConnected()).isTrue()
    }

    @Test
    fun `ordinary USB charger is not treated as PD connected`() {
        val snapshot = snapshot(plugged = 2, online = 5, chargerType = 0, hvc = false)

        assertThat(snapshot.isPdChargerConnected()).isFalse()
    }

    @Test
    fun `battery level computes from level and scale`() {
        val snapshot = snapshot(level = 45, scale = 90)

        assertThat(snapshot.levelPercent()).isEqualTo(50)
    }

    private fun snapshot(
        plugged: Int = 1,
        level: Int = 100,
        scale: Int = 100,
        maxChargingCurrentMicroAmps: Int = 0,
        hvc: Boolean = false,
        chargerType: Int = -1,
        online: Int = -1,
    ): BatterySnapshot = BatterySnapshot(
        plugged = plugged,
        level = level,
        scale = scale,
        maxChargingCurrentMicroAmps = maxChargingCurrentMicroAmps,
        hvc = hvc,
        chargerType = chargerType,
        online = online,
    )
}
