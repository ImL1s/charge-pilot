package com.chargepilot.feature.about.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object AboutRoute

fun NavController.navigateToAbout() = navigate(AboutRoute)

fun NavGraphBuilder.aboutRoute(content: @Composable () -> Unit) {
    composable<AboutRoute> { content() }
}
