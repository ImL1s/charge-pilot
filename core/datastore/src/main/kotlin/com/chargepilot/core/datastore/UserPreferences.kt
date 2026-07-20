package com.chargepilot.core.datastore

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * On-device user preferences. Persisted via Preferences DataStore + JSON (same pattern as
 * [OperationHistoryDataSource]); Proto is intentionally not used to avoid extra plugins.
 */
@Serializable
data class UserPreferences(
    val acknowledgedCapabilityIds: Set<String> = emptySet(),
    val useDynamicColor: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
