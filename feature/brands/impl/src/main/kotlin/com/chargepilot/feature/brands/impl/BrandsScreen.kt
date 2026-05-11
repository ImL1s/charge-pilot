package com.chargepilot.feature.brands.impl

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
import com.chargepilot.core.capability.CapabilityRule
import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.CapabilityType
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.Evidence
import com.chargepilot.core.ui.InfoCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@Composable
fun BrandsScreen(viewModel: BrandsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        BrandsUiState.Loading -> BrandsFrame {
            item {
                InfoCard(
                    title = stringResource(R.string.brands_loading_title),
                    body = stringResource(R.string.brands_loading_body),
                )
            }
        }
        is BrandsUiState.Error -> BrandsFrame {
            item {
                InfoCard(
                    title = stringResource(R.string.brands_error_title),
                    body = current.message,
                )
            }
        }
        is BrandsUiState.Ready -> BrandsFrame {
            item {
                InfoCard(
                    title = stringResource(R.string.brands_ready_title),
                    body = stringResource(R.string.brands_ready_body),
                )
            }
            items(items = current.brands, key = { it.ruleId }) { brand ->
                BrandCard(brand = brand)
            }
        }
    }
}

@Composable
private fun BrandsFrame(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun BrandCard(brand: BrandCapabilityUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = brand.brandName, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = brand.matchSummary(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            brand.capabilities.forEach { capability ->
                val methods = capability.methodsLabel()
                val manualPath = capability.manualPath
                Text(text = capability.type.displayName(), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.brands_route_format, methods),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (manualPath != null) {
                    Text(
                        text = stringResource(R.string.brands_manual_path_format, manualPath),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = capability.evidenceLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@HiltViewModel
class BrandsViewModel @Inject constructor(
    registry: CapabilityRegistry,
) : ViewModel() {
    val state: StateFlow<BrandsUiState> = flow<BrandsUiState> {
        emit(
            BrandsUiState.Ready(
                registry.rules().map { rule -> rule.toUi() },
            ),
        )
    }.catch { throwable ->
        emit(BrandsUiState.Error(throwable.message ?: "Unknown registry error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BrandsUiState.Loading,
    )
}

sealed interface BrandsUiState {
    data object Loading : BrandsUiState
    data class Ready(val brands: List<BrandCapabilityUi>) : BrandsUiState
    data class Error(val message: String) : BrandsUiState
}

data class BrandCapabilityUi(
    val ruleId: String,
    val brandName: String,
    val modelRegex: String?,
    val minRomFlavor: String?,
    val minRomVersion: String?,
    val capabilities: List<CapabilityUi>,
)

data class CapabilityUi(
    val type: CapabilityType,
    val methods: List<ControlMethod>,
    val manualPath: String?,
    val evidence: Evidence,
    val verifiedDate: String?,
    val hasSource: Boolean,
)

private fun CapabilityRule.toUi(): BrandCapabilityUi =
    BrandCapabilityUi(
        ruleId = id,
        brandName = matchers.manufacturer.toLabel(),
        modelRegex = matchers.modelRegex,
        minRomFlavor = matchers.minRomVersion?.flavor?.toLabel(),
        minRomVersion = matchers.minRomVersion?.version,
        capabilities = capabilities.map { it.toUi() },
    )

private fun CapabilityDescriptor.toUi(): CapabilityUi =
    CapabilityUi(
        type = type,
        methods = availableMethods,
        manualPath = officialIntent?.fallbackPath ?: guidanceSteps.firstOrNull(),
        evidence = evidence,
        verifiedDate = verifiedDate,
        hasSource = sourceUrl != null,
    )

@Composable
private fun BrandCapabilityUi.matchSummary(): String {
    val parts = mutableListOf<String>()
    if (modelRegex != null) {
        parts += stringResource(R.string.brands_models_format, modelRegex)
    }
    if (minRomFlavor != null && minRomVersion != null) {
        parts += stringResource(R.string.brands_min_rom_format, minRomFlavor, minRomVersion)
    }
    if (parts.isEmpty()) {
        parts += stringResource(R.string.brands_all_supported_format, brandName)
    }
    return parts.joinToString(" · ")
}

@Composable
private fun CapabilityUi.methodsLabel(): String =
    if (methods.isEmpty()) {
        stringResource(R.string.brands_recorded_unsupported)
    } else {
        val labels = mutableListOf<String>()
        methods.forEach { method -> labels += method.displayName() }
        labels.joinToString()
    }

@Composable
private fun CapabilityUi.evidenceLabel(): String {
    var label = evidence.displayName()
    if (verifiedDate != null) {
        label = stringResource(R.string.brands_verified_format, label, verifiedDate)
    }
    if (hasSource) {
        label = stringResource(R.string.brands_source_recorded_format, label)
    }
    return label
}

@Composable
private fun CapabilityType.displayName(): String = when (this) {
    CapabilityType.PAUSE_PD_DURING_GAMING -> stringResource(R.string.brands_cap_pause_pd)
    CapabilityType.BYPASS_CHARGING -> stringResource(R.string.brands_cap_bypass)
    CapabilityType.CHARGE_SEPARATION -> stringResource(R.string.brands_cap_charge_separation)
    CapabilityType.BATTERY_LIMIT_80 -> stringResource(R.string.brands_cap_battery_limit_80)
    CapabilityType.ADAPTIVE_CHARGING -> stringResource(R.string.brands_cap_adaptive_charging)
    CapabilityType.SMART_CHARGING -> stringResource(R.string.brands_cap_smart_charging)
    CapabilityType.CUSTOM_CHARGE_LIMIT -> stringResource(R.string.brands_cap_custom_charge_limit)
}

@Composable
private fun ControlMethod.displayName(): String = when (this) {
    ControlMethod.OFFICIAL_GUIDANCE -> stringResource(R.string.brands_method_official)
    ControlMethod.WRITE_SETTINGS_KEY -> stringResource(R.string.brands_method_write_settings)
    ControlMethod.SHIZUKU_RPC -> stringResource(R.string.brands_method_shizuku)
    ControlMethod.ROOT_SHELL -> stringResource(R.string.brands_method_root)
}

@Composable
private fun Evidence.displayName(): String = when (this) {
    Evidence.OFFICIAL_DOC -> stringResource(R.string.brands_evidence_official_doc)
    Evidence.OFFICIAL_COMMUNITY -> stringResource(R.string.brands_evidence_official_community)
    Evidence.COMMUNITY_TESTED -> stringResource(R.string.brands_evidence_community_tested)
    Evidence.PROJECT_VERIFIED -> stringResource(R.string.brands_evidence_project_verified)
    Evidence.UNVERIFIED -> stringResource(R.string.brands_evidence_unverified)
}

private fun String.toLabel(): String =
    when (this) {
        "ONEPLUS" -> "OnePlus"
        "ONE_UI" -> "One UI"
        "OXYGEN_OS" -> "OxygenOS"
        "COLOR_OS" -> "ColorOS"
        "HYPER_OS" -> "HyperOS"
        "MAGIC_OS", "HONOR_MAGIC" -> "MagicOS"
        "EMUI" -> "EMUI"
        "AOSP" -> "AOSP"
        "HONOR" -> "HONOR"
        "POCO" -> "POCO"
        else -> lowercase()
            .split('_')
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    }
