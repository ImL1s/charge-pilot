package com.chargepilot.core.domain

import com.chargepilot.core.model.Precondition

/**
 * Evaluates a list of [Precondition]s against the current device runtime state.
 *
 * Implementation lives in higher-level modules that have access to BatteryManager
 * (`:core:battery`), foreground app detection (`:core:foreground`), etc.
 */
fun interface PreconditionChecker {
    suspend fun check(preconditions: List<Precondition>): Evaluation
}

data class Evaluation(
    val unmet: List<Precondition>,
) {
    val allMet: Boolean get() = unmet.isEmpty()
}
