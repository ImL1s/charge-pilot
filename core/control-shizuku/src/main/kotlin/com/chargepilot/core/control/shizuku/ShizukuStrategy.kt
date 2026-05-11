package com.chargepilot.core.control.shizuku

import com.chargepilot.core.control.ControlStrategy
import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlResult
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.DeviceProfile
import com.chargepilot.core.model.FailureReason
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Skeleton Shizuku-backed strategy. A future task fleshes out the real implementation
 * using `dev.rikka.shizuku:api`. For now it reports unavailable so the orchestrator
 * never picks it.
 */
@Singleton
class ShizukuStrategy @Inject constructor() : ControlStrategy {
    override val method: ControlMethod = ControlMethod.SHIZUKU_RPC

    override suspend fun isAvailable(
        profile: DeviceProfile,
        descriptor: CapabilityDescriptor,
    ): Boolean = false

    override suspend fun getCurrentState(descriptor: CapabilityDescriptor): ControlState =
        ControlState.Unknown

    override suspend fun setEnabled(
        descriptor: CapabilityDescriptor,
        enabled: Boolean,
    ): ControlResult = ControlResult.Failed(FailureReason.SHIZUKU_NOT_RUNNING)

    override suspend fun reset(descriptor: CapabilityDescriptor): ControlResult =
        ControlResult.Failed(FailureReason.SHIZUKU_NOT_RUNNING)
}
