package com.chargepilot.core.control.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import com.chargepilot.core.control.ControlStrategy
import com.chargepilot.core.control.SetupNavigationResult
import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlResult
import com.chargepilot.core.model.ControlSetupStage
import com.chargepilot.core.model.ControlSetupStatus
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.DeviceProfile
import com.chargepilot.core.model.FailureReason
import com.chargepilot.core.model.SettingsKey
import com.chargepilot.core.model.WritableSettingsKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full-flavor Shizuku strategy. It runs a small UserService with Shizuku's ADB/root
 * identity and delegates Samsung's private `settings system pass_through` read/write
 * through the platform `settings` command. This intentionally does not exist in the
 * Play flavor.
 */
@Singleton
class ShizukuStrategy @Inject constructor(
    @ApplicationContext private val context: Context,
) : ControlStrategy {
    override val method: ControlMethod = ControlMethod.SHIZUKU_RPC

    @Volatile
    private var cachedService: IShizukuSettingsService? = null

    @Volatile
    private var cachedConnection: ServiceConnection? = null

    private val bindMutex = Mutex()

    override suspend fun isAvailable(
        profile: DeviceProfile,
        descriptor: CapabilityDescriptor,
    ): Boolean = setupStatus(profile, descriptor).stage == ControlSetupStage.READY

    override suspend fun setupStatus(
        profile: DeviceProfile,
        descriptor: CapabilityDescriptor,
    ): ControlSetupStatus {
        if (!descriptorAllowsShizuku(descriptor)) {
            return ControlSetupStatus(method, ControlSetupStage.UNSUPPORTED)
        }
        if (!isShizukuInstalled()) {
            return ControlSetupStatus(method, ControlSetupStage.NOT_INSTALLED)
        }
        if (!isBinderRunning()) {
            return ControlSetupStatus(method, ControlSetupStage.INSTALLED_NOT_RUNNING)
        }
        return if (permissionFailure(requestIfNeeded = false) == null) {
            ControlSetupStatus(method, ControlSetupStage.READY)
        } else {
            ControlSetupStatus(method, ControlSetupStage.PERMISSION_REQUIRED)
        }
    }

    override suspend fun requestSetupAuthorization(descriptor: CapabilityDescriptor): SetupNavigationResult {
        if (!descriptorAllowsShizuku(descriptor)) return SetupNavigationResult.Unsupported
        if (!isShizukuInstalled() || !isBinderRunning()) return SetupNavigationResult.Failed
        return try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                SetupNavigationResult.AlreadyReady
            } else {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                SetupNavigationResult.RequestedPermission
            }
        } catch (t: Throwable) {
            Timber.w(t, "Shizuku permission request failed")
            SetupNavigationResult.Failed
        }
    }

    override suspend fun getCurrentState(descriptor: CapabilityDescriptor): ControlState {
        val key = descriptor.settingsKey?.takeIf { descriptorAllowsShizuku(descriptor) }
            ?: return ControlState.Unknown
        if (permissionFailure(requestIfNeeded = false) != null) return ControlState.Unknown
        val service = bindService() ?: return ControlState.Unknown
        return try {
            when (service.readSystemSetting(key.key).trim()) {
                "", "null" -> ControlState.Unknown
                "0" -> ControlState.Inactive
                else -> ControlState.Engaged
            }
        } catch (t: Throwable) {
            Timber.w(t, "Shizuku read failed for key=${key.key}")
            cachedService = null
            ControlState.Unknown
        }
    }

    override suspend fun setEnabled(
        descriptor: CapabilityDescriptor,
        enabled: Boolean,
    ): ControlResult {
        val key = descriptor.settingsKey?.takeIf { descriptorAllowsShizuku(descriptor) }
            ?: return ControlResult.Failed(FailureReason.UNSUPPORTED_DEVICE)
        permissionFailure(requestIfNeeded = true)?.let { reason ->
            return ControlResult.Failed(reason)
        }
        val service = bindService() ?: return ControlResult.Failed(FailureReason.SHIZUKU_NOT_RUNNING)
        return try {
            val desired = if (enabled) "1" else "0"
            service.writeSystemSetting(key.key, desired)
            // Read-after-write so history does not log Success when the key did not change.
            val readBack = service.readSystemSetting(key.key).trim()
            val matches = when (readBack) {
                "", "null" -> false
                "0" -> !enabled
                else -> enabled
            }
            if (matches) {
                ControlResult.Success
            } else {
                ControlResult.Failed(FailureReason.KEY_NOT_WRITABLE)
            }
        } catch (t: RemoteException) {
            Timber.w(t, "Shizuku remote write failed for key=${key.key}")
            cachedService = null
            ControlResult.Failed(FailureReason.KEY_NOT_WRITABLE)
        } catch (t: Throwable) {
            Timber.w(t, "Shizuku write failed for key=${key.key}")
            cachedService = null
            ControlResult.Failed(FailureReason.UNKNOWN)
        }
    }

    override suspend fun reset(descriptor: CapabilityDescriptor): ControlResult =
        setEnabled(descriptor, enabled = false)

    private fun descriptorAllowsShizuku(descriptor: CapabilityDescriptor): Boolean {
        if (ControlMethod.SHIZUKU_RPC !in descriptor.availableMethods) return false
        val key: SettingsKey = descriptor.settingsKey ?: return false
        if (key.namespace != "system") return false
        if (key.type != "int") return false
        if (!SAFE_SETTINGS_KEY.matches(key.key)) return false
        if (!WritableSettingsKeys.isAllowedSystemInt(key.key)) return false
        return true
    }

    private fun permissionFailure(requestIfNeeded: Boolean): FailureReason? {
        if (!isBinderRunning()) return FailureReason.SHIZUKU_NOT_RUNNING
        return try {
            if (Shizuku.isPreV11()) return FailureReason.SHIZUKU_NOT_RUNNING
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                null
            } else {
                if (requestIfNeeded && !Shizuku.shouldShowRequestPermissionRationale()) {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                }
                FailureReason.SHIZUKU_PERMISSION_DENIED
            }
        } catch (t: Throwable) {
            Timber.w(t, "Shizuku permission check failed")
            FailureReason.SHIZUKU_NOT_RUNNING
        }
    }

    private fun isBinderRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    private fun isShizukuInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: Throwable) {
        false
    }

    private suspend fun bindService(): IShizukuSettingsService? = bindMutex.withLock {
        cachedService?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        if (permissionFailure(requestIfNeeded = false) != null) return null

        val deferred = CompletableDeferred<IShizukuSettingsService?>()
        val args = userServiceArgs()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
                val service = binder
                    ?.takeIf { it.pingBinder() }
                    ?.let(IShizukuSettingsService.Stub::asInterface)
                cachedService = service
                deferred.complete(service)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                cachedService = null
                cachedConnection = null
            }
        }
        cachedConnection = connection

        return try {
            Shizuku.bindUserService(args, connection)
            val service = withTimeoutOrNull(BIND_TIMEOUT_MS) { deferred.await() }
            if (service == null) {
                // Timeout or null binder: unbind so we do not leave zombie UserServices.
                unbindQuietly(args, connection)
                cachedService = null
                cachedConnection = null
                null
            } else {
                service
            }
        } catch (t: Throwable) {
            Timber.w(t, "Shizuku UserService bind failed")
            unbindQuietly(args, connection)
            cachedService = null
            cachedConnection = null
            null
        }
    }

    private fun unbindQuietly(args: Shizuku.UserServiceArgs, connection: ServiceConnection) {
        try {
            Shizuku.unbindUserService(args, connection, true)
        } catch (t: Throwable) {
            Timber.d(t, "Shizuku unbindUserService failed")
        }
    }

    private fun userServiceArgs(): Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShizukuSettingsUserService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix("chargepilot_settings")
            .debuggable(false)
            .version(packageVersionCode())

    private fun packageVersionCode(): Int = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else info.versionCode
    } catch (_: Throwable) {
        1
    }

    private companion object {
        private const val BIND_TIMEOUT_MS = 5_000L
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 24051
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private val SAFE_SETTINGS_KEY = Regex("[A-Za-z0-9_.:-]+")
    }
}
