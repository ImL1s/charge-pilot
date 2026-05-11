package com.chargepilot.core.data

import com.chargepilot.core.capability.CapabilityRegistry
import com.chargepilot.core.device.DeviceDetector
import com.chargepilot.core.model.CapabilityDescriptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapabilityRepository @Inject constructor(
    private val deviceDetector: DeviceDetector,
    private val capabilityRegistry: CapabilityRegistry,
) {
    suspend fun resolveForCurrentDevice(): List<CapabilityDescriptor> {
        val profile = deviceDetector.detect()
        return capabilityRegistry.resolve(profile)
    }
}
