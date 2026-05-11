package com.chargepilot.feature.advanced.impl

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdvancedScreen() {
    Text(
        text = "Advanced mode (full flavor): Shizuku and root strategy panels go here.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(24.dp),
    )
}
