package com.chargepilot.feature.disclosure.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DisclosureScreen(capabilityId: String) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "Disclosure (work in progress)",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(text = "Capability id: $capabilityId", style = MaterialTheme.typography.bodyMedium)
    }
}
