package com.chargepilot.core.device

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads Android system properties. Uses reflection on `android.os.SystemProperties` first;
 * falls back to running `getprop <name>` via Runtime.exec on failure.
 *
 * Wrapping so tests can inject a fake without depending on a real device.
 */
interface SystemPropertyReader {
    fun read(name: String): String?
}

@Singleton
class DefaultSystemPropertyReader @Inject constructor() : SystemPropertyReader {
    override fun read(name: String): String? {
        readViaReflection(name)?.let { return it }
        return readViaGetprop(name)
    }

    private fun readViaReflection(name: String): String? = try {
        val clazz = Class.forName("android.os.SystemProperties")
        val getMethod = clazz.getMethod("get", String::class.java)
        (getMethod.invoke(null, name) as? String)?.takeIf { it.isNotEmpty() }
    } catch (t: Throwable) {
        Log.d(TAG, "SystemProperties.get reflection failed for $name", t)
        null
    }

    private fun readViaGetprop(name: String): String? = try {
        val process = ProcessBuilder("getprop", name)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().use { it.readText() }
            .trim()
            .takeIf { it.isNotEmpty() }
    } catch (t: Throwable) {
        Log.d(TAG, "getprop fallback failed for $name", t)
        null
    }

    private companion object {
        const val TAG = "SystemPropertyReader"
    }
}
