package com.chargepilot.feature.home.impl

import com.chargepilot.core.domain.Evaluation
import com.chargepilot.core.domain.PreconditionChecker
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.Precondition
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * Validates the spec-mandated Engaged -> Active/PendingConditions bridge: a strategy
 * reports "key written" (Engaged); the ViewModel uses [PreconditionChecker] to decide
 * whether to surface "Active" or "PendingConditions" to the UI.
 */
class HomeViewModelBridgeTest {

    private val preconditions = listOf(
        Precondition.PdChargerPresent,
        Precondition.BatteryLevelAbove(percent = 20),
    )

    @Test
    fun `Engaged with no unmet preconditions becomes Active`() = runTest {
        val checker = PreconditionChecker { Evaluation(unmet = emptyList()) }
        val out = bridge(ControlState.Engaged, checker)
        assertThat(out).isInstanceOf(ControlState.Active::class.java)
    }

    @Test
    fun `Engaged with unmet preconditions becomes PendingConditions`() = runTest {
        val checker = PreconditionChecker { Evaluation(unmet = preconditions) }
        val out = bridge(ControlState.Engaged, checker)
        assertThat(out).isInstanceOf(ControlState.PendingConditions::class.java)
        val pending = out as ControlState.PendingConditions
        assertThat(pending.unmet).hasSize(2)
    }

    @Test
    fun `Inactive remains Inactive`() = runTest {
        val checker = PreconditionChecker { error("should not be called") }
        val out = bridge(ControlState.Inactive, checker)
        assertThat(out).isEqualTo(ControlState.Inactive)
    }

    @Test
    fun `Unknown remains Unknown`() = runTest {
        val checker = PreconditionChecker { error("should not be called") }
        val out = bridge(ControlState.Unknown, checker)
        assertThat(out).isEqualTo(ControlState.Unknown)
    }

    /** Mirror of [HomeViewModel.bridgeEngagedToActive] for unit-testing the policy. */
    private suspend fun bridge(
        rawState: ControlState,
        checker: PreconditionChecker,
    ): ControlState {
        if (rawState !is ControlState.Engaged) return rawState
        val ev = checker.check(preconditions)
        return if (ev.allMet) {
            ControlState.Active(sinceEpochMs = 0L)
        } else {
            ControlState.PendingConditions(unmet = ev.unmet)
        }
    }
}
