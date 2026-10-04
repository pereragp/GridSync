package com.gridsync.mobile.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local SQLite database for caching the authenticated user session.
 * Auth remains on the remote API — this only stores session rows after login.
 */
class SessionDbHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DB_NAME,
    null,
    DB_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_SESSIONS (
                $COL_ID INTEGER PRIMARY KEY NOT NULL,
                $COL_TOKEN TEXT NOT NULL,
                $COL_USER_ID TEXT NOT NULL,
                $COL_FULL_NAME TEXT NOT NULL,
                $COL_EMAIL TEXT NOT NULL,
                $COL_ROLE TEXT NOT NULL,
                $COL_STATUS TEXT NOT NULL,
                $COL_NIC TEXT,
                $COL_EXPIRES_AT TEXT,
                $COL_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSIONS")
        onCreate(db)
    }

    companion object {
        const val DB_NAME = "gridsync.db"
        const val DB_VERSION = 1

        const val TABLE_SESSIONS = "user_sessions"
        const val COL_ID = "id"
        const val COL_TOKEN = "token"
        const val COL_USER_ID = "user_id"
        const val COL_FULL_NAME = "full_name"
        const val COL_EMAIL = "email"
        const val COL_ROLE = "role"
        const val COL_STATUS = "status"
        const val COL_NIC = "nic"
        const val COL_EXPIRES_AT = "expires_at"
        const val COL_UPDATED_AT = "updated_at"

        /** Single active session row. */
        const val CURRENT_SESSION_ID = 1
    }
}
