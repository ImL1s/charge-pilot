package com.chargepilot.feature.about.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Charge Pilot", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Cross-brand charging-capability detector. Open source, AGPL-3.0. No telemetry. All changes reversible.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
