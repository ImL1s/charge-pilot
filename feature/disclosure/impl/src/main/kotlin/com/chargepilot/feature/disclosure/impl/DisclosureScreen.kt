package com.chargepilot.feature.disclosure.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chargepilot.core.ui.InfoCard
import com.chargepilot.core.ui.StepList

@Composable
fun DisclosureScreen(capabilityId: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoCard(
            title = stringResource(R.string.disclosure_title),
            body = stringResource(R.string.disclosure_body_format, capabilityId),
        )
        InfoCard(
            title = stringResource(R.string.disclosure_local_title),
            body = stringResource(R.string.disclosure_local_body),
        )
        InfoCard(
            title = stringResource(R.string.disclosure_safety_title),
            body = stringResource(R.string.disclosure_step_user_initiated),
        ) {
            StepList(
                steps = listOf(
                    stringResource(R.string.disclosure_step_preconditions),
                    stringResource(R.string.disclosure_step_shizuku),
                    stringResource(R.string.disclosure_step_samsung),
                ),
            )
        }
    }
}
