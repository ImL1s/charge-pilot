package com.chargepilot.core.domain

import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.Precondition

/**
 * Spec-mandated Engaged → Active / PendingConditions bridge.
 *
 * Strategies report "key written" ([ControlState.Engaged]); callers use a
 * [PreconditionChecker] to decide whether the feature is actually [ControlState.Active].
 *
 * When [previousState] is already [ControlState.Active], the `sinceEpochMs` timestamp is
 * preserved across refreshes so the UI does not reset "active since".
 */
object ControlStateBridge {
    suspend fun fromEngaged(
        rawState: ControlState,
        preconditions: List<Precondition>,
        checker: PreconditionChecker,
        previousState: ControlState? = null,
        nowEpochMs: () -> Long = { System.currentTimeMillis() },
    ): ControlState {
        if (rawState !is ControlState.Engaged) return rawState
        val evaluation = checker.check(preconditions)
        return if (evaluation.allMet) {
            val since = (previousState as? ControlState.Active)?.sinceEpochMs ?: nowEpochMs()
            ControlState.Active(sinceEpochMs = since)
        } else {
            ControlState.PendingConditions(unmet = evaluation.unmet)
        }
    }
}
