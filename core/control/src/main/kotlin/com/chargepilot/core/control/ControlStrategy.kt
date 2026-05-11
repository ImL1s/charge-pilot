package com.chargepilot.core.control

import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlResult
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.DeviceProfile

/**
 * Pluggable control strategy. Each strategy is responsible for ONE [ControlMethod] and
 * MUST be safe to call when its method is unavailable on the running device — that case
 * is reported through [isAvailable] and [getCurrentState].
 *
 * Important: a strategy that *succeeded in writing a settings key* MUST still report
 * [ControlState.PendingConditions] when the OEM-defined preconditions are not met. Never
 * conflate "write succeeded" with "feature active".
 */
interface ControlStrategy {
    val method: ControlMethod

    suspend fun isAvailable(
        profile: DeviceProfile,
        descriptor: CapabilityDescriptor,
    ): Boolean

    suspend fun getCurrentState(descriptor: CapabilityDescriptor): ControlState

    suspend fun setEnabled(
        descriptor: CapabilityDescriptor,
        enabled: Boolean,
    ): ControlResult

    suspend fun reset(descriptor: CapabilityDescriptor): ControlResult
}
