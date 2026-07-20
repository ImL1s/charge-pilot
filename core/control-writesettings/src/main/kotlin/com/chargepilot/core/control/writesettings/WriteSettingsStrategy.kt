package com.chargepilot.core.control.writesettings

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.chargepilot.core.control.ControlStrategy
import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlResult
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.DeviceProfile
import com.chargepilot.core.model.FailureReason
import com.chargepilot.core.model.SettingsKey
import com.chargepilot.core.model.WritableSettingsKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes a documented (but typically *unofficial third-party*) system-settings key, e.g.
 * Samsung's `pass_through` for "Pause USB PD charging when gaming".
 *
 * The strategy never assumes a successful write means the OEM has activated the feature:
 * the descriptor's preconditions (PD charger, battery level, foreground game) must also
 * hold. It is the responsibility of the caller (typically `:feature:home:impl`) to combine
 * this strategy's [ControlState.Inactive] / `Active`-without-conditions reading with a
 * preconditions check before showing the user "Active".
 *
 * Requires [Settings.System.canWrite] returning true; otherwise reports
 * [FailureReason.SPECIAL_ACCESS_NOT_GRANTED] and does NOT attempt a write.
 */
@Singleton
class WriteSettingsStrategy @Inject constructor(
    @ApplicationContext private val context: Context,
) : ControlStrategy {
    override val method: ControlMethod = ControlMethod.WRITE_SETTINGS_KEY

    override suspend fun isAvailable(
        profile: DeviceProfile,
        descriptor: CapabilityDescriptor,
    ): Boolean {
        val settingsKey = descriptor.settingsKey ?: return false
        // Don't require canWrite() here — we want to surface the capability to the user
        // and prompt them for the special access. canWrite() is checked at write time.
        return ControlMethod.WRITE_SETTINGS_KEY in descriptor.availableMethods &&
                settingsKey.namespace == "system"
    }

    override suspend fun getCurrentState(descriptor: CapabilityDescriptor): ControlState {
        if (!descriptorAllowsWrite(descriptor)) return ControlState.Unknown
        val key = descriptor.settingsKey ?: return ControlState.Unknown
        // Engaged (NOT Active) is intentional: this strategy cannot tell whether the
        // OEM preconditions (charger, battery, game-in-foreground) are met. The
        // HomeViewModel combines this with a PreconditionChecker before deciding
        // whether to display Active or PendingConditions.
        return when (readInt(key)) {
            null -> ControlState.Unknown
            0 -> ControlState.Inactive
            else -> ControlState.Engaged
        }
    }

    override suspend fun setEnabled(
        descriptor: CapabilityDescriptor,
        enabled: Boolean,
    ): ControlResult {
        // Defense-in-depth: even if a future caller bypasses the orchestrator's
        // pickStrategy guard, this strategy MUST refuse to write a key that the
        // descriptor's allow-list doesn't permit.
        if (!descriptorAllowsWrite(descriptor)) {
            return ControlResult.Failed(FailureReason.UNSUPPORTED_DEVICE)
        }
        val key = descriptor.settingsKey
            ?: return ControlResult.Failed(FailureReason.UNSUPPORTED_DEVICE)
        if (!Settings.System.canWrite(context)) {
            return ControlResult.Failed(FailureReason.SPECIAL_ACCESS_NOT_GRANTED)
        }
        return try {
            val desired = if (enabled) 1 else 0
            val ok = Settings.System.putInt(context.contentResolver, key.key, desired)
            if (!ok) return ControlResult.Failed(FailureReason.KEY_NOT_WRITABLE)
            // Read-after-write: putInt can return true while the OEM leaves the key unchanged.
            val readBack = readInt(key)
            val expectedEngaged = enabled
            val matches = when (readBack) {
                null -> false
                0 -> !expectedEngaged
                else -> expectedEngaged
            }
            if (matches) {
                ControlResult.Success
            } else {
                ControlResult.Failed(FailureReason.KEY_NOT_WRITABLE)
            }
        } catch (t: SecurityException) {
            Timber.w(t, "WriteSettings security exception for key=${key.key}")
            ControlResult.Failed(FailureReason.SPECIAL_ACCESS_NOT_GRANTED)
        } catch (t: Throwable) {
            Timber.w(t, "WriteSettings unexpected failure for key=${key.key}")
            ControlResult.Failed(FailureReason.UNKNOWN)
        }
    }

    private fun descriptorAllowsWrite(descriptor: CapabilityDescriptor): Boolean {
        if (ControlMethod.WRITE_SETTINGS_KEY !in descriptor.availableMethods) return false
        val key = descriptor.settingsKey ?: return false
        if (key.namespace != "system") return false
        if (key.type != "int") return false
        if (!WritableSettingsKeys.isAllowedSystemInt(key.key)) return false
        return true
    }

    override suspend fun reset(descriptor: CapabilityDescriptor): ControlResult =
        setEnabled(descriptor, enabled = false)

    /** Build the intent the UI should fire to walk the user to the special-access page. */
    fun buildSpecialAccessIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun readInt(key: SettingsKey): Int? {
        val resolver: ContentResolver = context.contentResolver
        return try {
            Settings.System.getInt(resolver, key.key)
        } catch (_: Settings.SettingNotFoundException) {
            null
        } catch (t: Throwable) {
            Timber.d(t, "WriteSettings.readInt failed for key=${key.key}")
            null
        }
    }
}
