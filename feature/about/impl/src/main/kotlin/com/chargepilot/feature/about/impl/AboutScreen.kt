package com.chargepilot.feature.about.impl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chargepilot.core.device.DeviceDetector
import com.chargepilot.core.ui.InfoCard
import com.chargepilot.core.ui.StepList
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(viewModel: AboutViewModel = hiltViewModel()) {
    val report by viewModel.reportMarkdown.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                title = stringResource(R.string.about_title_device_report),
                body = stringResource(R.string.about_body_device_report),
            ) {
                Button(
                    onClick = {
                        val text = report
                        if (text.isBlank()) return@Button
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Charge Pilot device report", text))
                        Toast.makeText(
                            context,
                            context.getString(R.string.about_device_report_copied),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.about_copy_device_report))
                }
                OutlinedButton(
                    onClick = {
                        val text = report
                        if (text.isBlank()) return@OutlinedButton
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(
                            Intent.createChooser(
                                send,
                                context.getString(R.string.about_share_device_report),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.about_share_device_report))
                }
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

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val deviceDetector: DeviceDetector,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _reportMarkdown = MutableStateFlow("")
    val reportMarkdown: StateFlow<String> = _reportMarkdown.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = deviceDetector.detect()
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                info.versionCode.toLong()
            }
            _reportMarkdown.value = DeviceReportFormatter.format(
                profile = profile,
                appVersionName = info.versionName.orEmpty(),
                appVersionCode = versionCode,
            )
        }
    }
}
