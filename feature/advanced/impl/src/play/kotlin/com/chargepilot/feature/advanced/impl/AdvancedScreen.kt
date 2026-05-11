package com.chargepilot.feature.advanced.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chargepilot.core.ui.InfoCard
import com.chargepilot.core.ui.StepList

@Composable
fun AdvancedScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            InfoCard(
                title = stringResource(R.string.advanced_play_title),
                body = stringResource(R.string.advanced_play_body),
            )
        }
        item {
            InfoCard(
                title = stringResource(R.string.advanced_play_samsung_fallback_title),
                body = stringResource(R.string.advanced_play_samsung_fallback_body),
            ) {
                StepList(
                    steps = listOf(
                        stringResource(R.string.advanced_play_step_pd),
                        stringResource(R.string.advanced_play_step_battery),
                        stringResource(R.string.advanced_play_step_game),
                    ),
                )
            }
        }
        item {
            InfoCard(
                title = stringResource(R.string.advanced_play_need_control_title),
                body = stringResource(R.string.advanced_play_need_control_body),
            )
        }
    }
}
