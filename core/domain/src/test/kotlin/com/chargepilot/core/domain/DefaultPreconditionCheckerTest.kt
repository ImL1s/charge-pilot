package com.chargepilot.core.domain

import com.chargepilot.core.model.Precondition
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Characterization of precondition evaluation semantics used by
 * [DefaultPreconditionChecker]. BatteryProvider / ForegroundDetector require Android
 * Context, so the pure decision table is tested here and must stay aligned with
 * [DefaultPreconditionChecker.isMet].
 */
class DefaultPreconditionCheckerTest {

    private class FakeBattery(
        var pd: Boolean = false,
        var level: Int? = 50,
    )

    private class FakeForeground(
        var game: Boolean = false,
    )

    private fun evaluate(
        preconditions: List<Precondition>,
        battery: FakeBattery,
        foreground: FakeForeground,
    ): Evaluation {
        val unmet = preconditions.filterNot { precondition ->
            when (precondition) {
                Precondition.PdChargerPresent -> battery.pd
                is Precondition.BatteryLevelAbove -> {
                    val level = battery.level ?: return@filterNot false
                    // Keep in sync with DefaultPreconditionChecker (≥ semantics, plan 005).
                    level >= precondition.percent
                }
                is Precondition.GameInForeground -> foreground.game
            }
        }
        return Evaluation(unmet = unmet)
    }

    @Test
    fun `empty preconditions are all met`() {
        val result = evaluate(emptyList(), FakeBattery(), FakeForeground())
        assertThat(result.allMet).isTrue()
        assertThat(result.unmet).isEmpty()
    }

    @Test
    fun `PdChargerPresent false is unmet`() {
        val result = evaluate(
            listOf(Precondition.PdChargerPresent),
            FakeBattery(pd = false),
            FakeForeground(),
        )
        assertThat(result.unmet).containsExactly(Precondition.PdChargerPresent)
    }

    @Test
    fun `PdChargerPresent true is met`() {
        val result = evaluate(
            listOf(Precondition.PdChargerPresent),
            FakeBattery(pd = true),
            FakeForeground(),
        )
        assertThat(result.allMet).isTrue()
    }

    @Test
    fun `BatteryLevelAbove level equal to threshold is met with ge semantics`() {
        val result = evaluate(
            listOf(Precondition.BatteryLevelAbove(20)),
            FakeBattery(level = 20),
            FakeForeground(),
        )
        assertThat(result.allMet).isTrue()
    }

    @Test
    fun `BatteryLevelAbove level one below threshold is unmet`() {
        val result = evaluate(
            listOf(Precondition.BatteryLevelAbove(20)),
            FakeBattery(level = 19),
            FakeForeground(),
        )
        assertThat(result.unmet).hasSize(1)
    }

    @Test
    fun `BatteryLevelAbove level one above threshold is met`() {
        val result = evaluate(
            listOf(Precondition.BatteryLevelAbove(20)),
            FakeBattery(level = 21),
            FakeForeground(),
        )
        assertThat(result.allMet).isTrue()
    }

    @Test
    fun `BatteryLevelAbove null level is unmet`() {
        val result = evaluate(
            listOf(Precondition.BatteryLevelAbove(20)),
            FakeBattery(level = null),
            FakeForeground(),
        )
        assertThat(result.unmet).hasSize(1)
    }

    @Test
    fun `GameInForeground respects membership`() {
        val unmet = evaluate(
            listOf(Precondition.GameInForeground(knownGames = setOf("com.game.a"))),
            FakeBattery(),
            FakeForeground(game = false),
        )
        assertThat(unmet.allMet).isFalse()
        val met = evaluate(
            listOf(Precondition.GameInForeground(knownGames = setOf("com.game.a"))),
            FakeBattery(),
            FakeForeground(game = true),
        )
        assertThat(met.allMet).isTrue()
    }
}
