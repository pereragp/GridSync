package com.gridsync.mobile.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gridsync.mobile.GridSyncApp
import com.gridsync.mobile.ui.components.SystemBarsStyle
import com.gridsync.mobile.ui.screens.auth.ForgotPasswordScreen
import com.gridsync.mobile.ui.screens.auth.ResetPasswordScreen
import com.gridsync.mobile.ui.screens.bookings.BookingDetailScreen
import com.gridsync.mobile.ui.screens.bookings.MyBookingsScreen
import com.gridsync.mobile.ui.screens.dashboard.ProsumerDashboardScreen
import com.gridsync.mobile.ui.screens.login.LoginScreen
import com.gridsync.mobile.ui.screens.operator.OperatorBookingDetailScreen
import com.gridsync.mobile.ui.screens.operator.OperatorBookingsScreen
import com.gridsync.mobile.ui.screens.operator.OperatorDashboardScreen
import com.gridsync.mobile.ui.screens.operator.OperatorScanScreen
import com.gridsync.mobile.ui.screens.operator.OperatorStationBatteriesScreen
import com.gridsync.mobile.ui.screens.operator.OperatorStationsScreen
import com.gridsync.mobile.ui.screens.profile.ProfileScreen
import com.gridsync.mobile.ui.screens.register.RegisterScreen
import com.gridsync.mobile.ui.screens.reservation.CreateReservationScreen
import com.gridsync.mobile.ui.screens.station.StationDetailScreen

@Composable
fun GridSyncNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val app = LocalContext.current.applicationContext as GridSyncApp
    val startDestination = remember {
        if (app.sessionStore.isLoggedIn) {
            AppRoutes.homeForRole(app.sessionStore.getSession()?.role)
        } else {
            AppRoutes.Login
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val sessionRole = app.sessionStore.getSession()?.role
    val isOperator = sessionRole.equals("GridOperator", ignoreCase = true)
    val showProsumerBottomBar = !isOperator && bottomBarVisibleFor(currentRoute)
    val showOperatorBottomBar = isOperator && operatorBottomBarVisibleFor(currentRoute)

    val lightSystemBars = when {
        currentRoute == AppRoutes.Login ||
            currentRoute == AppRoutes.Register ||
            currentRoute == AppRoutes.ForgotPassword ||
            currentRoute?.startsWith("reset_password") == true -> false
        currentRoute == AppRoutes.ProsumerDashboard -> false
        currentRoute == AppRoutes.OperatorDashboard -> false
        currentRoute == AppRoutes.Profile || currentRoute == AppRoutes.OperatorProfile -> false
        currentRoute?.startsWith("station/") == true -> false
        else -> true
    }
    SystemBarsStyle(lightBackground = lightSystemBars)

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            when {
                showOperatorBottomBar -> {
                    OperatorBottomBar(
                        currentRoute = currentRoute,
                        onTabSelected = { tab -> navigateToOperatorTab(navController, tab) },
                    )
                }
                showProsumerBottomBar -> {
                    ProsumerBottomBar(
                        currentRoute = currentRoute,
                        onTabSelected = { tab -> navigateToTab(navController, tab) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(AppRoutes.Login) {
                LoginScreen(
                    onForgotPassword = {
                        navController.navigate(AppRoutes.ForgotPassword)
                    },
                    onRegister = {
                        navController.navigate(AppRoutes.Register)
                    },
                    onLoginSuccess = { role ->
                        navController.navigate(AppRoutes.homeForRole(role)) {
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
            composable(AppRoutes.ForgotPassword) {
                ForgotPasswordScreen(
                    onBackToLogin = {
                        navController.popBackStack(AppRoutes.Login, inclusive = false)
                    },
                    onContinueToReset = { email ->
                        navController.navigate(AppRoutes.resetPassword(email))
                    },
                )
            }
            composable(
                route = AppRoutes.ResetPassword,
                arguments = listOf(
                    navArgument("email") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                ),
            ) { entry ->
                val email = entry.arguments?.getString("email").orEmpty()
                ResetPasswordScreen(
                    initialEmail = email,
                    onBackToLogin = {
                        navController.popBackStack(AppRoutes.Login, inclusive = false)
                    },
                )
            }

            // —— Prosumer ——
            composable(AppRoutes.ProsumerDashboard) {
                val sessionName = app.sessionStore.getSession()?.fullName.orEmpty()
                ProsumerDashboardScreen(
                    userName = sessionName.ifBlank { "Prosumer" },
                    onBookEnergy = {
                        navController.navigate(AppRoutes.createReservation())
                    },
                    onMyBookings = {
                        navigateToTab(navController, ProsumerTab.Bookings)
                    },
                    onStationClick = { station ->
                        navController.navigate(AppRoutes.stationDetail(station.id))
                    },
                )
            }
            composable(
                route = AppRoutes.StationDetail,
                arguments = listOf(navArgument("stationId") { type = NavType.StringType }),
            ) { entry ->
                val stationId = entry.arguments?.getString("stationId").orEmpty()
                StationDetailScreen(
                    stationId = stationId,
                    onBack = { navController.popBackStack() },
                    onReserve = { id ->
                        navController.navigate(AppRoutes.createReservation(id))
                    },
                )
            }
            composable(
                route = AppRoutes.CreateReservation,
                arguments = listOf(navArgument("stationId") { type = NavType.StringType }),
            ) { entry ->
                val rawId = entry.arguments?.getString("stationId").orEmpty()
                val stationId = rawId.takeUnless { it.isBlank() || it == "none" }
                CreateReservationScreen(
                    initialStationId = stationId,
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.navigate(AppRoutes.MyBookings) {
                            popUpTo(AppRoutes.ProsumerDashboard) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(AppRoutes.MyBookings) {
                MyBookingsScreen(
                    showBack = false,
                    onBack = {
                        navigateToTab(navController, ProsumerTab.Home)
                    },
                    onBookingClick = { reservation ->
                        navController.navigate(AppRoutes.bookingDetail(reservation.id))
                    },
                    onCreateBooking = {
                        navController.navigate(AppRoutes.createReservation())
                    },
                )
            }
            composable(
                route = AppRoutes.BookingDetail,
                arguments = listOf(navArgument("reservationId") { type = NavType.StringType }),
            ) { entry ->
                val reservationId = entry.arguments?.getString("reservationId").orEmpty()
                BookingDetailScreen(
                    reservationId = reservationId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.Profile) {
                ProfileScreen(
                    showBack = false,
                    onBack = {
                        navigateToTab(navController, ProsumerTab.Home)
                    },
                    onSignOut = {
                        app.authRepository.logoutLocal()
                        navController.navigate(AppRoutes.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }

            // —— Grid Operator ——
            composable(AppRoutes.OperatorDashboard) {
                val sessionName = app.sessionStore.getSession()?.fullName.orEmpty()
                OperatorDashboardScreen(
                    userName = sessionName.ifBlank { "Operator" },
                    onReviewBookings = {
                        navigateToOperatorTab(navController, OperatorTab.Bookings)
                    },
                    onOpenStations = {
                        navigateToOperatorTab(navController, OperatorTab.Stations)
                    },
                    onScanQr = {
                        navigateToOperatorTab(navController, OperatorTab.Scan)
                    },
                    onBookingClick = { reservation ->
                        navController.navigate(AppRoutes.operatorBookingDetail(reservation.id))
                    },
                )
            }
            composable(AppRoutes.OperatorBookings) {
                OperatorBookingsScreen(
                    onBookingClick = { reservation ->
                        navController.navigate(AppRoutes.operatorBookingDetail(reservation.id))
                    },
                )
            }
            composable(
                route = AppRoutes.OperatorBookingDetail,
                arguments = listOf(navArgument("reservationId") { type = NavType.StringType }),
            ) { entry ->
                val reservationId = entry.arguments?.getString("reservationId").orEmpty()
                OperatorBookingDetailScreen(
                    reservationId = reservationId,
                    onBack = { navController.popBackStack() },
                    onOpenScan = {
                        navigateToOperatorTab(navController, OperatorTab.Scan)
                    },
                )
            }
            composable(AppRoutes.OperatorStations) {
                OperatorStationsScreen(
                    onStationClick = { station ->
                        navController.navigate(AppRoutes.operatorStationBatteries(station.id))
                    },
                )
            }
            composable(
                route = AppRoutes.OperatorStationBatteries,
                arguments = listOf(navArgument("stationId") { type = NavType.StringType }),
            ) { entry ->
                val stationId = entry.arguments?.getString("stationId").orEmpty()
                OperatorStationBatteriesScreen(
                    stationId = stationId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.OperatorScan) {
                OperatorScanScreen()
            }
            composable(AppRoutes.OperatorProfile) {
                ProfileScreen(
                    showBack = false,
                    onBack = {
                        navigateToOperatorTab(navController, OperatorTab.Home)
                    },
                    onSignOut = {
                        app.authRepository.logoutLocal()
                        navController.navigate(AppRoutes.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}

private fun navigateToTab(navController: NavHostController, tab: ProsumerTab) {
    val route = tab.route
    if (tab == ProsumerTab.Book) {
        navController.navigate(route)
        return
    }
    navController.navigate(route) {
        popUpTo(AppRoutes.ProsumerDashboard) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun navigateToOperatorTab(navController: NavHostController, tab: OperatorTab) {
    navController.navigate(tab.route) {
        popUpTo(AppRoutes.OperatorDashboard) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
