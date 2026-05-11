package com.chargepilot.core.control

import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.DeviceProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Picks the highest-preference [ControlStrategy] that is currently available for a given
 * capability. Order: explicit user preference -> the descriptor's `availableMethods` list.
 * Descriptors should put app-internal control methods before manual/official fallback routes.
 */
@Singleton
class ControlOrchestrator @Inject constructor(
    private val strategies: Map<ControlMethod, @JvmSuppressWildcards ControlStrategy>,
) {
    suspend fun pickStrategy(
        profile: DeviceProfile,
        descriptor: CapabilityDescriptor,
        userPreference: ControlMethod? = null,
    ): ControlStrategy? {
        val ordered = if (userPreference != null) {
            listOf(userPreference) + descriptor.availableMethods.filter { it != userPreference }
        } else {
            descriptor.availableMethods
        }
        return ordered.firstNotNullOfOrNull { method ->
            strategies[method]?.takeIf { it.isAvailable(profile, descriptor) }
        }
    }

    fun availableMethods(): Set<ControlMethod> = strategies.keys

    fun strategyFor(method: ControlMethod): ControlStrategy? = strategies[method]
}
