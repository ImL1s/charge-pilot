package com.chargepilot.core.capability

import android.content.Context
import com.chargepilot.core.common.IoDispatcher
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.WritableSettingsKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the capability registry from the bundled `assets/capabilities-v1.json` only.
 * OTA / remote cache is not implemented; keep this loader offline-safe.
 */
interface CapabilityRegistryLoader {
    suspend fun load(): CapabilityRegistrySnapshot
}

@Singleton
class BundledCapabilityRegistryLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CapabilityRegistryLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun load(): CapabilityRegistrySnapshot = withContext(ioDispatcher) {
        val raw = context.assets.open(BUNDLED_ASSET).bufferedReader().use { it.readText() }
        val snapshot = json.decodeFromString(CapabilityRegistrySnapshot.serializer(), raw)
        validate(snapshot)
        snapshot
    }

    private fun validate(snapshot: CapabilityRegistrySnapshot) {
        val seenIds = mutableSetOf<String>()
        snapshot.rules.forEach { rule ->
            rule.capabilities.forEach { descriptor ->
                val expectedId = "${rule.id}::${descriptor.type.name}"
                require(descriptor.id == expectedId) {
                    "Invalid descriptor id '${descriptor.id}'; expected '$expectedId'"
                }
                require(seenIds.add(descriptor.id)) {
                    "Duplicate descriptor id '${descriptor.id}'"
                }
                descriptor.settingsKey?.let { key ->
                    require(key.namespace == "system") {
                        "settingsKey for '${descriptor.id}' must be in 'system' namespace, got '${key.namespace}'"
                    }
                    val writeCapable = ControlMethod.WRITE_SETTINGS_KEY in descriptor.availableMethods ||
                        ControlMethod.SHIZUKU_RPC in descriptor.availableMethods ||
                        ControlMethod.ROOT_SHELL in descriptor.availableMethods
                    if (writeCapable) {
                        require(WritableSettingsKeys.isAllowedSystemInt(key.key)) {
                            "settingsKey '${key.key}' for '${descriptor.id}' is not in WritableSettingsKeys allowlist"
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val BUNDLED_ASSET = "capabilities-v1.json"
    }
}
