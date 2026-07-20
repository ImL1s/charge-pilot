package com.chargepilot.core.capability

import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.DeviceProfile
import com.chargepilot.core.model.Manufacturer
import com.chargepilot.core.model.RomFlavor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a [DeviceProfile] to the list of capabilities the registry knows about for it.
 * The registry is loaded once from the bundled asset and cached in memory; OTA is not
 * implemented (see [CapabilityRegistryLoader]).
 */
@Singleton
class CapabilityRegistry @Inject constructor(
    private val loader: CapabilityRegistryLoader,
) {
    private val mutex = Mutex()
    @Volatile
    private var cached: CapabilityRegistrySnapshot? = null

    suspend fun resolve(profile: DeviceProfile): List<CapabilityDescriptor> {
        val registry = snapshot()
        return registry.rules
            .filter { rule -> matches(rule.matchers, profile) }
            .flatMap { it.capabilities }
    }

    suspend fun rules(): List<CapabilityRule> = snapshot().rules

    /** Clears the in-memory cache (tests / future OTA apply). */
    suspend fun clearCache() {
        mutex.withLock { cached = null }
    }

    private suspend fun snapshot(): CapabilityRegistrySnapshot {
        cached?.let { return it }
        return mutex.withLock {
            cached?.let { return it }
            val loaded = loader.load()
            cached = loaded
            loaded
        }
    }

    private fun matches(matchers: Matchers, profile: DeviceProfile): Boolean {
        // Use safe lookups instead of `valueOf` so a typo or unknown enum value in the
        // registry does not crash `resolve()` and lose all good rules.
        val ruleManufacturer = Manufacturer.entries.firstOrNull { it.name == matchers.manufacturer }
            ?: return false
        if (ruleManufacturer != profile.manufacturer) return false
        matchers.modelRegex?.let { pattern ->
            if (!Regex(pattern).matches(profile.model)) return false
        }
        matchers.minRomVersion?.let { req ->
            val rom = profile.romVersion ?: return false
            val requiredFlavor = RomFlavor.entries.firstOrNull { it.name == req.flavor }
                ?: return false
            if (rom.flavor != requiredFlavor) return false
            if (compareVersions(rom.version, req.version) < 0) return false
        }
        return true
    }

    private fun compareVersions(a: String, b: String): Int {
        val partsA = a.split(".").map { it.toIntOrNull() ?: 0 }
        val partsB = b.split(".").map { it.toIntOrNull() ?: 0 }
        val length = maxOf(partsA.size, partsB.size)
        for (i in 0 until length) {
            val pa = partsA.getOrElse(i) { 0 }
            val pb = partsB.getOrElse(i) { 0 }
            if (pa != pb) return pa.compareTo(pb)
        }
        return 0
    }
}
