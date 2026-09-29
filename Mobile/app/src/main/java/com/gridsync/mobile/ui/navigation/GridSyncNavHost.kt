package com.gridsync.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gridsync.mobile.ui.screens.dashboard.ProsumerDashboardScreen
import com.gridsync.mobile.ui.screens.login.LoginScreen
import com.gridsync.mobile.ui.screens.register.RegisterScreen

@Composable
fun GridSyncNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.Login,
        modifier = modifier,
    ) {
        composable(AppRoutes.Login) {
            LoginScreen(
                onForgotPassword = {
                    // Wire later
                },
                onRegister = {
                    navController.navigate(AppRoutes.Register)
                },
                onLoginSuccess = {
                    navController.navigate(AppRoutes.ProsumerDashboard) {
                        popUpTo(AppRoutes.Login) { inclusive = true }
                    }
                },
            )
        }
        composable(AppRoutes.Register) {
            RegisterScreen(
                onBackToLogin = {
                    navController.popBackStack()
                },
            )
        }
        composable(AppRoutes.ProsumerDashboard) {
            ProsumerDashboardScreen(
                userName = "Ayesha Perera",
                onBookEnergy = {
                    // Wire booking flow later
                },
                onBrowseStations = {
                    // Wire station list later
                },
                onStationClick = {
                    // Wire station detail later
                },
                onSignOut = {
                    navController.navigate(AppRoutes.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
