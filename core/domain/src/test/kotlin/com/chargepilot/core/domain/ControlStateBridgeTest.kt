package com.chargepilot.core.domain

import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.Precondition
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ControlStateBridgeTest {

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
            nowEpochMs = { 1_000L },
        )
        assertThat(out).isEqualTo(ControlState.Active(sinceEpochMs = 1_000L))
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
    fun `Unknown remains Unknown`() = runTest {
        val checker = PreconditionChecker { error("should not be called") }
        val out = ControlStateBridge.fromEngaged(
            rawState = ControlState.Unknown,
            preconditions = preconditions,
            checker = checker,
        )
        assertThat(out).isEqualTo(ControlState.Unknown)
    }

    @Test
    fun `preserves Active since when previous state was Active`() = runTest {
        var clock = 5_000L
        val checker = PreconditionChecker { Evaluation(unmet = emptyList()) }
        val first = ControlStateBridge.fromEngaged(
            rawState = ControlState.Engaged,
            preconditions = preconditions,
            checker = checker,
            nowEpochMs = { clock },
        )
        clock = 9_000L
        val second = ControlStateBridge.fromEngaged(
            rawState = ControlState.Engaged,
            preconditions = preconditions,
            checker = checker,
            previousState = first,
            nowEpochMs = { clock },
        )
        assertThat(second).isEqualTo(ControlState.Active(sinceEpochMs = 5_000L))
    }

    @Test
    fun `resets Active since after leaving Active`() = runTest {
        val checker = PreconditionChecker { Evaluation(unmet = emptyList()) }
        val afterPending = ControlStateBridge.fromEngaged(
            rawState = ControlState.Engaged,
            preconditions = preconditions,
            checker = checker,
            previousState = ControlState.PendingConditions(unmet = preconditions),
            nowEpochMs = { 42_000L },
        )
        assertThat(afterPending).isEqualTo(ControlState.Active(sinceEpochMs = 42_000L))
    }
}
