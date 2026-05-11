package com.chargepilot.feature.about.impl

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
fun AboutScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            InfoCard(
                title = stringResource(R.string.about_title_app),
                body = stringResource(R.string.about_body_app),
            )
        }
        item {
            InfoCard(
                title = stringResource(R.string.about_title_privacy),
                body = stringResource(R.string.about_body_privacy),
            ) {
                StepList(
                    steps = listOf(
                        stringResource(R.string.about_step_manual_first),
                        stringResource(R.string.about_step_app_control_condition),
                        stringResource(R.string.about_step_reversible),
                    ),
                )
            }
        }
        item {
            InfoCard(
                title = stringResource(R.string.about_title_build_boundaries),
                body = stringResource(R.string.about_body_build_boundaries),
            )
        }
        item {
            InfoCard(
                title = stringResource(R.string.about_title_license),
                body = stringResource(R.string.about_body_license),
            )
        }
    }
}
