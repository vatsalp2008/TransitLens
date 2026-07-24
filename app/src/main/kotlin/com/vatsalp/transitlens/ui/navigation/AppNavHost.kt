package com.vatsalp.transitlens.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vatsalp.transitlens.ui.screens.HomeScreen
import com.vatsalp.transitlens.ui.screens.NavigationScreen
import com.vatsalp.transitlens.ui.screens.OnboardingScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val NAVIGATION = "navigation"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(onStartNavigation = { navController.navigate(Routes.NAVIGATION) })
        }
        composable(Routes.NAVIGATION) { NavigationScreen() }
    }
}
