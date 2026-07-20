package com.chargepilot.feature.home.impl

import com.chargepilot.core.domain.ControlStateBridge
import com.chargepilot.core.domain.Evaluation
import com.chargepilot.core.domain.PreconditionChecker
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.Precondition
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * Home-facing coverage of the production [ControlStateBridge] (no local mirror).
 */
class HomeViewModelBridgeTest {

    private val preconditions = listOf(
        Precondition.PdChargerPresent,
        Precondition.BatteryLevelAbove(percent = 20),
    )

    @Test
    fun `Engaged with no unmet preconditions becomes Active`() = runTest {
        val checker = PreconditionChecker { Evaluation(unmet = emptyList()) }
        val out = ControlStateBridge.fromEngaged(
            rawState = ControlState.Engaged,
            preconditions = preconditions,
            checker = checker,
            nowEpochMs = { 0L },
        )
        assertThat(out).isInstanceOf(ControlState.Active::class.java)
    }

    @Test
    fun `Engaged with unmet preconditions becomes PendingConditions`() = runTest {
        val checker = PreconditionChecker { Evaluation(unmet = preconditions) }
        val out = ControlStateBridge.fromEngaged(
            rawState = ControlState.Engaged,
            preconditions = preconditions,
            checker = checker,
        )
        assertThat(out).isInstanceOf(ControlState.PendingConditions::class.java)
        val pending = out as ControlState.PendingConditions
        assertThat(pending.unmet).hasSize(2)
    }

    @Test
    fun `Inactive remains Inactive`() = runTest {
        val checker = PreconditionChecker { error("should not be called") }
        val out = ControlStateBridge.fromEngaged(
            rawState = ControlState.Inactive,
            preconditions = preconditions,
            checker = checker,
        )
        assertThat(out).isEqualTo(ControlState.Inactive)
    }

    @Test
    fun `preserves Active timestamp across refresh`() = runTest {
        val checker = PreconditionChecker { Evaluation(unmet = emptyList()) }
        val first = ControlStateBridge.fromEngaged(
            rawState = ControlState.Engaged,
            preconditions = preconditions,
            checker = checker,
            nowEpochMs = { 100L },
        )
        val second = ControlStateBridge.fromEngaged(
            rawState = ControlState.Engaged,
            preconditions = preconditions,
            checker = checker,
            previousState = first,
            nowEpochMs = { 999L },
        )
        assertThat(second).isEqualTo(ControlState.Active(sinceEpochMs = 100L))
    }
}
