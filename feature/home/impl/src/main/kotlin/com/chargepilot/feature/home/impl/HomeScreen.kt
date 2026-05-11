package com.chargepilot.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlSetupStage
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.ui.CapabilityCard
import com.chargepilot.core.ui.DeviceProfileCard
import com.chargepilot.core.ui.InfoCard
import com.chargepilot.core.ui.StepList
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    LaunchedEffect(viewModel) {
        while (true) {
            delay(2_000L)
            viewModel.refresh()
        }
    }
    Scaffold { innerPadding ->
        when (val current = state) {
            is HomeUiState.Loading -> CenteredText(stringResource(R.string.home_loading), innerPadding)
            is HomeUiState.Error -> CenteredText(
                stringResource(R.string.home_error_format, current.message),
                innerPadding,
            )
            is HomeUiState.Ready -> ReadyContent(
                state = current,
                innerPadding = innerPadding,
                onTryClick = viewModel::tryEnable,
                onOpenOfficialClick = viewModel::openOfficialSettings,
                onSetupClick = viewModel::setupPrivilegedMethod,
                onOpenSamsungGameSetupClick = viewModel::openSamsungGameClassificationSetup,
                onOpenSamsungGamingHubClick = viewModel::openSamsungGamingHub,
            )
        }
    }
}

@Composable
private fun CenteredText(text: String, padding: PaddingValues) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
    )
}

@Composable
private fun ReadyContent(
    state: HomeUiState.Ready,
    innerPadding: PaddingValues,
    onTryClick: (String) -> Unit,
    onOpenOfficialClick: (String) -> Unit,
    onSetupClick: (String) -> Unit,
    onOpenSamsungGameSetupClick: () -> Unit,
    onOpenSamsungGamingHubClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DeviceProfileCard(profile = state.profile) }
        item {
            InfoCard(
                title = stringResource(R.string.home_app_control_title),
                body = state.appControlSummary(),
            ) {
                val steps = state.appControlSteps()
                if (steps.isNotEmpty()) {
                    StepList(steps = steps)
                }
            }
        }
        state.samsungGameSetup?.let { setup ->
            item {
                SamsungGameSetupCard(
                    setup = setup,
                    onOpenClassificationClick = onOpenSamsungGameSetupClick,
                    onOpenGamingHubClick = onOpenSamsungGamingHubClick,
                )
            }
        }
        state.statusMessage?.let { message ->
            item {
                Text(
                    text = message.displayText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.capabilities.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_no_capabilities),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(items = state.capabilities, key = { it.descriptor.id }) { row ->
            CapabilityCard(
                descriptor = row.descriptor,
                state = row.state,
                directControlMethods = row.directControlMethods,
                setupMethods = row.setupMethods,
                setupStatuses = row.setupStatuses,
                onTryClick = { onTryClick(row.descriptor.id) },
                onOpenOfficialClick = { onOpenOfficialClick(row.descriptor.id) },
                onSetupClick = { onSetupClick(row.descriptor.id) },
            )
        }
    }
}

@Composable
private fun HomeUiState.Ready.appControlSummary(): String {
    val directMethods = capabilities.flatMap { it.directControlMethods }.distinct()
    val shizukuStage = capabilities
        .flatMap { it.setupStatuses }
        .firstOrNull { it.method == ControlMethod.SHIZUKU_RPC }
        ?.stage
    return when {
        capabilities.any { it.state is ControlState.Active } ->
            stringResource(R.string.home_summary_active)
        capabilities.any { it.state is ControlState.PendingConditions } ->
            stringResource(R.string.home_summary_pending)
        capabilities.any { it.state is ControlState.Engaged } ->
            stringResource(R.string.home_summary_engaged)
        directMethods.isNotEmpty() ->
            stringResource(R.string.home_summary_ready)
        shizukuStage == ControlSetupStage.NOT_INSTALLED ->
            stringResource(R.string.home_summary_install_shizuku)
        shizukuStage == ControlSetupStage.INSTALLED_NOT_RUNNING ->
            stringResource(R.string.home_summary_start_shizuku)
        shizukuStage == ControlSetupStage.PERMISSION_REQUIRED ->
            stringResource(R.string.home_summary_grant_shizuku)
        capabilities.any { ControlMethod.SHIZUKU_RPC in it.setupMethods } ->
            stringResource(R.string.home_summary_can_control_after_setup)
        else ->
            stringResource(R.string.home_summary_no_backend)
    }
}

@Composable
private fun HomeUiState.Ready.appControlSteps(): List<String> {
    if (capabilities.isEmpty()) {
        return listOf(
            stringResource(R.string.home_empty_step_brands),
            stringResource(R.string.home_empty_step_manual),
        )
    }

    val hasAppControlRoute = capabilities.any {
        it.directControlMethods.isNotEmpty() || it.setupMethods.isNotEmpty()
    }
    if (hasAppControlRoute) {
        return emptyList()
    }

    val fallbackStep = stringResource(R.string.home_fallback_step_only_when_needed)
    val historyStep = stringResource(R.string.home_fallback_step_history)
    return buildList {
        if (capabilities.any { it.descriptor.officialIntent != null }) {
            add(fallbackStep)
        }
        add(historyStep)
    }
}

@Composable
private fun HomeStatusMessage.displayText(): String {
    val methodName = method?.displayName() ?: stringResource(R.string.home_method_write_settings)
    return when (type) {
        HomeStatusType.ControlSuccess -> stringResource(R.string.home_status_control_success_format, methodName)
        HomeStatusType.OpenedSettings -> stringResource(R.string.home_status_opened_settings_format, methodName)
        HomeStatusType.GrantWriteSettings -> stringResource(R.string.home_status_grant_write_settings)
        HomeStatusType.ShizukuNotRunning -> stringResource(R.string.home_status_shizuku_not_running)
        HomeStatusType.ShizukuPermissionRequired -> stringResource(R.string.home_status_shizuku_permission_required)
        HomeStatusType.ControlFailed -> stringResource(
            R.string.home_status_control_failed_format,
            methodName,
            reasonName.orEmpty(),
        )
        HomeStatusType.SetupOpenedPermissionPage -> stringResource(R.string.home_status_setup_permission_format, methodName)
        HomeStatusType.SetupOpenedInstalledApp -> stringResource(R.string.home_status_setup_app_format, methodName)
        HomeStatusType.SetupOpenedInstaller -> stringResource(R.string.home_status_setup_installer_format, methodName)
        HomeStatusType.SetupRequestedPermission -> stringResource(
            R.string.home_status_setup_requested_permission_format,
            methodName,
        )
        HomeStatusType.SetupAlreadyReady -> stringResource(R.string.home_status_setup_ready_format, methodName)
        HomeStatusType.SetupUnsupported -> stringResource(R.string.home_status_setup_unsupported_format, methodName)
        HomeStatusType.SetupFailed -> stringResource(R.string.home_status_setup_failed_format, methodName)
        HomeStatusType.SamsungClassificationOpened -> stringResource(R.string.home_status_samsung_classification_opened)
        HomeStatusType.SamsungClassificationInstallerOpened -> stringResource(
            R.string.home_status_samsung_classification_installer,
        )
        HomeStatusType.SamsungClassificationOpenFailed -> stringResource(
            R.string.home_status_samsung_classification_failed,
        )
        HomeStatusType.SamsungGamingHubOpened -> stringResource(R.string.home_status_samsung_gaming_hub_opened)
        HomeStatusType.SamsungGamingHubOpenFailed -> stringResource(R.string.home_status_samsung_gaming_hub_failed)
    }
}

@Composable
private fun ControlMethod.displayName(): String = when (this) {
    ControlMethod.OFFICIAL_GUIDANCE -> stringResource(R.string.home_method_official)
    ControlMethod.WRITE_SETTINGS_KEY -> stringResource(R.string.home_method_write_settings)
    ControlMethod.SHIZUKU_RPC -> stringResource(R.string.home_method_shizuku)
    ControlMethod.ROOT_SHELL -> stringResource(R.string.home_method_root)
}
