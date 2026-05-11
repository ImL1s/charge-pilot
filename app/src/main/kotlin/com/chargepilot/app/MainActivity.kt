package com.chargepilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chargepilot.app.navigation.ChargePilotNavHost
import com.chargepilot.core.designsystem.theme.ChargePilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChargePilotTheme {
                ChargePilotNavHost()
            }
        }
    }
}
