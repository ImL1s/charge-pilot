package com.chargepilot.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chargepilot.core.ui.CapabilityCard
import com.chargepilot.core.ui.DeviceProfileCard

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold { innerPadding ->
        when (val current = state) {
            is HomeUiState.Loading -> CenteredText("Detecting device…", innerPadding)
            is HomeUiState.Error -> CenteredText("Error: ${current.message}", innerPadding)
            is HomeUiState.Ready -> ReadyContent(state = current, innerPadding = innerPadding)
        }
    }
}

@Composable
private fun CenteredText(text: String, padding: PaddingValues) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
    )
}

@Composable
private fun ReadyContent(state: HomeUiState.Ready, innerPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DeviceProfileCard(profile = state.profile) }
        if (state.capabilities.isEmpty()) {
            item {
                Text(
                    text = "No registered capabilities for this device. Try the Brands page to learn more.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(items = state.capabilities, key = { it.descriptor.id }) { row ->
            CapabilityCard(
                descriptor = row.descriptor,
                state = row.state,
                onTryClick = { /* wired up in disclosure flow task */ },
                onOpenOfficialClick = { /* wired up in disclosure flow task */ },
            )
        }
    }
}
