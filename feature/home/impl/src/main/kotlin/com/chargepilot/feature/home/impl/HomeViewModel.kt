package com.chargepilot.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargepilot.core.capability.CapabilityRegistry
import com.chargepilot.core.common.IoDispatcher
import com.chargepilot.core.control.ControlOrchestrator
import com.chargepilot.core.device.DeviceDetector
import com.chargepilot.core.domain.PreconditionChecker
import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.ControlState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceDetector: DeviceDetector,
    private val capabilityRegistry: CapabilityRegistry,
    private val controlOrchestrator: ControlOrchestrator,
    private val preconditionChecker: PreconditionChecker,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = withContext(ioDispatcher) {
                runCatching {
                    val profile = deviceDetector.detect()
                    val descriptors = capabilityRegistry.resolve(profile)
                    val rows = descriptors.map { descriptor ->
                        val strategy = controlOrchestrator.pickStrategy(profile, descriptor)
                        val rawState = strategy?.getCurrentState(descriptor) ?: ControlState.Unknown
                        CapabilityRow(
                            descriptor = descriptor,
                            state = bridgeEngagedToActive(rawState, descriptor),
                        )
                    }
                    HomeUiState.Ready(profile = profile, capabilities = rows)
                }.getOrElse { t ->
                    HomeUiState.Error(t.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Bridges [ControlState.Engaged] into either [ControlState.Active] or
     * [ControlState.PendingConditions] using the [PreconditionChecker]. This is the
     * single integration point mandated by the spec — strategies report "key written"
     * (Engaged); the ViewModel decides "feature actually active" (Active).
     */
    private suspend fun bridgeEngagedToActive(
        rawState: ControlState,
        descriptor: CapabilityDescriptor,
    ): ControlState {
        if (rawState !is ControlState.Engaged) return rawState
        val evaluation = preconditionChecker.check(descriptor.preconditions)
        return if (evaluation.allMet) {
            ControlState.Active(sinceEpochMs = System.currentTimeMillis())
        } else {
            ControlState.PendingConditions(unmet = evaluation.unmet)
        }
    }
}
