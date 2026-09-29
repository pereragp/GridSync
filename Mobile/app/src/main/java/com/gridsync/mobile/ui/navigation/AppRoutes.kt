package com.gridsync.mobile.ui.navigation

object AppRoutes {
    const val Login = "login"
    const val Register = "register"
    const val ForgotPassword = "forgot_password"
    const val ResetPassword = "reset_password?email={email}"
    const val ProsumerDashboard = "prosumer_dashboard"
    const val StationDetail = "station/{stationId}"
    const val CreateReservation = "book/{stationId}"
    const val MyBookings = "bookings"
    const val BookingDetail = "bookings/{reservationId}"
    const val Profile = "profile"

    fun stationDetail(stationId: String) = "station/$stationId"

    fun createReservation(stationId: String? = null): String {
        val id = stationId?.takeIf { it.isNotBlank() } ?: "none"
        return "book/$id"
    }

    fun bookingDetail(reservationId: String) = "bookings/$reservationId"

    fun resetPassword(email: String = ""): String {
        val encoded = android.net.Uri.encode(email)
        return "reset_password?email=$encoded"
    }
}
