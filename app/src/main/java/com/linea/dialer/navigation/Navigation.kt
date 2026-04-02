package com.linea.dialer.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.linea.dialer.ui.screens.*

sealed class Screen(val route: String) {
    object Onboarding    : Screen("onboarding")
    object Permissions   : Screen("permissions")
    object Main          : Screen("main")
    object ContactDetail : Screen("contact_detail/{contactId}") {
        fun createRoute(id: Long) = "contact_detail/$id"
    }
    object ActiveCall : Screen("active_call/{number}") {
        fun createRoute(number: String) = "active_call/${android.net.Uri.encode(number)}"
    }
}

@Composable
fun LineaNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Onboarding.route,
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        enterTransition  = { slideInVertically(tween(280)) { it / 12 } + fadeIn(tween(280)) },
        exitTransition   = { fadeOut(tween(200)) },
        popEnterTransition  = { fadeIn(tween(200)) },
        popExitTransition   = { slideOutVertically(tween(260)) { it / 12 } + fadeOut(tween(260)) },
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinish = {
                navController.navigate(Screen.Permissions.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Permissions.route) {
            PermissionsScreen(onAllGranted = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Permissions.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Main.route) {
            MainScreen(
                onCallNumber  = { number -> navController.navigate(Screen.ActiveCall.createRoute(number)) },
                onViewContact = { id     -> navController.navigate(Screen.ContactDetail.createRoute(id)) },
            )
        }

        // contactId typed as LongType so SavedStateHandle receives a Long
        composable(
            route     = Screen.ContactDetail.route,
            arguments = listOf(navArgument("contactId") { type = NavType.LongType }),
            enterTransition   = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(280)) { it } + fadeOut(tween(280)) },
        ) {
            // viewModel() auto-receives SavedStateHandle from the NavBackStackEntry
            ContactDetailScreen(
                onBack = { navController.popBackStack() },
                onCall = { number -> navController.navigate(Screen.ActiveCall.createRoute(number)) },
            )
        }

        composable(
            route     = Screen.ActiveCall.route,
            arguments = listOf(navArgument("number") { type = NavType.StringType }),
            enterTransition   = { slideInVertically(tween(350)) { it } + fadeIn(tween(350)) },
            popExitTransition = { slideOutVertically(tween(300)) { it } + fadeOut(tween(300)) },
        ) { backStack ->
            val number = android.net.Uri.decode(backStack.arguments?.getString("number") ?: "")
            ActiveCallScreen(
                number    = number,
                onEndCall = { navController.popBackStack() },
            )
        }
    }
}
