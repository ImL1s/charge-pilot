package com.chargepilot.core.battery

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub. Real impl wraps `BatteryManager` and a sticky `ACTION_BATTERY_CHANGED` receiver
 * to expose a Flow&lt;BatterySnapshot&gt;.
 */
@Singleton
class BatteryProvider @Inject constructor() {
    fun isPdChargerConnected(): Boolean = false
    fun batteryLevelPercent(): Int? = null
}
