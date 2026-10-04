package com.gridsync.mobile.ui.navigation

object AppRoutes {
    const val Splash = "splash"
    const val Login = "login"
    const val Register = "register"
    const val ForgotPassword = "forgot_password"
    const val ResetPassword = "reset_password?email={email}"

    // Prosumer
    const val ProsumerDashboard = "prosumer_dashboard"
    const val StationDetail = "station/{stationId}"
    const val CreateReservation = "book/{stationId}"
    const val MyBookings = "bookings"
    const val BookingDetail = "bookings/{reservationId}"
    const val Profile = "profile"

    // Grid Operator
    const val OperatorDashboard = "operator_dashboard"
    const val OperatorBookings = "operator_bookings?status={status}&successMessage={successMessage}"
    const val OperatorBookingDetail = "operator_bookings/{reservationId}"
    const val OperatorStations = "operator_stations"
    const val OperatorStationBatteries = "operator_stations/{stationId}/batteries"
    const val OperatorScan = "operator_scan"
    const val OperatorProfile = "operator_profile"

    fun stationDetail(stationId: String) = "station/$stationId"

    fun createReservation(stationId: String? = null): String {
        val id = stationId?.takeIf { it.isNotBlank() } ?: "none"
        return "book/$id"
    }

    fun bookingDetail(reservationId: String) = "bookings/$reservationId"

    fun operatorBookings(
        status: String = "Pending",
        successMessage: String = "",
    ): String {
        val encodedStatus = android.net.Uri.encode(status)
        val encodedMessage = android.net.Uri.encode(successMessage)
        return "operator_bookings?status=$encodedStatus&successMessage=$encodedMessage"
    }

    fun operatorBookingDetail(reservationId: String) = "operator_bookings/$reservationId"

    fun operatorStationBatteries(stationId: String) = "operator_stations/$stationId/batteries"

    fun resetPassword(email: String = ""): String {
        val encoded = android.net.Uri.encode(email)
        return "reset_password?email=$encoded"
    }

    fun homeForRole(role: String?): String {
        return when {
            role.equals("GridOperator", ignoreCase = true) -> OperatorDashboard
            role.equals("Prosumer", ignoreCase = true) -> ProsumerDashboard
            else -> Login
        }
    }
}
