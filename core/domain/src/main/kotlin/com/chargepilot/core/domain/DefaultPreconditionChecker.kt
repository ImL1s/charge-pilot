package com.chargepilot.core.domain

import com.chargepilot.core.battery.BatteryProvider
import com.chargepilot.core.foreground.ForegroundDetector
import com.chargepilot.core.model.Precondition
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [PreconditionChecker]: evaluates each precondition against the device's current
 * runtime by asking [BatteryProvider] and [ForegroundDetector] in turn.
 *
 * Both of those providers are stubs in the current scaffold (they always report
 * "unmet"). That is intentional: the checker is honest about ignorance — preconditions it
 * cannot evaluate are reported as unmet, never silently treated as met.
 */
@Singleton
class DefaultPreconditionChecker @Inject constructor(
    private val battery: BatteryProvider,
    private val foreground: ForegroundDetector,
) : PreconditionChecker {
    override suspend fun check(preconditions: List<Precondition>): Evaluation {
        val unmet = preconditions.filterNot(::isMet)
        return Evaluation(unmet = unmet)
    }

    private fun isMet(precondition: Precondition): Boolean = when (precondition) {
        Precondition.PdChargerPresent -> battery.isPdChargerConnected()
        is Precondition.BatteryLevelAbove -> {
            val level = battery.batteryLevelPercent() ?: return false
            level > precondition.percent
        }
        is Precondition.GameInForeground -> {
            val pkg = foreground.currentForegroundPackage() ?: return false
            precondition.knownGames?.contains(pkg) ?: true
        }
    }
}
