package com.chargepilot.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class CapabilityType {
    PAUSE_PD_DURING_GAMING,
    BYPASS_CHARGING,
    CHARGE_SEPARATION,
    BATTERY_LIMIT_80,
    ADAPTIVE_CHARGING,
}

@Serializable
enum class ControlMethod {
    OFFICIAL_GUIDANCE,
    WRITE_SETTINGS_KEY,
    SHIZUKU_RPC,
    ROOT_SHELL,
}

@Serializable
enum class Evidence {
    OFFICIAL_DOC,
    OFFICIAL_COMMUNITY,
    COMMUNITY_TESTED,
    PROJECT_VERIFIED,
    UNVERIFIED,
}

@Serializable
data class SettingsKey(
    val namespace: String,
    val key: String,
    val type: String,
)

@Serializable
data class IntentSpec(
    val action: String,
    val component: String? = null,
    val fallbackPath: String? = null,
)

@Serializable
sealed interface Precondition {
    @Serializable
    @kotlinx.serialization.SerialName("PdChargerPresent")
    data object PdChargerPresent : Precondition

    @Serializable
    @kotlinx.serialization.SerialName("BatteryLevelAbove")
    data class BatteryLevelAbove(val percent: Int) : Precondition

    @Serializable
    @kotlinx.serialization.SerialName("GameInForeground")
    data class GameInForeground(val knownGames: Set<String>? = null) : Precondition
}

/**
 * A single, fully-described capability that the app can detect, guide, and possibly
 * control. One [DeviceProfile] may resolve to several [CapabilityDescriptor]s (e.g. a
 * Pixel may surface both `BATTERY_LIMIT_80` and `ADAPTIVE_CHARGING`).
 */
@Serializable
data class CapabilityDescriptor(
    /**
     * Stable, registry-unique identifier. Used as the key for navigation, persistence,
     * and history records. Format: `<rule-id>::<capability-type>` enforced by the
     * registry loader.
     */
    val id: String,
    val type: CapabilityType,
    val availableMethods: List<ControlMethod>,
    val settingsKey: SettingsKey? = null,
    val officialIntent: IntentSpec? = null,
    val preconditions: List<Precondition> = emptyList(),
    val evidence: Evidence,
    val sourceUrl: String? = null,
    val verifiedDate: String? = null,
)
