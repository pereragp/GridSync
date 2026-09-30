package com.gridsync.mobile.data.session

import android.content.Context

data class UserSession(
    val token: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val role: String,
    val status: String,
    val nic: String?,
    val expiresAt: String?,
)

class SessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val token: String?
        get() = prefs.getString(KEY_TOKEN, null)

    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank()

    fun getSession(): UserSession? {
        val token = token ?: return null
        return UserSession(
            token = token,
            userId = prefs.getString(KEY_USER_ID, "").orEmpty(),
            fullName = prefs.getString(KEY_FULL_NAME, "").orEmpty(),
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
            role = prefs.getString(KEY_ROLE, "").orEmpty(),
            status = prefs.getString(KEY_STATUS, "").orEmpty(),
            nic = prefs.getString(KEY_NIC, null),
            expiresAt = prefs.getString(KEY_EXPIRES_AT, null),
        )
    }

    fun save(session: UserSession) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_FULL_NAME, session.fullName)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_ROLE, session.role)
            .putString(KEY_STATUS, session.status)
            .putString(KEY_NIC, session.nic)
            .putString(KEY_EXPIRES_AT, session.expiresAt)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "gridsync_session"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "userId"
        private const val KEY_FULL_NAME = "fullName"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROLE = "role"
        private const val KEY_STATUS = "status"
        private const val KEY_NIC = "nic"
        private const val KEY_EXPIRES_AT = "expiresAt"
    }
}
