package com.chargepilot.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chargepilot.core.common.IoDispatcher
import com.chargepilot.core.model.OperationRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.operationHistoryDataStore by preferencesDataStore(name = "operation_history")

/**
 * Small local audit log for user-initiated charging actions.
 *
 * Records stay on-device and are intentionally capped so the feature is useful without
 * becoming a database migration project.
 */
@Singleton
class OperationHistoryDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val dataStore = context.operationHistoryDataStore

    val records: Flow<List<OperationRecord>> = dataStore.data
        .map { preferences -> decode(preferences[RecordsJson].orEmpty()) }
        .catch { emit(emptyList()) }
        .flowOn(ioDispatcher)

    suspend fun append(record: OperationRecord) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                val current = decode(preferences[RecordsJson].orEmpty())
                preferences[RecordsJson] = json.encodeToString(
                    (listOf(record) + current).take(MaxRecords),
                )
            }
        }
    }

    private fun decode(raw: String): List<OperationRecord> {
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<OperationRecord>>(raw) }
            .getOrDefault(emptyList())
    }

    private companion object {
        val RecordsJson = stringPreferencesKey("records_json")
        const val MaxRecords = 50
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
