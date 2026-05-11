package com.chargepilot.feature.brands.impl

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BrandsScreen() {
    Text(
        text = "Brands matrix (coming soon)",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(24.dp),
    )
}
