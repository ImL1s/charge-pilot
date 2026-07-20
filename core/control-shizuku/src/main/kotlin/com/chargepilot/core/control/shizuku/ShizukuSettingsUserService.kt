package com.chargepilot.core.control.shizuku

import android.os.RemoteException
import android.system.Os
import com.chargepilot.core.model.WritableSettingsKeys
import java.util.concurrent.TimeUnit

/**
 * UserService instantiated by Shizuku. This process runs as shell (ADB mode) or root
 * (root mode), not as the normal app UID.
 */
class ShizukuSettingsUserService : IShizukuSettingsService.Stub() {

    override fun readSystemSetting(key: String): String =
        runSettingsCommand("get", "system", requireSafeKey(key)).trim()

    override fun writeSystemSetting(key: String, value: String) {
        if (value != "0" && value != "1") {
            throw RemoteException("Only integer toggle values 0/1 are supported")
        }
        runSettingsCommand("put", "system", requireSafeKey(key), value)
    }

    override fun identity(): String = "pid=${Os.getpid()}, uid=${Os.getuid()}"

    override fun destroy() {
        System.exit(0)
    }

    private fun requireSafeKey(key: String): String {
        if (!SAFE_SETTINGS_KEY.matches(key)) {
            throw RemoteException("Unsafe settings key")
        }
        if (!WritableSettingsKeys.isAllowedSystemInt(key)) {
            throw RemoteException("Settings key not allowlisted")
        }
        return key
    }

    private fun runSettingsCommand(vararg args: String): String {
        val command = listOf("/system/bin/settings") + args
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            throw RemoteException("settings command timed out")
        }
        val exitCode = process.exitValue()
        if (exitCode != 0) {
            throw RemoteException("settings command failed ($exitCode): ${output.trim()}")
        }
        return output
    }

    private companion object {
        private const val COMMAND_TIMEOUT_SECONDS = 5L
        private val SAFE_SETTINGS_KEY = Regex("[A-Za-z0-9_.:-]+")
    }
}
