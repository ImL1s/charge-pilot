package com.chargepilot.feature.disclosure.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data class DisclosureRoute(val capabilityId: String)

fun NavController.navigateToDisclosure(capabilityId: String) {
    navigate(route = DisclosureRoute(capabilityId))
}

typealias DisclosureRouteContent = @Composable (capabilityId: String) -> Unit

fun NavGraphBuilder.disclosureRoute(content: DisclosureRouteContent) {
    composable<DisclosureRoute> { entry ->
        val args: DisclosureRoute = entry.toRoute()
        content(args.capabilityId)
    }
}
