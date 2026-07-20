package com.chargepilot.feature.home.impl

import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.ControlSetupStatus
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.DeviceProfile

/** Single source of truth for the home screen, produced by [HomeViewModel]. */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(
        val profile: DeviceProfile,
        val capabilities: List<CapabilityRow>,
        val samsungGameSetup: SamsungGameSetupUiState? = null,
        val statusMessage: HomeStatusMessage? = null,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

data class CapabilityRow(
    val descriptor: CapabilityDescriptor,
    val state: ControlState,
    val directControlMethods: List<ControlMethod> = emptyList(),
    val setupMethods: List<ControlMethod> = emptyList(),
    val setupStatuses: List<ControlSetupStatus> = emptyList(),
)

data class HomeStatusMessage(
    val type: HomeStatusType,
    val method: ControlMethod? = null,
    val reasonName: String? = null,
)

enum class HomeStatusType {
    ControlSuccess,
    OpenedSettings,
    GrantWriteSettings,
    ShizukuNotRunning,
    ShizukuPermissionRequired,
    ControlFailed,
    SetupOpenedPermissionPage,
    SetupOpenedInstalledApp,
    SetupOpenedInstaller,
    SetupRequestedPermission,
    SetupAlreadyReady,
    SetupUnsupported,
    SetupFailed,
    SamsungClassificationOpened,
    SamsungClassificationInstallerOpened,
    SamsungClassificationOpenFailed,
    SamsungGamingHubOpened,
    SamsungGamingHubOpenFailed,
    UsageAccessOpened,
    UsageAccessOpenFailed,
}

data class SamsungGameSetupUiState(
    val gamingHubInstalled: Boolean,
    val gameBoosterInstalled: Boolean,
    val gameOptimizingServiceInstalled: Boolean,
    val goodLockInstalled: Boolean,
    val gamePluginsInstalled: Boolean,
    val gameBoosterPlusInstalled: Boolean,
    val classificationBlockedReason: String? = null,
) {
    val systemGameStackReady: Boolean =
        gamingHubInstalled && gameBoosterInstalled && gameOptimizingServiceInstalled
    val classificationToolInstalled: Boolean = gamePluginsInstalled && gameBoosterPlusInstalled
    val classificationToolReady: Boolean =
        classificationToolInstalled && classificationBlockedReason == null
    val primaryAction: SamsungGamePrimaryAction =
        when {
            !gamePluginsInstalled -> SamsungGamePrimaryAction.InstallGamePlugins
            !gameBoosterPlusInstalled -> SamsungGamePrimaryAction.InstallGameBoosterPlus
            classificationBlockedReason != null -> SamsungGamePrimaryAction.OpenGameBoosterPlusAnyway
            else -> SamsungGamePrimaryAction.OpenClassification
        }
    val primaryActionLabel: String =
        when (primaryAction) {
            SamsungGamePrimaryAction.InstallGamePlugins -> "Install Game Plugins"
            SamsungGamePrimaryAction.InstallGameBoosterPlus -> "Install Game Booster+"
            SamsungGamePrimaryAction.OpenGameBoosterPlusAnyway -> "Open Game Booster+ anyway"
            SamsungGamePrimaryAction.OpenClassification -> "Open game classification"
        }
}

enum class SamsungGamePrimaryAction {
    InstallGamePlugins,
    InstallGameBoosterPlus,
    OpenGameBoosterPlusAnyway,
    OpenClassification,
}

const val S24U_GOS_LITE_BLOCK_REASON: String =
    "This S24 Ultra One UI 8 build returned “Unable to run: GOS Lite”; normal-app game classification is blocked on this firmware."
