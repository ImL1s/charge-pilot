package com.chargepilot.feature.brands.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object BrandsRoute

fun NavController.navigateToBrands() = navigate(BrandsRoute)

fun NavGraphBuilder.brandsRoute(content: @Composable () -> Unit) {
    composable<BrandsRoute> { content() }
}
