package com.gridsync.mobile

import android.app.Application
import com.gridsync.mobile.data.auth.AuthRepository
import com.gridsync.mobile.data.booking.BookingSlotRepository
import com.gridsync.mobile.data.location.UserLocationProvider
import com.gridsync.mobile.data.remote.ApiClient
import com.gridsync.mobile.data.reservation.ReservationRepository
import com.gridsync.mobile.data.session.SessionStore
import com.gridsync.mobile.data.station.StationRepository

class GridSyncApp : Application() {
    lateinit var sessionStore: SessionStore
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var stationRepository: StationRepository
        private set

    lateinit var reservationRepository: ReservationRepository
        private set

    lateinit var bookingSlotRepository: BookingSlotRepository
        private set

    lateinit var userLocationProvider: UserLocationProvider
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        authRepository = AuthRepository(
            authApi = ApiClient.createAuthApi(sessionStore),
            sessionStore = sessionStore,
        )
        stationRepository = StationRepository(
            stationsApi = ApiClient.createStationsApi(sessionStore),
        )
        reservationRepository = ReservationRepository(
            reservationsApi = ApiClient.createReservationsApi(sessionStore),
        )
        bookingSlotRepository = BookingSlotRepository(
            bookingSlotsApi = ApiClient.createBookingSlotsApi(sessionStore),
        )
        userLocationProvider = UserLocationProvider(this)
    }
}
