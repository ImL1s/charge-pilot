package com.chargepilot.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargepilot.core.capability.CapabilityRegistry
import com.chargepilot.core.common.IoDispatcher
import com.chargepilot.core.control.ControlOrchestrator
import com.chargepilot.core.control.PrivilegedSetupNavigator
import com.chargepilot.core.control.SamsungGameToolsStatus
import com.chargepilot.core.control.SetupNavigationResult
import com.chargepilot.core.datastore.OperationHistoryDataSource
import com.chargepilot.core.datastore.UserPreferencesDataSource
import com.chargepilot.core.device.DeviceDetector
import com.chargepilot.core.domain.ControlStateBridge
import com.chargepilot.core.domain.PreconditionChecker
import com.chargepilot.core.foreground.ForegroundDetector
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

sealed interface HomeEffect {
    data class OpenDisclosure(val capabilityId: String) : HomeEffect
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceDetector: DeviceDetector,
    private val capabilityRegistry: CapabilityRegistry,
    private val controlOrchestrator: ControlOrchestrator,
    private val privilegedSetupNavigator: PrivilegedSetupNavigator,
    private val operationHistory: OperationHistoryDataSource,
    private val userPreferences: UserPreferencesDataSource,
    private val preconditionChecker: PreconditionChecker,
    private val foregroundDetector: ForegroundDetector,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<HomeEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<HomeEffect> = _effects.asSharedFlow()

    private var refreshJob: Job? = null
    private var pendingEnableId: String? = null

    init {
        load()
    }

    fun refresh() {
        val ready = _state.value as? HomeUiState.Ready
        if (ready == null) {
            load()
            return
        }
        refreshJob?.cancel()
        val generation = ready
        refreshJob = viewModelScope.launch {
            val updatedRows = withContext(ioDispatcher) {
                generation.capabilities.map { row ->
                    buildCapabilityRow(generation.profile, row.descriptor, row.state)
                }
            }
            val latest = _state.value as? HomeUiState.Ready ?: return@launch
            _state.value = latest.copy(
                capabilities = updatedRows,
                samsungGameSetup = buildSamsungGameSetup(
                    generation.profile,
                    updatedRows.map { it.descriptor },
                ),
                // Preserve a status message set by a newer user action during refresh.
                statusMessage = latest.statusMessage ?: generation.statusMessage,
            )
            maybeCompletePendingEnable()
        }
    }

    /** Whether any row needs live precondition/key polling while the screen is STARTED. */
    fun needsLivePolling(): Boolean {
        val ready = _state.value as? HomeUiState.Ready ?: return false
        return ready.capabilities.any { row ->
            row.state is ControlState.Active ||
                row.state is ControlState.PendingConditions ||
                row.state is ControlState.Engaged
        }
    }

    private fun load() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.value = withContext(ioDispatcher) {
                runCatching {
                    val profile = deviceDetector.detect()
                    val descriptors = capabilityRegistry.resolve(profile)
                    val rows = descriptors.map { descriptor ->
                        buildCapabilityRow(profile, descriptor, previousState = null)
                    }
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
        // Never fall through to write strategies for official guidance.
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
        val enabling = !row.state.isConfiguredOn()
        if (enabling) {
            viewModelScope.launch {
                if (!userPreferences.hasAcknowledged(capabilityId)) {
                    pendingEnableId = capabilityId
                    _effects.emit(HomeEffect.OpenDisclosure(capabilityId))
                    return@launch
                }
                runCapabilityAction(
                    capabilityId = capabilityId,
                    method = method,
                    enabled = true,
                )
            }
            return
        }
        runCapabilityAction(
            capabilityId = capabilityId,
            method = method,
            enabled = false,
        )
    }

    private suspend fun maybeCompletePendingEnable() {
        val id = pendingEnableId ?: return
        if (!userPreferences.hasAcknowledged(id)) return
        pendingEnableId = null
        val ready = _state.value as? HomeUiState.Ready ?: return
        val row = ready.capabilities.firstOrNull { it.descriptor.id == id } ?: return
        val method = row.directControlMethods.firstOrNull() ?: return
        if (!row.state.isConfiguredOn()) {
            runCapabilityAction(capabilityId = id, method = method, enabled = true)
        }
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
                Timber.w(throwable, "Failed to persist setup history")
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

    fun openUsageAccessSettings() {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                privilegedSetupNavigator.openUsageAccessSettings()
            }
            val statusMessage = if (result == SetupNavigationResult.OpenedPermissionPage) {
                HomeStatusMessage(HomeStatusType.UsageAccessOpened)
            } else {
                HomeStatusMessage(HomeStatusType.UsageAccessOpenFailed)
            }
            refreshHome(statusMessage)
        }
    }

    fun needsUsageAccessEducation(): Boolean {
        val ready = _state.value as? HomeUiState.Ready ?: return false
        if (ready.profile.manufacturer != Manufacturer.SAMSUNG) return false
        if (ready.capabilities.none { it.descriptor.type == CapabilityType.PAUSE_PD_DURING_GAMING }) {
            return false
        }
        return !foregroundDetector.hasUsageAccess()
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
            val (result, rawBefore, rawAfter) = withContext(ioDispatcher) {
                val strategy = when {
                    method == ControlMethod.OFFICIAL_GUIDANCE ->
                        controlOrchestrator.strategyFor(ControlMethod.OFFICIAL_GUIDANCE)
                    method.isDirectControl() -> controlOrchestrator.strategyFor(method)
                    else -> controlOrchestrator.pickStrategy(
                        profile = ready.profile,
                        descriptor = descriptor,
                        userPreference = method,
                    )
                } ?: return@withContext Triple(
                    ControlResult.Failed(FailureReason.UNSUPPORTED_DEVICE),
                    null as String?,
                    null as String?,
                )
                val beforeRaw = if (method.isDirectControl()) {
                    when (val state = strategy.getCurrentState(descriptor)) {
                        ControlState.Inactive -> "0"
                        ControlState.Engaged -> "1"
                        is ControlState.Active -> "1"
                        is ControlState.PendingConditions -> "1"
                        else -> null
                    }
                } else {
                    null
                }
                val writeResult = strategy.setEnabled(descriptor, enabled = enabled)
                val afterRaw = if (method.isDirectControl() && writeResult is ControlResult.Success) {
                    when (val state = strategy.getCurrentState(descriptor)) {
                        ControlState.Inactive -> "0"
                        ControlState.Engaged -> "1"
                        is ControlState.Active -> "1"
                        is ControlState.PendingConditions -> "1"
                        else -> if (enabled) "1" else "0"
                    }
                } else {
                    null
                }
                Triple(writeResult, beforeRaw, afterRaw)
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
                        rawBefore = rawBefore,
                        rawAfter = rawAfter,
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
                Timber.w(throwable, "Failed to persist operation history")
            }
            Timber.d("Capability action method=%s id=%s result=%s", method, capabilityId, result)
            refreshCapabilityState(
                capabilityId,
                setupResult?.toStatusMessage(method) ?: result.toStatusMessage(method),
            )
        }
    }

    private fun refreshCapabilityState(capabilityId: String, statusMessage: HomeStatusMessage? = null) {
        val ready = _state.value as? HomeUiState.Ready ?: return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val updatedRows = withContext(ioDispatcher) {
                ready.capabilities.map { row ->
                    if (row.descriptor.id != capabilityId) return@map row
                    buildCapabilityRow(ready.profile, row.descriptor, row.state)
                }
            }
            val latest = _state.value as? HomeUiState.Ready ?: return@launch
            _state.value = latest.copy(
                capabilities = updatedRows,
                samsungGameSetup = buildSamsungGameSetup(
                    ready.profile,
                    updatedRows.map { it.descriptor },
                ),
                statusMessage = statusMessage,
            )
        }
    }

    private fun refreshHome(statusMessage: HomeStatusMessage? = null) {
        val ready = _state.value as? HomeUiState.Ready ?: return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val updatedRows = withContext(ioDispatcher) {
                ready.capabilities.map { row ->
                    buildCapabilityRow(ready.profile, row.descriptor, row.state)
                }
            }
            val latest = _state.value as? HomeUiState.Ready ?: return@launch
            _state.value = latest.copy(
                capabilities = updatedRows,
                samsungGameSetup = buildSamsungGameSetup(
                    ready.profile,
                    updatedRows.map { it.descriptor },
                ),
                statusMessage = statusMessage ?: latest.statusMessage,
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
        previousState: ControlState?,
    ): ControlState {
        val preferredStateMethod = directControlMethods.firstOrNull()
        val strategy = preferredStateMethod?.let(controlOrchestrator::strategyFor)
        val rawState = strategy?.getCurrentState(descriptor) ?: ControlState.Unknown
        return ControlStateBridge.fromEngaged(
            rawState = rawState,
            preconditions = descriptor.preconditions,
            checker = preconditionChecker,
            previousState = previousState,
        )
    }

    private suspend fun buildCapabilityRow(
        profile: DeviceProfile,
        descriptor: CapabilityDescriptor,
        previousState: ControlState?,
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
            state = readDisplayState(profile, descriptor, directMethods, previousState),
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
        rawBefore: String? = null,
        rawAfter: String? = null,
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
            capabilityId = descriptor.id,
            settingsKeyName = descriptor.settingsKey?.key,
            rawBefore = rawBefore,
            rawAfter = rawAfter,
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
            capabilityId = descriptor.id,
            settingsKeyName = descriptor.settingsKey?.key,
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
        is Precondition.BatteryLevelAbove -> "battery at least $percent%"
        is Precondition.GameInForeground -> "supported game foreground"
    }
}
