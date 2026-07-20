package com.chargepilot.feature.history.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chargepilot.core.capability.CapabilityRegistry
import com.chargepilot.core.control.ControlOrchestrator
import com.chargepilot.core.datastore.OperationHistoryDataSource
import com.chargepilot.core.device.DeviceDetector
import com.chargepilot.core.model.CapabilityType
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlResult
import com.chargepilot.core.model.OperationRecord
import com.chargepilot.core.ui.InfoCard
import com.chargepilot.core.ui.StepList
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val records by viewModel.records.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            InfoCard(
                title = stringResource(R.string.history_title),
                body = stringResource(R.string.history_body),
            )
        }

        if (records.isEmpty()) {
            item {
                InfoCard(
                    title = stringResource(R.string.history_empty_title),
                    body = stringResource(R.string.history_empty_body),
                ) {
                    StepList(
                        steps = listOf(
                            stringResource(R.string.history_step_home),
                            stringResource(R.string.history_step_tap_action),
                            stringResource(R.string.history_step_return),
                        ),
                    )
                }
            }
        } else {
            items(items = records, key = { it.id }) { record ->
                OperationRecordCard(
                    record = record,
                    canRevert = viewModel.canRevert(record),
                    onRevert = { viewModel.revert(record.id) },
                )
            }
        }
    }
}

@Composable
private fun OperationRecordCard(
    record: OperationRecord,
    canRevert: Boolean,
    onRevert: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val status = if (record.success) {
                stringResource(R.string.history_success)
            } else {
                stringResource(R.string.history_failed)
            }
            val missingValue = stringResource(R.string.history_not_captured)
            Text(
                text = record.capability.displayName(),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.history_method_status_format, record.method.displayName(), status),
                style = MaterialTheme.typography.bodyMedium,
                color = if (record.success) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Text(
                text = formatTime(record.timestampMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.history_before_format, record.before ?: missingValue),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.history_after_format, record.after ?: missingValue),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (record.reverted) {
                Text(
                    text = stringResource(R.string.history_reverted),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (canRevert) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRevert, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.history_revert))
                }
            }
        }
    }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val operationHistory: OperationHistoryDataSource,
    private val controlOrchestrator: ControlOrchestrator,
    private val capabilityRegistry: CapabilityRegistry,
    private val deviceDetector: DeviceDetector,
) : ViewModel() {
    val records: StateFlow<List<OperationRecord>> = operationHistory.records
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun canRevert(record: OperationRecord): Boolean {
        if (!record.success || record.reverted) return false
        if (record.capabilityId == null || record.rawBefore == null) return false
        return record.method == ControlMethod.WRITE_SETTINGS_KEY ||
            record.method == ControlMethod.SHIZUKU_RPC ||
            record.method == ControlMethod.ROOT_SHELL
    }

    fun revert(recordId: String) {
        viewModelScope.launch {
            val record = records.value.firstOrNull { it.id == recordId } ?: return@launch
            if (!canRevert(record)) return@launch
            val capabilityId = record.capabilityId ?: return@launch
            val rawBefore = record.rawBefore ?: return@launch
            val profile = deviceDetector.detect()
            val descriptor = capabilityRegistry.resolve(profile)
                .firstOrNull { it.id == capabilityId }
                ?: capabilityRegistry.rules()
                    .flatMap { it.capabilities }
                    .firstOrNull { it.id == capabilityId }
                ?: return@launch
            val strategy = controlOrchestrator.strategyFor(record.method) ?: return@launch
            val enable = rawBefore != "0"
            // rawBefore is the pre-write value: restore by writing that value.
            // setEnabled(true) writes 1; setEnabled(false) writes 0.
            val result = strategy.setEnabled(descriptor, enabled = enable)
            operationHistory.markReverted(recordId)
            operationHistory.append(
                OperationRecord(
                    id = "${System.currentTimeMillis()}-revert-${UUID.randomUUID()}",
                    capability = record.capability,
                    method = record.method,
                    before = record.after,
                    after = if (result is ControlResult.Success) {
                        "Reverted to raw=$rawBefore"
                    } else {
                        "Revert failed: ${(result as? ControlResult.Failed)?.reason?.name}"
                    },
                    success = result is ControlResult.Success,
                    timestampMs = System.currentTimeMillis(),
                    capabilityId = capabilityId,
                    settingsKeyName = record.settingsKeyName,
                    rawBefore = record.rawAfter,
                    rawAfter = rawBefore,
                ),
            )
        }
    }
}

private fun formatTime(timestampMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(timestampMs))

@Composable
private fun CapabilityType.displayName(): String = when (this) {
    CapabilityType.PAUSE_PD_DURING_GAMING -> stringResource(R.string.history_cap_pause_pd)
    CapabilityType.BYPASS_CHARGING -> stringResource(R.string.history_cap_bypass)
    CapabilityType.CHARGE_SEPARATION -> stringResource(R.string.history_cap_charge_separation)
    CapabilityType.BATTERY_LIMIT_80 -> stringResource(R.string.history_cap_battery_limit_80)
    CapabilityType.ADAPTIVE_CHARGING -> stringResource(R.string.history_cap_adaptive_charging)
    CapabilityType.SMART_CHARGING -> stringResource(R.string.history_cap_smart_charging)
    CapabilityType.CUSTOM_CHARGE_LIMIT -> stringResource(R.string.history_cap_custom_charge_limit)
}

@Composable
private fun ControlMethod.displayName(): String = when (this) {
    ControlMethod.OFFICIAL_GUIDANCE -> stringResource(R.string.history_method_official)
    ControlMethod.WRITE_SETTINGS_KEY -> stringResource(R.string.history_method_write_settings)
    ControlMethod.SHIZUKU_RPC -> stringResource(R.string.history_method_shizuku)
    ControlMethod.ROOT_SHELL -> stringResource(R.string.history_method_root)
}
