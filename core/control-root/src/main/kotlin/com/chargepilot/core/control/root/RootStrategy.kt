package com.chargepilot.core.control.root

import com.chargepilot.core.control.ControlStrategy
import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlResult
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.DeviceProfile
import com.chargepilot.core.model.FailureReason
import javax.inject.Inject
import javax.inject.Singleton

/** Skeleton root strategy; real implementation will reintroduce libsu when shipped. */
@Singleton
class RootStrategy @Inject constructor() : ControlStrategy {
    override val method: ControlMethod = ControlMethod.ROOT_SHELL

    override suspend fun isAvailable(
        profile: DeviceProfile,
        descriptor: CapabilityDescriptor,
    ): Boolean = false

    override suspend fun getCurrentState(descriptor: CapabilityDescriptor): ControlState =
        ControlState.Unknown

    override suspend fun setEnabled(
        descriptor: CapabilityDescriptor,
        enabled: Boolean,
    ): ControlResult = ControlResult.Failed(FailureReason.ROOT_DENIED)

    override suspend fun reset(descriptor: CapabilityDescriptor): ControlResult =
        ControlResult.Failed(FailureReason.ROOT_DENIED)
}
