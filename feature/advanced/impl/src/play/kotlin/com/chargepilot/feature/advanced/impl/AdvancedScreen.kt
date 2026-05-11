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
        text = "Advanced (Shizuku / root) features are only available in the F-Droid / GitHub releases build of Charge Pilot. The Play Store build does not bundle privileged-mode code.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(24.dp),
    )
}
