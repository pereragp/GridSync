package com.gridsync.mobile.data.auth

import com.gridsync.mobile.data.remote.ApiException
import com.gridsync.mobile.data.remote.AuthApi
import com.gridsync.mobile.data.remote.dto.ChangePasswordRequestDto
import com.gridsync.mobile.data.remote.dto.LoginRequestDto
import com.gridsync.mobile.data.remote.dto.RegisterProsumerRequestDto
import com.gridsync.mobile.data.remote.dto.UserResponseDto
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

    /**
     * Creates a Pending prosumer account. Does not sign the user in —
     * Backoffice must approve before login works.
     */
    suspend fun registerProsumer(
        nic: String,
        fullName: String,
        email: String,
        phone: String,
        password: String,
        address: String?,
    ): UserResponseDto {
        try {
            return authApi.registerProsumer(
                RegisterProsumerRequestDto(
                    nic = nic.trim(),
                    fullName = fullName.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    password = password,
                    address = address?.trim()?.takeIf { it.isNotEmpty() },
                )
            )
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        try {
            authApi.changePassword(
                ChangePasswordRequestDto(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                )
            )
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    /**
     * Revokes the JWT on the server when possible, then clears local session.
     */
    suspend fun logout() {
        try {
            authApi.logout()
        } catch (_: Exception) {
            // Still clear local session if the revoke call fails (offline / expired token).
        } finally {
            sessionStore.clear()
        }
    }

    fun logoutLocal() {
        sessionStore.clear()
    }

    fun currentSession(): UserSession? = sessionStore.getSession()
}
