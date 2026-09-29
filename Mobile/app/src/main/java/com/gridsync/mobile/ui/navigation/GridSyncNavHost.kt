package com.gridsync.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gridsync.mobile.ui.screens.bookings.BookingDetailScreen
import com.gridsync.mobile.ui.screens.bookings.MyBookingsScreen
import com.gridsync.mobile.ui.screens.dashboard.ProsumerDashboardScreen
import com.gridsync.mobile.ui.screens.login.LoginScreen
import com.gridsync.mobile.ui.screens.register.RegisterScreen
import com.gridsync.mobile.ui.screens.reservation.CreateReservationScreen
import com.gridsync.mobile.ui.screens.station.StationDetailScreen

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
                    navController.navigate(AppRoutes.createReservation())
                },
                onMyBookings = {
                    navController.navigate(AppRoutes.MyBookings)
                },
                onStationClick = { station ->
                    navController.navigate(AppRoutes.stationDetail(station.id))
                },
                onSignOut = {
                    navController.navigate(AppRoutes.Login) {
                        popUpTo(0) { inclusive = true }
                    }
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
                        popUpTo(AppRoutes.ProsumerDashboard)
                    }
                },
            )
        }
        composable(AppRoutes.MyBookings) {
            MyBookingsScreen(
                onBack = { navController.popBackStack() },
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
    }
}
