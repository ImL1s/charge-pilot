package com.chargepilot.feature.home.impl

import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.ControlState
import com.chargepilot.core.model.DeviceProfile

/** Single source of truth for the home screen, produced by [HomeViewModel]. */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(
        val profile: DeviceProfile,
        val capabilities: List<CapabilityRow>,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

data class CapabilityRow(
    val descriptor: CapabilityDescriptor,
    val state: ControlState,
)
