package com.chargepilot.feature.home.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable

fun NavController.navigateToHome(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = HomeRoute) { navOptions() }
}

typealias HomeRouteContent = @Composable () -> Unit

fun NavGraphBuilder.homeRoute(content: HomeRouteContent) {
    composable<HomeRoute> { content() }
}
