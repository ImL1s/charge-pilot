package com.chargepilot.feature.advanced.impl

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chargepilot.core.control.PrivilegedSetupNavigator
import com.chargepilot.core.ui.InfoCard
import com.chargepilot.core.ui.StepList

@Composable
fun AdvancedScreen() {
    val context = LocalContext.current
    val setupNavigator = remember(context) {
        PrivilegedSetupNavigator(context.applicationContext)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            InfoCard(
                title = stringResource(R.string.advanced_full_title),
                body = stringResource(R.string.advanced_full_body),
            )
        }
        item {
            InfoCard(
                title = stringResource(R.string.advanced_shizuku_title),
                body = stringResource(R.string.advanced_shizuku_body),
            ) {
                StepList(
                    steps = listOf(
                        stringResource(R.string.advanced_shizuku_step_install),
                        stringResource(R.string.advanced_shizuku_step_wireless_debugging),
                        stringResource(R.string.advanced_shizuku_step_pair_start),
                        stringResource(R.string.advanced_shizuku_step_grant),
                        stringResource(R.string.advanced_shizuku_step_use_home),
                    ),
                )
            }
        }
        item {
            InfoCard(
                title = stringResource(R.string.advanced_samsung_limit_title),
                body = stringResource(R.string.advanced_samsung_limit_body),
            )
        }
        item {
            Button(
                onClick = { setupNavigator.openShizukuSetup() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.advanced_install_open_shizuku))
            }
        }
        item {
            OutlinedButton(
                onClick = { context.safeStart(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.advanced_open_developer_options))
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    context.safeStart(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(PrivilegedSetupNavigator.SHIZUKU_SETUP_GUIDE_URL),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.advanced_official_shizuku_guide))
            }
        }
    }
}

private fun android.content.Context.safeStart(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No-op: Home and this screen still contain manual recovery steps.
    } catch (_: SecurityException) {
        // No-op: some OEMs gate developer-settings entry points.
    }
}
