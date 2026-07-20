package com.chargepilot.core.domain

import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.Precondition
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin domain helper for control-state bridging used by Home / History.
 * Full row-building and orchestrator I/O remain in feature ViewModels until a larger
 * extraction (plan 022) lands; this keeps the Engaged→Active policy in one place.
 */
@Singleton
class CapabilityControlInteractor @Inject constructor(
    private val preconditionChecker: PreconditionChecker,
) {
    suspend fun displayState(
        rawState: ControlState,
        preconditions: List<Precondition>,
        previousState: ControlState? = null,
    ): ControlState = ControlStateBridge.fromEngaged(
        rawState = rawState,
        preconditions = preconditions,
        checker = preconditionChecker,
        previousState = previousState,
    )
}
