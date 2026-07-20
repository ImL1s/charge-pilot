package com.chargepilot.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chargepilot.core.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

/**
 * Preferences DataStore for consent + theme hooks.
 *
 * Uses Preferences DataStore + kotlinx.serialization JSON (matching
 * [OperationHistoryDataSource]) rather than Proto DataStore to avoid protobuf plugins.
 */
@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val dataStore = context.userPreferencesDataStore

    val preferences: Flow<UserPreferences> = dataStore.data
        .map { prefs -> decode(prefs[PreferencesJson].orEmpty()) }
        .catch { emit(UserPreferences()) }
        .flowOn(ioDispatcher)

    suspend fun acknowledgeDisclosure(capabilityId: String) {
        withContext(ioDispatcher) {
            dataStore.edit { prefs ->
                val current = decode(prefs[PreferencesJson].orEmpty())
                val updated = current.copy(
                    acknowledgedCapabilityIds = current.acknowledgedCapabilityIds + capabilityId,
                )
                prefs[PreferencesJson] = json.encodeToString(updated)
            }
        }
    }

    suspend fun hasAcknowledged(capabilityId: String): Boolean {
        return withContext(ioDispatcher) {
            preferences.first().acknowledgedCapabilityIds.contains(capabilityId)
        }
    }

    suspend fun updateTheme(useDynamicColor: Boolean? = null, themeMode: ThemeMode? = null) {
        withContext(ioDispatcher) {
            dataStore.edit { prefs ->
                val current = decode(prefs[PreferencesJson].orEmpty())
                val updated = current.copy(
                    useDynamicColor = useDynamicColor ?: current.useDynamicColor,
                    themeMode = themeMode ?: current.themeMode,
                )
                prefs[PreferencesJson] = json.encodeToString(updated)
            }
        }
    }

    private fun decode(raw: String): UserPreferences {
        if (raw.isBlank()) return UserPreferences()
        return runCatching { json.decodeFromString<UserPreferences>(raw) }
            .getOrDefault(UserPreferences())
    }

    private companion object {
        val PreferencesJson = stringPreferencesKey("preferences_json")
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
