package com.chargepilot.feature.home.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargepilot.core.capability.CapabilityRegistry
import com.chargepilot.core.common.IoDispatcher
import com.chargepilot.core.control.ControlOrchestrator
import com.chargepilot.core.control.PrivilegedSetupNavigator
import com.chargepilot.core.control.SamsungGameToolsStatus
import com.chargepilot.core.control.SetupNavigationResult
import com.chargepilot.core.datastore.OperationHistoryDataSource
import com.chargepilot.core.device.DeviceDetector
import com.chargepilot.core.domain.PreconditionChecker
import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.CapabilityType
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlResult
import com.chargepilot.core.model.ControlSetupStage
import com.chargepilot.core.model.ControlSetupStatus
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.DeviceProfile
import com.chargepilot.core.model.FailureReason
import com.chargepilot.core.model.Manufacturer
import com.chargepilot.core.model.OperationRecord
import com.chargepilot.core.model.Precondition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceDetector: DeviceDetector,
    private val capabilityRegistry: CapabilityRegistry,
    private val controlOrchestrator: ControlOrchestrator,
    private val privilegedSetupNavigator: PrivilegedSetupNavigator,
    private val operationHistory: OperationHistoryDataSource,
    private val preconditionChecker: PreconditionChecker,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        val ready = _state.value as? HomeUiState.Ready
        if (ready == null) {
            load()
            return
        }
        viewModelScope.launch {
            val updatedRows = withContext(ioDispatcher) {
                ready.capabilities.map { row -> buildCapabilityRow(ready.profile, row.descriptor) }
            }
            _state.value = ready.copy(
                capabilities = updatedRows,
                samsungGameSetup = buildSamsungGameSetup(ready.profile, updatedRows.map { it.descriptor }),
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = withContext(ioDispatcher) {
                runCatching {
                    val profile = deviceDetector.detect()
                    val descriptors = capabilityRegistry.resolve(profile)
                    val rows = descriptors.map { descriptor -> buildCapabilityRow(profile, descriptor) }
                    HomeUiState.Ready(
                        profile = profile,
                        capabilities = rows,
                        samsungGameSetup = buildSamsungGameSetup(profile, descriptors),
                    )
                }.getOrElse { t ->
                    HomeUiState.Error(t.message ?: "Unknown error")
                }
            }
        }
    }

    fun openOfficialSettings(capabilityId: String) {
        runCapabilityAction(capabilityId, ControlMethod.OFFICIAL_GUIDANCE)
    }

    fun tryEnable(capabilityId: String) {
        val ready = _state.value as? HomeUiState.Ready ?: return
        val row = ready.capabilities.firstOrNull { it.descriptor.id == capabilityId } ?: return
        val method = row.directControlMethods.firstOrNull()
        if (method == null) {
            setupPrivilegedMethod(capabilityId)
            return
        }
        runCapabilityAction(
            capabilityId = capabilityId,
            method = method,
            enabled = !row.state.isConfiguredOn(),
        )
    }

    fun setupPrivilegedMethod(capabilityId: String) {
        val ready = _state.value as? HomeUiState.Ready ?: return
        val row = ready.capabilities.firstOrNull { it.descriptor.id == capabilityId } ?: return
        val method = row.setupMethods.firstOrNull() ?: return
        viewModelScope.launch {
            val strategy = controlOrchestrator.strategyFor(method)
            val setupStatus = withContext(ioDispatcher) {
                strategy?.setupStatus(ready.profile, row.descriptor)
            }
            val result = if (setupStatus?.stage == ControlSetupStage.PERMISSION_REQUIRED) {
                withContext(ioDispatcher) {
                    strategy?.requestSetupAuthorization(row.descriptor)
                        ?: SetupNavigationResult.Unsupported
                }
            } else {
                privilegedSetupNavigator.openSetup(method)
            }
            runCatching {
                operationHistory.append(
                    setupRecord(
                        descriptor = row.descriptor,
                        method = method,
                        result = result,
                    ),
                )
            }.onFailure { throwable ->
                Log.w("ChargePilotHome", "Failed to persist setup history", throwable)
            }
            refreshCapabilityState(capabilityId, result.toStatusMessage(method))
        }
    }

    fun openSamsungGameClassificationSetup() {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                privilegedSetupNavigator.openSamsungGameClassificationSetup()
            }
            val statusMessage = when (result) {
                SetupNavigationResult.OpenedInstalledApp ->
                    HomeStatusMessage(HomeStatusType.SamsungClassificationOpened)
                SetupNavigationResult.OpenedInstaller ->
                    HomeStatusMessage(HomeStatusType.SamsungClassificationInstallerOpened)
                else -> HomeStatusMessage(HomeStatusType.SamsungClassificationOpenFailed)
            }
            refreshHome(statusMessage)
        }
    }

    fun openSamsungGamingHub() {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                privilegedSetupNavigator.openSamsungGamingHub()
            }
            val statusMessage = if (result == SetupNavigationResult.OpenedInstalledApp) {
                HomeStatusMessage(HomeStatusType.SamsungGamingHubOpened)
            } else {
                HomeStatusMessage(HomeStatusType.SamsungGamingHubOpenFailed)
            }
            refreshHome(statusMessage)
        }
    }

    private fun runCapabilityAction(
        capabilityId: String,
        method: ControlMethod,
        enabled: Boolean = true,
    ) {
        val ready = _state.value as? HomeUiState.Ready ?: return
        val row = ready.capabilities.firstOrNull { it.descriptor.id == capabilityId } ?: return
        val descriptor = row.descriptor
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                val strategy = if (method.isDirectControl()) {
                    controlOrchestrator.strategyFor(method)
                } else {
                    controlOrchestrator.pickStrategy(
                        profile = ready.profile,
                        descriptor = descriptor,
                        userPreference = method,
                    )
                } ?: return@withContext ControlResult.Failed(FailureReason.UNSUPPORTED_DEVICE)
                strategy.setEnabled(descriptor, enabled = enabled)
            }
            val setupResult = when {
                method == ControlMethod.WRITE_SETTINGS_KEY &&
                    result is ControlResult.Failed &&
                    result.reason == FailureReason.SPECIAL_ACCESS_NOT_GRANTED ->
                    privilegedSetupNavigator.openSetup(method)
                else -> null
            }
            runCatching {
                operationHistory.append(
                    actionRecord(
                        descriptor = descriptor,
                        method = method,
                        before = row.state,
                        result = result,
                        enabled = enabled,
                    ),
                )
                if (setupResult != null) {
                    operationHistory.append(
                        setupRecord(
                            descriptor = descriptor,
                            method = method,
                            result = setupResult,
                        ),
                    )
                }
            }.onFailure { throwable ->
                Log.w("ChargePilotHome", "Failed to persist operation history", throwable)
            }
            Log.i("ChargePilotHome", "Capability action method=$method id=$capabilityId result=$result")
            refreshCapabilityState(
                capabilityId,
                setupResult?.toStatusMessage(method) ?: result.toStatusMessage(method),
            )
        }
    }

    private fun refreshCapabilityState(capabilityId: String, statusMessage: HomeStatusMessage? = null) {
        val ready = _state.value as? HomeUiState.Ready ?: return
        viewModelScope.launch {
            val updatedRows = withContext(ioDispatcher) {
                ready.capabilities.map { row ->
                    if (row.descriptor.id != capabilityId) return@map row
                    buildCapabilityRow(ready.profile, row.descriptor)
                }
            }
            _state.value = ready.copy(
                capabilities = updatedRows,
                samsungGameSetup = buildSamsungGameSetup(ready.profile, updatedRows.map { it.descriptor }),
                statusMessage = statusMessage,
            )
        }
    }

    private fun refreshHome(statusMessage: HomeStatusMessage? = null) {
        val ready = _state.value as? HomeUiState.Ready ?: return
        viewModelScope.launch {
            val updatedRows = withContext(ioDispatcher) {
                ready.capabilities.map { row -> buildCapabilityRow(ready.profile, row.descriptor) }
            }
            _state.value = ready.copy(
                capabilities = updatedRows,
                samsungGameSetup = buildSamsungGameSetup(ready.profile, updatedRows.map { it.descriptor }),
                statusMessage = statusMessage,
            )
        }
    }

    private fun ControlResult.toStatusMessage(method: ControlMethod): HomeStatusMessage = when (this) {
        ControlResult.Success -> HomeStatusMessage(HomeStatusType.ControlSuccess, method = method)
        ControlResult.NavigatedToSettings -> HomeStatusMessage(HomeStatusType.OpenedSettings, method = method)
        is ControlResult.Failed -> if (
            method == ControlMethod.WRITE_SETTINGS_KEY &&
            reason == FailureReason.SPECIAL_ACCESS_NOT_GRANTED
        ) {
            HomeStatusMessage(HomeStatusType.GrantWriteSettings, method = method)
        } else if (method == ControlMethod.SHIZUKU_RPC && reason == FailureReason.SHIZUKU_NOT_RUNNING) {
            HomeStatusMessage(HomeStatusType.ShizukuNotRunning, method = method)
        } else if (method == ControlMethod.SHIZUKU_RPC && reason == FailureReason.SHIZUKU_PERMISSION_DENIED) {
            HomeStatusMessage(HomeStatusType.ShizukuPermissionRequired, method = method)
        } else {
            HomeStatusMessage(HomeStatusType.ControlFailed, method = method, reasonName = reason.name)
        }
    }

    private suspend fun readDisplayState(
        profile: DeviceProfile,
        descriptor: CapabilityDescriptor,
        directControlMethods: List<ControlMethod>,
    ): ControlState {
        val preferredStateMethod = directControlMethods.firstOrNull()
        val strategy = preferredStateMethod?.let(controlOrchestrator::strategyFor)
        val rawState = strategy?.getCurrentState(descriptor) ?: ControlState.Unknown
        return bridgeEngagedToActive(rawState, descriptor)
    }

    private suspend fun buildCapabilityRow(
        profile: DeviceProfile,
        descriptor: CapabilityDescriptor,
    ): CapabilityRow {
        val setupStatuses = directSetupStatuses(profile, descriptor)
        val directMethods = setupStatuses
            .filter { it.stage == ControlSetupStage.READY }
            .map { it.method }
        val setupMethods = setupStatuses
            .filter { it.method == ControlMethod.SHIZUKU_RPC && it.stage.needsUserSetup() }
            .map { it.method }
        return CapabilityRow(
            descriptor = descriptor,
            state = readDisplayState(profile, descriptor, directMethods),
            directControlMethods = directMethods,
            setupMethods = setupMethods,
            setupStatuses = setupStatuses,
        )
    }

    private suspend fun directSetupStatuses(
        profile: DeviceProfile,
        descriptor: CapabilityDescriptor,
    ): List<ControlSetupStatus> =
        descriptor.availableMethods.filter { method ->
            method.isDirectControl()
        }.mapNotNull { method ->
            val strategy = controlOrchestrator.strategyFor(method) ?: return@mapNotNull null
            strategy.setupStatus(profile, descriptor)
        }

    /**
     * Bridges [ControlState.Engaged] into either [ControlState.Active] or
     * [ControlState.PendingConditions] using the [PreconditionChecker]. This is the
     * single integration point mandated by the spec — strategies report "key written"
     * (Engaged); the ViewModel decides "feature actually active" (Active).
     */
    private suspend fun bridgeEngagedToActive(
        rawState: ControlState,
        descriptor: CapabilityDescriptor,
    ): ControlState {
        if (rawState !is ControlState.Engaged) return rawState
        val evaluation = preconditionChecker.check(descriptor.preconditions)
        return if (evaluation.allMet) {
            ControlState.Active(sinceEpochMs = System.currentTimeMillis())
        } else {
            ControlState.PendingConditions(unmet = evaluation.unmet)
        }
    }

    private fun ControlMethod.isDirectControl(): Boolean =
        this == ControlMethod.WRITE_SETTINGS_KEY ||
            this == ControlMethod.SHIZUKU_RPC ||
            this == ControlMethod.ROOT_SHELL

    private fun ControlState.isConfiguredOn(): Boolean =
        this is ControlState.Active ||
            this is ControlState.PendingConditions ||
            this is ControlState.Engaged

    private fun ControlSetupStage.needsUserSetup(): Boolean =
        this == ControlSetupStage.NOT_INSTALLED ||
            this == ControlSetupStage.INSTALLED_NOT_RUNNING ||
            this == ControlSetupStage.PERMISSION_REQUIRED

    private fun buildSamsungGameSetup(
        profile: DeviceProfile,
        descriptors: List<CapabilityDescriptor>,
    ): SamsungGameSetupUiState? {
        if (profile.manufacturer != Manufacturer.SAMSUNG) return null
        if (descriptors.none { it.type == CapabilityType.PAUSE_PD_DURING_GAMING }) return null
        val status = privilegedSetupNavigator.samsungGameToolsStatus()
        return status.toUiState(profile)
    }

    private fun SamsungGameToolsStatus.toUiState(profile: DeviceProfile): SamsungGameSetupUiState =
        SamsungGameSetupUiState(
            gamingHubInstalled = gamingHubInstalled,
            gameBoosterInstalled = gameBoosterInstalled,
            gameOptimizingServiceInstalled = gameOptimizingServiceInstalled,
            goodLockInstalled = goodLockInstalled,
            gamePluginsInstalled = gamePluginsInstalled,
            gameBoosterPlusInstalled = gameBoosterPlusInstalled,
            classificationBlockedReason = knownGameBoosterPlusBlock(profile),
        )

    private fun knownGameBoosterPlusBlock(profile: DeviceProfile): String? {
        val isTestedS24UltraBuild =
            profile.model.equals("SM-S9280", ignoreCase = true) &&
                profile.androidApi >= 36 &&
                profile.buildFingerprint.contains("S9280ZHS5CZC1")
        return if (isTestedS24UltraBuild) {
            S24U_GOS_LITE_BLOCK_REASON
        } else {
            null
        }
    }

    private fun SetupNavigationResult.toStatusMessage(method: ControlMethod): HomeStatusMessage =
        when (this) {
            SetupNavigationResult.OpenedPermissionPage ->
                HomeStatusMessage(HomeStatusType.SetupOpenedPermissionPage, method = method)
            SetupNavigationResult.OpenedInstalledApp ->
                HomeStatusMessage(HomeStatusType.SetupOpenedInstalledApp, method = method)
            SetupNavigationResult.OpenedInstaller ->
                HomeStatusMessage(HomeStatusType.SetupOpenedInstaller, method = method)
            SetupNavigationResult.RequestedPermission ->
                HomeStatusMessage(HomeStatusType.SetupRequestedPermission, method = method)
            SetupNavigationResult.AlreadyReady ->
                HomeStatusMessage(HomeStatusType.SetupAlreadyReady, method = method)
            SetupNavigationResult.Unsupported ->
                HomeStatusMessage(HomeStatusType.SetupUnsupported, method = method)
            SetupNavigationResult.Failed ->
                HomeStatusMessage(HomeStatusType.SetupFailed, method = method)
        }

    private fun actionRecord(
        descriptor: CapabilityDescriptor,
        method: ControlMethod,
        before: ControlState,
        result: ControlResult,
        enabled: Boolean,
    ): OperationRecord {
        val timestamp = System.currentTimeMillis()
        return OperationRecord(
            id = "$timestamp-${descriptor.id}-${method.name}",
            capability = descriptor.type,
            method = method,
            before = before.historyLabel(),
            after = result.historyLabel(enabled),
            success = result !is ControlResult.Failed,
            timestampMs = timestamp,
        )
    }

    private fun setupRecord(
        descriptor: CapabilityDescriptor,
        method: ControlMethod,
        result: SetupNavigationResult,
    ): OperationRecord {
        val timestamp = System.currentTimeMillis()
        return OperationRecord(
            id = "$timestamp-${descriptor.id}-${method.name}-setup",
            capability = descriptor.type,
            method = method,
            before = "Setup not started in this session",
            after = result.historyLabel(),
            success = result == SetupNavigationResult.OpenedPermissionPage ||
                result == SetupNavigationResult.OpenedInstalledApp ||
                result == SetupNavigationResult.OpenedInstaller ||
                result == SetupNavigationResult.RequestedPermission ||
                result == SetupNavigationResult.AlreadyReady,
            timestampMs = timestamp,
        )
    }

    private fun ControlState.historyLabel(): String = when (this) {
        ControlState.Unknown -> "Unknown"
        ControlState.Inactive -> "Inactive"
        ControlState.Engaged -> "Configured on; preconditions not checked"
        is ControlState.PendingConditions -> "Configured on; waiting for ${unmet.historySummary()}"
        is ControlState.Active -> "Active"
    }

    private fun ControlResult.historyLabel(enabled: Boolean): String = when (this) {
        ControlResult.Success -> if (enabled) "Requested ON succeeded" else "Requested OFF succeeded"
        ControlResult.NavigatedToSettings -> "Opened official settings for manual change"
        is ControlResult.Failed -> "Failed: ${reason.name}"
    }

    private fun SetupNavigationResult.historyLabel(): String = when (this) {
        SetupNavigationResult.OpenedPermissionPage -> "Opened Android Modify system settings grant page"
        SetupNavigationResult.OpenedInstalledApp -> "Opened installed setup app"
        SetupNavigationResult.OpenedInstaller -> "Opened official install/setup page"
        SetupNavigationResult.RequestedPermission -> "Requested privileged setup permission"
        SetupNavigationResult.AlreadyReady -> "Privileged setup already ready"
        SetupNavigationResult.Unsupported -> "Setup method unsupported in this build"
        SetupNavigationResult.Failed -> "No setup app or install page opened"
    }

    private fun ControlMethod.displayName(): String = when (this) {
        ControlMethod.OFFICIAL_GUIDANCE -> "Official guide"
        ControlMethod.WRITE_SETTINGS_KEY -> "App control"
        ControlMethod.SHIZUKU_RPC -> "Shizuku"
        ControlMethod.ROOT_SHELL -> "Root shell"
    }

    private fun List<Precondition>.historySummary(): String =
        if (isEmpty()) {
            "live requirements"
        } else {
            joinToString { it.historyLabel() }
        }

    private fun Precondition.historyLabel(): String = when (this) {
        Precondition.PdChargerPresent -> "USB PD/PPS charger"
        is Precondition.BatteryLevelAbove -> "battery above $percent%"
        is Precondition.GameInForeground -> "supported game foreground"
    }
}
