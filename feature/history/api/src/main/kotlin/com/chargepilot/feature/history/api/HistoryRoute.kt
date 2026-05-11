package com.chargepilot.feature.history.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object HistoryRoute

fun NavController.navigateToHistory() = navigate(HistoryRoute)

fun NavGraphBuilder.historyRoute(content: @Composable () -> Unit) {
    composable<HistoryRoute> { content() }
}
