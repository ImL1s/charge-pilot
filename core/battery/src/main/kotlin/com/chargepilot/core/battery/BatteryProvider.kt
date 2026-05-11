package com.chargepilot.core.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the current sticky battery broadcast and exposes the runtime checks used by OEM rules. */
@Singleton
class BatteryProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isPdChargerConnected(): Boolean = batterySnapshot()?.isPdChargerConnected() ?: false

    fun batteryLevelPercent(): Int? = batterySnapshot()?.levelPercent()

    private fun batterySnapshot(): BatterySnapshot? = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.toSnapshot()
    }.getOrNull()
}

internal data class BatterySnapshot(
    val plugged: Int,
    val level: Int,
    val scale: Int,
    val maxChargingCurrentMicroAmps: Int,
    val hvc: Boolean,
    val chargerType: Int,
    val online: Int,
) {
    fun levelPercent(): Int? {
        if (level < 0 || scale <= 0) return null
        return (level * 100) / scale
    }

    fun isPdChargerConnected(): Boolean {
        if (plugged == 0) return false
        return hvc ||
            chargerType == SAMSUNG_PD_CHARGER_TYPE ||
            online == SAMSUNG_PD_ONLINE_VALUE ||
            maxChargingCurrentMicroAmps >= PD_CURRENT_THRESHOLD_MICRO_AMPS
    }

    private companion object {
        const val PD_CURRENT_THRESHOLD_MICRO_AMPS = 1_500_000
        const val SAMSUNG_PD_CHARGER_TYPE = 3
        const val SAMSUNG_PD_ONLINE_VALUE = 38
    }
}

private fun Intent.toSnapshot(): BatterySnapshot = BatterySnapshot(
    plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
    level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
    scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1),
    maxChargingCurrentMicroAmps = getIntExtra("max_charging_current", 0),
    hvc = getBooleanExtra("hvc", false),
    chargerType = getIntExtra("charger_type", -1),
    online = getIntExtra("online", -1),
)
