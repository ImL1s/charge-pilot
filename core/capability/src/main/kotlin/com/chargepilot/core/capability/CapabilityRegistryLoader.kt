package com.chargepilot.core.capability

import android.content.Context
import com.chargepilot.core.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the capability registry. Order: cached OTA copy on disk -> bundled `assets/`.
 * The OTA fetcher is intentionally NOT in this class — keep this small and offline-safe.
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
                }
            }
        }
    }

    private companion object {
        const val BUNDLED_ASSET = "capabilities-v1.json"
    }
}
