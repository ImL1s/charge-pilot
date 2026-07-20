package com.chargepilot.feature.disclosure.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chargepilot.core.capability.CapabilityRegistry
import com.chargepilot.core.datastore.UserPreferencesDataSource
import com.chargepilot.core.device.DeviceDetector
import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.ui.InfoCard
import com.chargepilot.core.ui.StepList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun DisclosureScreen(
    capabilityId: String,
    onFinished: () -> Unit,
    viewModel: DisclosureViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var understoodExperimental by remember { mutableStateOf(false) }
    var understoodWrite by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(capabilityId) {
        viewModel.load(capabilityId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoCard(
            title = stringResource(R.string.disclosure_title),
            body = stringResource(R.string.disclosure_body_format, capabilityId),
        )
        when (val current = state) {
            is DisclosureUiState.Loading -> {
                Text(stringResource(R.string.disclosure_loading))
            }
            is DisclosureUiState.Missing -> {
                Text(stringResource(R.string.disclosure_missing))
            }
            is DisclosureUiState.Ready -> {
                val d = current.descriptor
                InfoCard(
                    title = d.type.name,
                    body = buildString {
                        append("Evidence: ${d.evidence.name}")
                        d.sourceUrl?.let { append("\nSource: $it") }
                        d.settingsKey?.let {
                            append("\nSettings: ${it.namespace}/${it.key} (${it.type})")
                            append("\nIntended values: 0 (off) / 1 (on)")
                        }
                        if (d.preconditions.isNotEmpty()) {
                            append("\nPreconditions: ")
                            append(d.preconditions.joinToString { it::class.simpleName ?: "?" })
                        }
                        append("\nMethods: ")
                        append(d.availableMethods.joinToString { it.name })
                    },
                )
            }
        }
        InfoCard(
            title = stringResource(R.string.disclosure_local_title),
            body = stringResource(R.string.disclosure_local_body),
        )
        InfoCard(
            title = stringResource(R.string.disclosure_safety_title),
            body = stringResource(R.string.disclosure_step_user_initiated),
        ) {
            StepList(
                steps = listOf(
                    stringResource(R.string.disclosure_step_preconditions),
                    stringResource(R.string.disclosure_step_shizuku),
                    stringResource(R.string.disclosure_step_samsung),
                ),
            )
        }
        CheckboxRow(
            checked = understoodExperimental,
            onCheckedChange = { understoodExperimental = it },
            label = stringResource(R.string.disclosure_check_experimental),
        )
        CheckboxRow(
            checked = understoodWrite,
            onCheckedChange = { understoodWrite = it },
            label = stringResource(R.string.disclosure_check_write),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    viewModel.acknowledge(capabilityId)
                    onFinished()
                }
            },
            enabled = understoodExperimental && understoodWrite,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.disclosure_continue))
        }
    }
}

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

sealed interface DisclosureUiState {
    data object Loading : DisclosureUiState
    data object Missing : DisclosureUiState
    data class Ready(val descriptor: CapabilityDescriptor) : DisclosureUiState
}

@HiltViewModel
class DisclosureViewModel @Inject constructor(
    private val deviceDetector: DeviceDetector,
    private val capabilityRegistry: CapabilityRegistry,
    private val userPreferences: UserPreferencesDataSource,
) : ViewModel() {
    private val _state = MutableStateFlow<DisclosureUiState>(DisclosureUiState.Loading)
    val state: StateFlow<DisclosureUiState> = _state.asStateFlow()

    fun load(capabilityId: String) {
        viewModelScope.launch {
            _state.value = runCatching {
                val profile = deviceDetector.detect()
                val descriptor = capabilityRegistry.resolve(profile)
                    .firstOrNull { it.id == capabilityId }
                    ?: capabilityRegistry.rules()
                        .flatMap { it.capabilities }
                        .firstOrNull { it.id == capabilityId }
                if (descriptor == null) {
                    DisclosureUiState.Missing
                } else {
                    DisclosureUiState.Ready(descriptor)
                }
            }.getOrElse { DisclosureUiState.Missing }
        }
    }

    suspend fun acknowledge(capabilityId: String) {
        userPreferences.acknowledgeDisclosure(capabilityId)
    }
}
