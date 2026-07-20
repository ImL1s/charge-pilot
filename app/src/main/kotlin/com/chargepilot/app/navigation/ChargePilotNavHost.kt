package com.chargepilot.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chargepilot.app.R
import com.chargepilot.feature.about.api.AboutRoute
import com.chargepilot.feature.about.api.aboutRoute
import com.chargepilot.feature.about.impl.AboutScreen
import com.chargepilot.feature.advanced.api.AdvancedRoute
import com.chargepilot.feature.advanced.api.advancedRoute
import com.chargepilot.feature.advanced.impl.AdvancedScreen
import com.chargepilot.feature.brands.api.BrandsRoute
import com.chargepilot.feature.brands.api.brandsRoute
import com.chargepilot.feature.brands.impl.BrandsScreen
import com.chargepilot.feature.history.api.HistoryRoute
import com.chargepilot.feature.history.api.historyRoute
import com.chargepilot.feature.history.impl.HistoryScreen
import com.chargepilot.feature.disclosure.api.disclosureRoute
import com.chargepilot.feature.disclosure.api.navigateToDisclosure
import com.chargepilot.feature.disclosure.impl.DisclosureScreen
import com.chargepilot.feature.home.api.HomeRoute
import com.chargepilot.feature.home.api.homeRoute
import com.chargepilot.feature.home.impl.HomeScreen
import kotlin.reflect.KClass

private sealed class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    val labelRes: Int,
) {
    data object Home : TopLevelDestination(HomeRoute, HomeRoute::class, Icons.Outlined.Home, R.string.nav_home)
    data object Brands : TopLevelDestination(BrandsRoute, BrandsRoute::class, Icons.Outlined.Star, R.string.nav_brands)
    data object History : TopLevelDestination(HistoryRoute, HistoryRoute::class, Icons.Outlined.History, R.string.nav_history)
    data object Advanced : TopLevelDestination(AdvancedRoute, AdvancedRoute::class, Icons.Outlined.Tune, R.string.nav_advanced)
    data object About : TopLevelDestination(AboutRoute, AboutRoute::class, Icons.Outlined.Info, R.string.nav_about)
}

private val topLevelDestinations: List<TopLevelDestination> = listOf(
    TopLevelDestination.Home,
    TopLevelDestination.Brands,
    TopLevelDestination.History,
    TopLevelDestination.Advanced,
    TopLevelDestination.About,
)

@Composable
fun ChargePilotNavHost() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = currentBackStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    val destinationLabel = stringResource(destination.labelRes)
                    val selected = currentDestination?.hierarchy()
                        ?.any { it.hasRoute(destination.routeClass) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destinationLabel) },
                        label = { Text(destinationLabel) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            homeRoute {
                HomeScreen(
                    onNeedDisclosure = { capabilityId ->
                        navController.navigateToDisclosure(capabilityId)
                    },
                )
            }
            brandsRoute { BrandsScreen() }
            historyRoute { HistoryScreen() }
            advancedRoute { AdvancedScreen() }
            aboutRoute { AboutScreen() }
            disclosureRoute { capabilityId ->
                DisclosureScreen(
                    capabilityId = capabilityId,
                    onFinished = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavDestination.hierarchy(): Sequence<NavDestination> =
    generateSequence(this) { it.parent }
