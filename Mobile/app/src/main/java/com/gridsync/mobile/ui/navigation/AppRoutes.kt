package com.gridsync.mobile.ui.navigation

object AppRoutes {
    const val Login = "login"
    const val Register = "register"
    const val ProsumerDashboard = "prosumer_dashboard"
    const val StationDetail = "station/{stationId}"
    const val CreateReservation = "book/{stationId}"
    const val MyBookings = "bookings"
    const val BookingDetail = "bookings/{reservationId}"

    fun stationDetail(stationId: String) = "station/$stationId"

    fun createReservation(stationId: String? = null): String {
        val id = stationId?.takeIf { it.isNotBlank() } ?: "none"
        return "book/$id"
    }

    fun bookingDetail(reservationId: String) = "bookings/$reservationId"
}
