package com.chargepilot.core.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chargepilot.core.model.CapabilityDescriptor
import com.chargepilot.core.model.ControlState

/** Visual variant for a [CapabilityCard]. */
enum class CapabilityCardVariant { Unsupported, AvailableIdle, PendingConditions, Active }

@Composable
fun CapabilityCard(
    descriptor: CapabilityDescriptor,
    state: ControlState,
    onTryClick: () -> Unit,
    onOpenOfficialClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val variant = state.toVariant(descriptor)
    val borderColor = when (variant) {
        CapabilityCardVariant.Active -> MaterialTheme.colorScheme.tertiary
        CapabilityCardVariant.PendingConditions -> MaterialTheme.colorScheme.error
        CapabilityCardVariant.Unsupported -> MaterialTheme.colorScheme.outlineVariant
        CapabilityCardVariant.AvailableIdle -> MaterialTheme.colorScheme.outline
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = descriptor.type.name.replace('_', ' '),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Source: ${descriptor.evidence.name.replace('_', ' ')}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            StateLine(state = state)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenOfficialClick) {
                    Text(text = "Open official settings")
                }
                if (variant != CapabilityCardVariant.Unsupported) {
                    OutlinedButton(onClick = onTryClick) {
                        Text(
                            text = when (variant) {
                                CapabilityCardVariant.Active -> "Disable"
                                else -> "Try it"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StateLine(state: ControlState) {
    val (text, color) = when (state) {
        ControlState.Unknown -> "Status: unknown" to MaterialTheme.colorScheme.onSurfaceVariant
        ControlState.Inactive -> "Status: available, not enabled" to MaterialTheme.colorScheme.onSurfaceVariant
        ControlState.Engaged ->
            "Status: configured (preconditions not verified)" to MaterialTheme.colorScheme.tertiary
        is ControlState.PendingConditions ->
            "Status: pending conditions (${state.unmet.size} unmet)" to MaterialTheme.colorScheme.error
        is ControlState.Active -> "Status: active" to MaterialTheme.colorScheme.tertiary
    }
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
}

private fun ControlState.toVariant(descriptor: CapabilityDescriptor): CapabilityCardVariant =
    when {
        descriptor.availableMethods.isEmpty() -> CapabilityCardVariant.Unsupported
        this is ControlState.Active -> CapabilityCardVariant.Active
        this is ControlState.PendingConditions -> CapabilityCardVariant.PendingConditions
        else -> CapabilityCardVariant.AvailableIdle
    }
