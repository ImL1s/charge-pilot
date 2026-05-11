package com.chargepilot.core.model

import kotlinx.serialization.Serializable

/**
 * A capability's runtime state. The UI MUST distinguish [Active] from [PendingConditions]
 * — a write to a settings key may have succeeded while the OEM's preconditions
 * (e.g. PD charger, battery level, game in foreground) are not met.
 */
sealed interface ControlState {
    data object Unknown : ControlState
    data object Inactive : ControlState

    /**
     * The configuration / settings key is set ON, but the OEM's preconditions have not
     * yet been evaluated by the consumer. A strategy MAY return [Engaged] from its
     * `getCurrentState` when it knows the write is in place but cannot itself check
     * preconditions (e.g. WriteSettingsStrategy doesn't talk to BatteryManager).
     *
     * The home view-model is responsible for combining [Engaged] with a
     * `PreconditionChecker` to map to either [PendingConditions] or [Active].
     */
    data object Engaged : ControlState

    data class PendingConditions(val unmet: List<Precondition>) : ControlState
    data class Active(val sinceEpochMs: Long) : ControlState
}

sealed interface ControlResult {
    /** An actual settings write or state change succeeded. */
    data object Success : ControlResult

    /**
     * The user was navigated to an official Settings page; the app could not (or chose
     * not to) write any state itself. The caller MUST NOT treat this as "feature on" —
     * the user has to complete the operation manually in the OEM UI.
     */
    data object NavigatedToSettings : ControlResult

    data class Failed(val reason: FailureReason) : ControlResult
}

enum class FailureReason {
    UNSUPPORTED_DEVICE,
    SPECIAL_ACCESS_NOT_GRANTED,
    KEY_NOT_FOUND,
    KEY_NOT_WRITABLE,
    SHIZUKU_NOT_RUNNING,
    SHIZUKU_PERMISSION_DENIED,
    ROOT_DENIED,
    PRECONDITIONS_NOT_MET,
    UNKNOWN,
}

enum class ControlSetupStage {
    NOT_INSTALLED,
    INSTALLED_NOT_RUNNING,
    PERMISSION_REQUIRED,
    READY,
    UNAVAILABLE,
    UNSUPPORTED,
}

data class ControlSetupStatus(
    val method: ControlMethod,
    val stage: ControlSetupStage,
)

/**
 * Metadata about a single user-initiated operation, persisted to local history so the user
 * can audit and revert. **Never sent off-device.**
 */
@Serializable
data class OperationRecord(
    val id: String,
    val capability: CapabilityType,
    val method: ControlMethod,
    val before: String?,
    val after: String?,
    val success: Boolean,
    val timestampMs: Long,
)
