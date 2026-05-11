package com.chargepilot.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chargepilot.core.ui.InfoCard

@Composable
fun SamsungGameSetupCard(
    setup: SamsungGameSetupUiState,
    onOpenClassificationClick: () -> Unit,
    onOpenGamingHubClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoCard(
        title = stringResource(R.string.samsung_game_title),
        body = stringResource(R.string.samsung_game_body),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.samsung_game_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SamsungGameSetupStatusRow(
                label = stringResource(R.string.samsung_game_stack),
                ready = setup.systemGameStackReady,
                readyText = stringResource(R.string.samsung_game_ready),
                missingText = stringResource(R.string.samsung_game_missing),
            )
            SamsungGameSetupStatusRow(
                label = stringResource(R.string.samsung_game_plugins_host),
                ready = setup.gamePluginsInstalled,
                readyText = stringResource(R.string.samsung_game_installed),
                missingText = stringResource(R.string.samsung_game_install_required),
            )
            SamsungGameSetupStatusRow(
                label = stringResource(R.string.samsung_game_booster_plus_module),
                ready = setup.gameBoosterPlusInstalled,
                readyText = stringResource(R.string.samsung_game_installed),
                missingText = stringResource(R.string.samsung_game_install_required),
            )
            setup.classificationBlockedReason?.let { reason ->
                SamsungGameSetupStatusRow(
                    label = stringResource(R.string.samsung_game_compatibility),
                    ready = false,
                    readyText = stringResource(R.string.samsung_game_ready),
                    missingText = stringResource(R.string.samsung_game_blocked),
                )
                Text(
                    text = localizedBlockedReason(reason),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = onOpenClassificationClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = setup.primaryAction.label())
            }
            OutlinedButton(
                onClick = onOpenGamingHubClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.samsung_game_open_hub_booster))
            }
            SamsungGameSetupStatusRow(
                label = stringResource(R.string.samsung_game_good_lock),
                ready = setup.goodLockInstalled,
                readyText = stringResource(R.string.samsung_game_installed),
                missingText = stringResource(R.string.samsung_game_optional),
            )
        }
    }
}

@Composable
private fun SamsungGamePrimaryAction.label(): String = when (this) {
    SamsungGamePrimaryAction.InstallGamePlugins -> stringResource(R.string.samsung_game_action_install_plugins)
    SamsungGamePrimaryAction.InstallGameBoosterPlus -> stringResource(R.string.samsung_game_action_install_booster_plus)
    SamsungGamePrimaryAction.OpenGameBoosterPlusAnyway -> stringResource(R.string.samsung_game_action_open_anyway)
    SamsungGamePrimaryAction.OpenClassification -> stringResource(R.string.samsung_game_action_open_classification)
}

@Composable
private fun localizedBlockedReason(reason: String): String =
    if (reason == S24U_GOS_LITE_BLOCK_REASON) {
        stringResource(R.string.samsung_game_s24u_gos_lite_block)
    } else {
        reason
    }

@Composable
private fun SamsungGameSetupStatusRow(
    label: String,
    ready: Boolean,
    readyText: String,
    missingText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (ready) readyText else missingText,
            style = MaterialTheme.typography.labelMedium,
            color = if (ready) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            },
        )
    }
}
