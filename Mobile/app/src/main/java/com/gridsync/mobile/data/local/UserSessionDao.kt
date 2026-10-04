package com.gridsync.mobile.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.gridsync.mobile.data.session.UserSession

class UserSessionDao(context: Context) {
    private val dbHelper = SessionDbHelper(context)

    fun getCurrent(): UserSession? {
        val db = dbHelper.readableDatabase
        db.query(
            SessionDbHelper.TABLE_SESSIONS,
            null,
            "${SessionDbHelper.COL_ID} = ?",
            arrayOf(SessionDbHelper.CURRENT_SESSION_ID.toString()),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.toSession()
        }
    }

    fun upsert(session: UserSession) {
        val values = ContentValues().apply {
            put(SessionDbHelper.COL_ID, SessionDbHelper.CURRENT_SESSION_ID)
            put(SessionDbHelper.COL_TOKEN, session.token)
            put(SessionDbHelper.COL_USER_ID, session.userId)
            put(SessionDbHelper.COL_FULL_NAME, session.fullName)
            put(SessionDbHelper.COL_EMAIL, session.email)
            put(SessionDbHelper.COL_ROLE, session.role)
            put(SessionDbHelper.COL_STATUS, session.status)
            put(SessionDbHelper.COL_NIC, session.nic)
            put(SessionDbHelper.COL_EXPIRES_AT, session.expiresAt)
            put(SessionDbHelper.COL_UPDATED_AT, System.currentTimeMillis())
        }
        dbHelper.writableDatabase.insertWithOnConflict(
            SessionDbHelper.TABLE_SESSIONS,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun clear() {
        dbHelper.writableDatabase.delete(SessionDbHelper.TABLE_SESSIONS, null, null)
    }

    private fun Cursor.toSession(): UserSession {
        fun str(column: String): String = getString(getColumnIndexOrThrow(column)).orEmpty()
        fun strOrNull(column: String): String? {
            val index = getColumnIndexOrThrow(column)
            return if (isNull(index)) null else getString(index)
        }
        return UserSession(
            token = str(SessionDbHelper.COL_TOKEN),
            userId = str(SessionDbHelper.COL_USER_ID),
            fullName = str(SessionDbHelper.COL_FULL_NAME),
            email = str(SessionDbHelper.COL_EMAIL),
            role = str(SessionDbHelper.COL_ROLE),
            status = str(SessionDbHelper.COL_STATUS),
            nic = strOrNull(SessionDbHelper.COL_NIC),
            expiresAt = strOrNull(SessionDbHelper.COL_EXPIRES_AT),
        )
    }
}
