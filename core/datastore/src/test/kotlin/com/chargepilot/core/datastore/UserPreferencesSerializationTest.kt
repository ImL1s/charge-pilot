package com.chargepilot.core.datastore

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class UserPreferencesSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `defaults round trip`() {
        val prefs = UserPreferences()
        val encoded = json.encodeToString(prefs)
        val decoded = json.decodeFromString(UserPreferences.serializer(), encoded)
        assertThat(decoded.acknowledgedCapabilityIds).isEmpty()
        assertThat(decoded.useDynamicColor).isTrue()
        assertThat(decoded.themeMode).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `acknowledge set survives encode`() {
        val prefs = UserPreferences(
            acknowledgedCapabilityIds = setOf("rule::PAUSE_PD_DURING_GAMING"),
            themeMode = ThemeMode.DARK,
        )
        val decoded = json.decodeFromString(
            UserPreferences.serializer(),
            json.encodeToString(prefs),
        )
        assertThat(decoded.acknowledgedCapabilityIds)
            .containsExactly("rule::PAUSE_PD_DURING_GAMING")
        assertThat(decoded.themeMode).isEqualTo(ThemeMode.DARK)
    }
}
