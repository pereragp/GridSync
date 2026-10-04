package com.gridsync.mobile

import android.app.Application
import com.gridsync.mobile.data.auth.AuthRepository
import com.gridsync.mobile.data.location.UserLocationProvider
import com.gridsync.mobile.data.remote.ApiClient
import com.gridsync.mobile.data.session.SessionStore
import com.gridsync.mobile.data.station.StationRepository

class GridSyncApp : Application() {
    lateinit var sessionStore: SessionStore
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var stationRepository: StationRepository
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
        userLocationProvider = UserLocationProvider(this)
    }
}
