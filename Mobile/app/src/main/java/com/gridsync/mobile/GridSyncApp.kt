package com.gridsync.mobile

import android.app.Application
import com.gridsync.mobile.data.auth.AuthRepository
import com.gridsync.mobile.data.remote.ApiClient
import com.gridsync.mobile.data.session.SessionStore

class GridSyncApp : Application() {
    lateinit var sessionStore: SessionStore
        private set

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        authRepository = AuthRepository(
            authApi = ApiClient.createAuthApi(sessionStore),
            sessionStore = sessionStore,
        )
    }
}
