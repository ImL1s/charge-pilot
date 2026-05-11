package com.chargepilot.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chargepilot.core.model.CapabilityType
import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlSetupStage
import com.chargepilot.core.model.ControlSetupStatus
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.Evidence
import com.chargepilot.core.model.Precondition

/** Visual variant for a [CapabilityCard]. */
enum class CapabilityCardVariant { Unsupported, AvailableIdle, PendingConditions, Active }

@Composable
fun CapabilityCard(
    descriptor: CapabilityDescriptor,
    state: ControlState,
    directControlMethods: List<ControlMethod> = descriptor.directControlMethodsFromDescriptor(),
    setupMethods: List<ControlMethod> = emptyList(),
    setupStatuses: List<ControlSetupStatus> = emptyList(),
    onTryClick: () -> Unit,
    onOpenOfficialClick: () -> Unit,
    onSetupClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val variant = state.toVariant(descriptor)
    val shizukuSetupStatus = setupStatuses
        .firstOrNull { it.method == ControlMethod.SHIZUKU_RPC && it.stage != ControlSetupStage.READY }
    val borderColor = when (variant) {
        CapabilityCardVariant.Active -> MaterialTheme.colorScheme.tertiary
        CapabilityCardVariant.PendingConditions -> MaterialTheme.colorScheme.secondary
        CapabilityCardVariant.Unsupported -> MaterialTheme.colorScheme.outlineVariant
        CapabilityCardVariant.AvailableIdle -> MaterialTheme.colorScheme.outline
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = descriptor.type.displayName(),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = descriptor.evidenceLine(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = descriptor.routeLine(directControlMethods, setupMethods),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            StateLine(state = state)
            RuntimeConditionChecklist(descriptor = descriptor, state = state)
            descriptor.officialIntent?.fallbackPath?.let { path ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.fallback_path_format, path),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (
                descriptor.guidanceSteps.isNotEmpty() &&
                directControlMethods.isEmpty() &&
                setupMethods.isEmpty()
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.fallback_notes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                descriptor.guidanceSteps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (shizukuSetupStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = shizukuSetupStatus.title(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = shizukuSetupStatus.detail(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (directControlMethods.isNotEmpty() && variant != CapabilityCardVariant.Unsupported) {
                    Button(
                        onClick = onTryClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = descriptor.primaryActionLabel(state))
                    }
                }
                if (ControlMethod.SHIZUKU_RPC in setupMethods) {
                    if (directControlMethods.isEmpty()) {
                        Button(onClick = onSetupClick, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = shizukuSetupStatus?.buttonLabel()
                                    ?: stringResource(R.string.setup_app_control_shizuku),
                            )
                        }
                    } else {
                        OutlinedButton(onClick = onSetupClick, modifier = Modifier.fillMaxWidth()) {
                            Text(text = stringResource(R.string.setup_shizuku_fallback))
                        }
                    }
                }
                if (descriptor.officialIntent != null) {
                    TextButton(
                        onClick = onOpenOfficialClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.open_fallback_path))
                    }
                }
            }
        }
    }
}

@Composable
private fun StateLine(state: ControlState) {
    val (text, color) = when (state) {
        ControlState.Unknown ->
            stringResource(R.string.state_ready_to_check) to MaterialTheme.colorScheme.onSurfaceVariant
        ControlState.Inactive -> stringResource(R.string.state_off) to MaterialTheme.colorScheme.onSurfaceVariant
        ControlState.Engaged ->
            stringResource(R.string.state_on_checking) to MaterialTheme.colorScheme.onSurfaceVariant
        is ControlState.PendingConditions ->
            stringResource(R.string.state_waiting_format, state.unmet.requirementSummary()) to
                MaterialTheme.colorScheme.onSurfaceVariant
        is ControlState.Active -> stringResource(R.string.state_active) to MaterialTheme.colorScheme.tertiary
    }
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
}

@Composable
private fun RuntimeConditionChecklist(
    descriptor: CapabilityDescriptor,
    state: ControlState,
) {
    if (descriptor.preconditions.isEmpty()) return

    val unmet = (state as? ControlState.PendingConditions)?.unmet.orEmpty()
    val hasLiveEvaluation = state is ControlState.Engaged ||
        state is ControlState.PendingConditions ||
        state is ControlState.Active
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.live_requirements),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        descriptor.preconditions.forEach { precondition ->
            val status = when {
                !hasLiveEvaluation -> ConditionStatus.Required
                state is ControlState.Engaged -> ConditionStatus.Checking
                state is ControlState.Active -> ConditionStatus.Ready
                precondition in unmet -> ConditionStatus.Waiting
                else -> ConditionStatus.Ready
            }
            ConditionRow(status = status, precondition = precondition)
        }
    }
    if (state is ControlState.PendingConditions) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.condition_tip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConditionRow(
    status: ConditionStatus,
    precondition: Precondition,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = status.containerColor(),
                    shape = RoundedCornerShape(999.dp),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = status.label(),
                style = MaterialTheme.typography.labelSmall,
                color = status.contentColor(),
            )
        }
        Text(
            text = precondition.displayLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private enum class ConditionStatus { Ready, Waiting, Required, Checking }

@Composable
private fun ConditionStatus.label(): String = when (this) {
    ConditionStatus.Ready -> stringResource(R.string.condition_ready)
    ConditionStatus.Waiting -> stringResource(R.string.condition_waiting)
    ConditionStatus.Required -> stringResource(R.string.condition_required)
    ConditionStatus.Checking -> stringResource(R.string.condition_checking)
}

@Composable
private fun ConditionStatus.containerColor() = when (this) {
    ConditionStatus.Ready -> MaterialTheme.colorScheme.primaryContainer
    ConditionStatus.Waiting -> MaterialTheme.colorScheme.secondaryContainer
    ConditionStatus.Required -> MaterialTheme.colorScheme.surfaceVariant
    ConditionStatus.Checking -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun ConditionStatus.contentColor() = when (this) {
    ConditionStatus.Ready -> MaterialTheme.colorScheme.onPrimaryContainer
    ConditionStatus.Waiting -> MaterialTheme.colorScheme.onSecondaryContainer
    ConditionStatus.Required -> MaterialTheme.colorScheme.onSurfaceVariant
    ConditionStatus.Checking -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun ControlState.toVariant(descriptor: CapabilityDescriptor): CapabilityCardVariant =
    when {
        descriptor.availableMethods.isEmpty() -> CapabilityCardVariant.Unsupported
        this is ControlState.Active -> CapabilityCardVariant.Active
        this is ControlState.PendingConditions -> CapabilityCardVariant.PendingConditions
        else -> CapabilityCardVariant.AvailableIdle
    }

private fun CapabilityDescriptor.directControlMethodsFromDescriptor(): List<ControlMethod> =
    availableMethods.filter { method ->
        method == ControlMethod.WRITE_SETTINGS_KEY ||
            method == ControlMethod.SHIZUKU_RPC ||
            method == ControlMethod.ROOT_SHELL
    }

private fun ControlState.isConfiguredOn(): Boolean =
    this is ControlState.Active ||
        this is ControlState.PendingConditions ||
        this is ControlState.Engaged

@Composable
private fun Precondition.displayLabel(): String = when (this) {
    Precondition.PdChargerPresent -> stringResource(R.string.precondition_pd_charger)
    is Precondition.BatteryLevelAbove -> stringResource(R.string.precondition_battery_above_format, percent)
    is Precondition.GameInForeground -> stringResource(R.string.precondition_game_foreground)
}

@Composable
private fun List<Precondition>.requirementSummary(): String = when (size) {
    0 -> stringResource(R.string.summary_live_requirements)
    1 -> first().summaryPhrase()
    2 -> stringResource(R.string.summary_two_format, first().summaryPhrase(), last().summaryPhrase())
    else -> {
        val leading = mutableListOf<String>()
        for (precondition in dropLast(1)) {
            leading += precondition.summaryPhrase()
        }
        stringResource(
            R.string.summary_many_format,
            leading.joinToString(", "),
            last().summaryPhrase(),
        )
    }
}

@Composable
private fun Precondition.summaryPhrase(): String = when (this) {
    Precondition.PdChargerPresent -> stringResource(R.string.summary_pd_charger)
    is Precondition.BatteryLevelAbove -> stringResource(R.string.summary_battery_above_format, percent)
    is Precondition.GameInForeground -> stringResource(R.string.summary_game_foreground)
}

@Composable
private fun CapabilityType.displayName(): String = when (this) {
    CapabilityType.PAUSE_PD_DURING_GAMING -> stringResource(R.string.cap_pause_pd)
    CapabilityType.BYPASS_CHARGING -> stringResource(R.string.cap_bypass)
    CapabilityType.CHARGE_SEPARATION -> stringResource(R.string.cap_charge_separation)
    CapabilityType.BATTERY_LIMIT_80 -> stringResource(R.string.cap_battery_limit_80)
    CapabilityType.ADAPTIVE_CHARGING -> stringResource(R.string.cap_adaptive_charging)
    CapabilityType.SMART_CHARGING -> stringResource(R.string.cap_smart_charging)
    CapabilityType.CUSTOM_CHARGE_LIMIT -> stringResource(R.string.cap_custom_charge_limit)
}

@Composable
private fun ControlMethod.displayName(): String = when (this) {
    ControlMethod.OFFICIAL_GUIDANCE -> stringResource(R.string.method_official)
    ControlMethod.WRITE_SETTINGS_KEY -> stringResource(R.string.method_write_settings)
    ControlMethod.SHIZUKU_RPC -> stringResource(R.string.method_shizuku)
    ControlMethod.ROOT_SHELL -> stringResource(R.string.method_root)
}

@Composable
private fun ControlSetupStatus.title(): String = when (stage) {
    ControlSetupStage.NOT_INSTALLED -> stringResource(R.string.setup_title_install_shizuku)
    ControlSetupStage.INSTALLED_NOT_RUNNING -> stringResource(R.string.setup_title_start_shizuku)
    ControlSetupStage.PERMISSION_REQUIRED -> stringResource(R.string.setup_title_grant_charge_pilot)
    ControlSetupStage.UNAVAILABLE -> stringResource(R.string.setup_title_unavailable)
    ControlSetupStage.UNSUPPORTED -> stringResource(R.string.setup_title_unsupported)
    ControlSetupStage.READY -> stringResource(R.string.setup_title_ready)
}

@Composable
private fun ControlSetupStatus.detail(): String = when (stage) {
    ControlSetupStage.NOT_INSTALLED -> stringResource(R.string.setup_detail_install_shizuku)
    ControlSetupStage.INSTALLED_NOT_RUNNING -> stringResource(R.string.setup_detail_start_shizuku)
    ControlSetupStage.PERMISSION_REQUIRED -> stringResource(R.string.setup_detail_grant_charge_pilot)
    ControlSetupStage.UNAVAILABLE -> stringResource(R.string.setup_detail_unavailable)
    ControlSetupStage.UNSUPPORTED -> stringResource(R.string.setup_detail_unsupported)
    ControlSetupStage.READY -> stringResource(R.string.setup_detail_ready)
}

@Composable
private fun ControlSetupStatus.buttonLabel(): String = when (stage) {
    ControlSetupStage.NOT_INSTALLED -> stringResource(R.string.setup_button_install_shizuku)
    ControlSetupStage.INSTALLED_NOT_RUNNING -> stringResource(R.string.setup_button_open_shizuku)
    ControlSetupStage.PERMISSION_REQUIRED -> stringResource(R.string.setup_button_grant_shizuku)
    ControlSetupStage.UNAVAILABLE -> stringResource(R.string.setup_button_open_shizuku_setup)
    ControlSetupStage.UNSUPPORTED -> stringResource(R.string.setup_button_set_up_app_control)
    ControlSetupStage.READY -> stringResource(R.string.setup_button_ready)
}

@Composable
private fun CapabilityDescriptor.primaryActionLabel(state: ControlState): String {
    if (state.isConfiguredOn()) {
        return when (type) {
            CapabilityType.PAUSE_PD_DURING_GAMING,
            CapabilityType.BYPASS_CHARGING,
            CapabilityType.CHARGE_SEPARATION,
            -> stringResource(R.string.action_turn_off_bypass)
            else -> stringResource(R.string.action_turn_off)
        }
    }
    return when (type) {
        CapabilityType.PAUSE_PD_DURING_GAMING,
        CapabilityType.BYPASS_CHARGING,
        CapabilityType.CHARGE_SEPARATION,
        -> stringResource(R.string.action_turn_on_bypass)
        else -> stringResource(R.string.action_turn_on)
    }
}

@Composable
private fun CapabilityDescriptor.evidenceLine(): String {
    val evidenceLabel = evidence.displayName()
    val verified = verifiedDate
    val detail = if (verified != null) {
        stringResource(R.string.evidence_verified_format, evidenceLabel, verified)
    } else {
        evidenceLabel
    }
    return stringResource(R.string.evidence_prefix, detail)
}


@Composable
private fun Evidence.displayName(): String = when (this) {
    Evidence.OFFICIAL_DOC -> stringResource(R.string.evidence_official_doc)
    Evidence.OFFICIAL_COMMUNITY -> stringResource(R.string.evidence_official_community)
    Evidence.COMMUNITY_TESTED -> stringResource(R.string.evidence_community_tested)
    Evidence.PROJECT_VERIFIED -> stringResource(R.string.evidence_project_verified)
    Evidence.UNVERIFIED -> stringResource(R.string.evidence_unverified)
}

@Composable
private fun CapabilityDescriptor.routeLine(
    directControlMethods: List<ControlMethod>,
    setupMethods: List<ControlMethod>,
): String {
    val registryDirect = directControlMethodsFromDescriptor()
    return when {
        directControlMethods.isNotEmpty() -> {
            val methodLabels = mutableListOf<String>()
            for (method in directControlMethods) {
                methodLabels += method.displayName()
            }
            stringResource(
                R.string.route_app_control_format,
                methodLabels.joinToString(),
            )
        }
        setupMethods.isNotEmpty() -> {
            val methodLabels = mutableListOf<String>()
            for (method in setupMethods) {
                methodLabels += method.displayName()
            }
            stringResource(
                R.string.route_needs_setup_format,
                methodLabels.joinToString(),
            )
        }
        registryDirect.isNotEmpty() ->
            stringResource(R.string.route_exists_inactive)
        ControlMethod.OFFICIAL_GUIDANCE in availableMethods ->
            stringResource(R.string.route_manual_available)
        else -> stringResource(R.string.route_none_registered)
    }
}
