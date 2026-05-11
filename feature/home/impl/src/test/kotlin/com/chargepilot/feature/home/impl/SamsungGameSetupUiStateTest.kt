package com.chargepilot.feature.home.impl

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class SamsungGameSetupUiStateTest {

    @Test
    fun `classification requires both Game Plugins host and Game Booster Plus module`() {
        val onlyModule = state(gamePluginsInstalled = false, gameBoosterPlusInstalled = true)
        val onlyHost = state(gamePluginsInstalled = true, gameBoosterPlusInstalled = false)
        val both = state(gamePluginsInstalled = true, gameBoosterPlusInstalled = true)

        assertThat(onlyModule.classificationToolInstalled).isFalse()
        assertThat(onlyModule.primaryActionLabel).isEqualTo("Install Game Plugins")
        assertThat(onlyHost.classificationToolInstalled).isFalse()
        assertThat(onlyHost.primaryActionLabel).isEqualTo("Install Game Booster+")
        assertThat(both.classificationToolInstalled).isTrue()
        assertThat(both.primaryActionLabel).isEqualTo("Open game classification")
    }

    @Test
    fun `blocked firmware keeps install state but changes action label`() {
        val blocked = state(
            gamePluginsInstalled = true,
            gameBoosterPlusInstalled = true,
            classificationBlockedReason = "Unable to run: GOS Lite",
        )

        assertThat(blocked.classificationToolInstalled).isTrue()
        assertThat(blocked.classificationToolReady).isFalse()
        assertThat(blocked.primaryActionLabel).isEqualTo("Open Game Booster+ anyway")
    }

    private fun state(
        gamePluginsInstalled: Boolean,
        gameBoosterPlusInstalled: Boolean,
        classificationBlockedReason: String? = null,
    ) = SamsungGameSetupUiState(
        gamingHubInstalled = true,
        gameBoosterInstalled = true,
        gameOptimizingServiceInstalled = true,
        goodLockInstalled = true,
        gamePluginsInstalled = gamePluginsInstalled,
        gameBoosterPlusInstalled = gameBoosterPlusInstalled,
        classificationBlockedReason = classificationBlockedReason,
    )
}
