package com.chargepilot.core.control

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlResult
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.DeviceProfile
import com.chargepilot.core.model.FailureReason
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The lowest-privilege strategy. It cannot read or write any state — it only knows how to
 * *open the manufacturer's official settings page*. Always available; should be the
 * first option in a descriptor's `availableMethods` list.
 */
@Singleton
class OfficialGuidanceStrategy @Inject constructor(
    @ApplicationContext private val context: Context,
) : ControlStrategy {
    override val method: ControlMethod = ControlMethod.OFFICIAL_GUIDANCE

    override suspend fun isAvailable(
        profile: DeviceProfile,
        descriptor: CapabilityDescriptor,
    ): Boolean = descriptor.officialIntent != null

    override suspend fun getCurrentState(
        descriptor: CapabilityDescriptor,
    ): ControlState = ControlState.Unknown // Official guidance cannot inspect state.

    override suspend fun setEnabled(
        descriptor: CapabilityDescriptor,
        enabled: Boolean,
    ): ControlResult {
        val spec = descriptor.officialIntent
            ?: return ControlResult.Failed(FailureReason.UNSUPPORTED_DEVICE)
        val intent = Intent(spec.action).apply {
            spec.categories.forEach(::addCategory)
            spec.component?.let { component ->
                val explicitComponent = ComponentName.unflattenFromString(component)
                if (explicitComponent != null) {
                    setComponent(explicitComponent)
                } else {
                    setPackage(component)
                }
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Pre-flight: refuse rather than silently fall back to the wrong screen. If the
        // specific OEM action does not resolve, the UI should show the descriptor's
        // `fallbackPath` text as guidance instead of opening the generic Settings screen
        // and falsely declaring "Success".
        if (intent.resolveActivity(context.packageManager) == null) {
            return ControlResult.Failed(FailureReason.UNSUPPORTED_DEVICE)
        }
        return try {
            context.startActivity(intent)
            // Navigated, NOT Success — the user must still complete the change manually
            // in the OEM Settings UI.
            ControlResult.NavigatedToSettings
        } catch (_: Throwable) {
            ControlResult.Failed(FailureReason.UNKNOWN)
        }
    }

    override suspend fun reset(descriptor: CapabilityDescriptor): ControlResult =
        setEnabled(descriptor, enabled = false)
}
