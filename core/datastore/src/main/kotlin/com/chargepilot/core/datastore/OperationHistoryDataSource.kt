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
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.operationHistoryDataStore by preferencesDataStore(name = "operation_history")

/**
 * Small local audit log for user-initiated charging actions.
 *
 * Records stay on-device and are intentionally capped so the feature is useful without
 * becoming a database migration project.
 *
 * Corrupt JSON is never silently treated as empty on append: the prior raw payload is
 * moved to a backup key once, then the new record starts a fresh list.
 */
@Singleton
class OperationHistoryDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val dataStore = context.operationHistoryDataStore

    val records: Flow<List<OperationRecord>> = dataStore.data
        .map { preferences ->
            val raw = preferences[RecordsJson].orEmpty()
            when (val decoded = decodeOrNull(raw)) {
                null -> {
                    if (raw.isNotBlank()) {
                        Timber.e("Operation history JSON corrupt; exposing empty list (backup preserved)")
                    }
                    emptyList()
                }
                else -> decoded
            }
        }
        .catch {
            Timber.e(it, "Operation history flow failed")
            emit(emptyList())
        }
        .flowOn(ioDispatcher)

    val loadState: Flow<HistoryLoadState> = dataStore.data
        .map { preferences ->
            val raw = preferences[RecordsJson].orEmpty()
            when {
                raw.isBlank() -> HistoryLoadState.Empty
                decodeOrNull(raw) == null -> HistoryLoadState.Corrupted(
                    hasBackup = !preferences[RecordsJsonBackup].isNullOrBlank(),
                )
                else -> HistoryLoadState.Ok
            }
        }
        .catch { emit(HistoryLoadState.Empty) }
        .flowOn(ioDispatcher)

    suspend fun append(record: OperationRecord) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                val raw = preferences[RecordsJson].orEmpty()
                val current = decodeOrNull(raw)
                if (current == null && raw.isNotBlank()) {
                    // Preserve corrupt payload once, then start fresh with this record.
                    if (preferences[RecordsJsonBackup].isNullOrBlank()) {
                        preferences[RecordsJsonBackup] = raw
                    }
                    Timber.e(
                        "Operation history corrupt on append; backed up raw and starting fresh",
                    )
                    preferences[RecordsJson] = json.encodeToString(listOf(record).take(MaxRecords))
                    return@edit
                }
                val list = current.orEmpty()
                preferences[RecordsJson] = json.encodeToString(
                    (listOf(record) + list).take(MaxRecords),
                )
            }
        }
    }

    suspend fun markReverted(recordId: String) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                val raw = preferences[RecordsJson].orEmpty()
                val current = decodeOrNull(raw) ?: return@edit
                val updated = current.map { record ->
                    if (record.id == recordId) record.copy(reverted = true) else record
                }
                preferences[RecordsJson] = json.encodeToString(updated)
            }
        }
    }

    /** Test / recovery helper: returns backup raw if present. */
    suspend fun backupRaw(): String? = withContext(ioDispatcher) {
        var result: String? = null
        dataStore.edit { preferences ->
            result = preferences[RecordsJsonBackup]
        }
        result
    }

    private fun decodeOrNull(raw: String): List<OperationRecord>? {
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<OperationRecord>>(raw) }.getOrNull()
    }

    private companion object {
        val RecordsJson = stringPreferencesKey("records_json")
        val RecordsJsonBackup = stringPreferencesKey("records_json_backup")
        const val MaxRecords = 50
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

sealed interface HistoryLoadState {
    data object Ok : HistoryLoadState
    data object Empty : HistoryLoadState
    data class Corrupted(val hasBackup: Boolean) : HistoryLoadState
}
