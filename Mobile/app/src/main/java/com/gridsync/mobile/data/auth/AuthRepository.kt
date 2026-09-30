package com.gridsync.mobile.data.auth

import com.gridsync.mobile.data.remote.ApiException
import com.gridsync.mobile.data.remote.AuthApi
import com.gridsync.mobile.data.remote.dto.LoginRequestDto
import com.gridsync.mobile.data.remote.toUserMessage
import com.gridsync.mobile.data.session.SessionStore
import com.gridsync.mobile.data.session.UserSession

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
) {
    suspend fun login(email: String, password: String): UserSession {
        try {
            val response = authApi.login(
                LoginRequestDto(
                    email = email.trim(),
                    password = password,
                )
            )
            if (response.token.isBlank()) {
                throw ApiException("Login response did not include a token.")
            }
            val session = UserSession(
                token = response.token,
                userId = response.userId,
                fullName = response.fullName,
                email = response.email,
                role = response.role,
                status = response.status,
                nic = response.nic,
                expiresAt = response.expiresAt,
            )
            sessionStore.save(session)
            return session
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    fun logoutLocal() {
        sessionStore.clear()
    }

    fun currentSession(): UserSession? = sessionStore.getSession()
}
