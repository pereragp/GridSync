package com.gridsync.mobile.data.session

import android.content.Context
import com.gridsync.mobile.data.local.UserSessionDao

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

/**
 * Persists the authenticated user session in local SQLite.
 * Login still requires the network API — this only caches the session after success.
 */
class SessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val dao = UserSessionDao(appContext)

    @Volatile
    private var cached: UserSession? = null

    init {
        cached = dao.getCurrent() ?: migrateFromLegacyPrefs()
    }

    val token: String?
        get() = cached?.token

    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank()

    fun getSession(): UserSession? = cached

    fun save(session: UserSession) {
        dao.upsert(session)
        cached = session
        clearLegacyPrefs()
    }

    fun clear() {
        dao.clear()
        cached = null
        clearLegacyPrefs()
    }

    /**
     * One-time move from the old SharedPreferences session store into SQLite,
     * so already-logged-in installs keep working after the upgrade.
     */
    private fun migrateFromLegacyPrefs(): UserSession? {
        val prefs = appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        val session = UserSession(
            token = token,
            userId = prefs.getString(KEY_USER_ID, "").orEmpty(),
            fullName = prefs.getString(KEY_FULL_NAME, "").orEmpty(),
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
            role = prefs.getString(KEY_ROLE, "").orEmpty(),
            status = prefs.getString(KEY_STATUS, "").orEmpty(),
            nic = prefs.getString(KEY_NIC, null),
            expiresAt = prefs.getString(KEY_EXPIRES_AT, null),
        )
        dao.upsert(session)
        prefs.edit().clear().apply()
        return session
    }

    private fun clearLegacyPrefs() {
        appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    companion object {
        private const val LEGACY_PREFS = "gridsync_session"
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
