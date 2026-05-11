package com.chargepilot.feature.history.impl

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HistoryScreen() {
    Text(
        text = "Operation history (coming soon)",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(24.dp),
    )
}
