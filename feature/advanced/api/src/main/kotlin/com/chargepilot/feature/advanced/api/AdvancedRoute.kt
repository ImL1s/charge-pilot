package com.chargepilot.feature.advanced.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object AdvancedRoute

fun NavController.navigateToAdvanced() = navigate(AdvancedRoute)

fun NavGraphBuilder.advancedRoute(content: @Composable () -> Unit) {
    composable<AdvancedRoute> { content() }
}
