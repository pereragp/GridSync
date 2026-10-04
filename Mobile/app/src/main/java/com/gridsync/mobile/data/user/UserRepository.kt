package com.gridsync.mobile.data.user

import com.gridsync.mobile.data.remote.ApiException
import com.gridsync.mobile.data.remote.UsersApi
import com.gridsync.mobile.data.remote.dto.UpdateUserRequestDto
import com.gridsync.mobile.data.remote.dto.UserResponseDto
import com.gridsync.mobile.data.remote.toUserMessage
import com.gridsync.mobile.data.session.SessionStore

class UserRepository(
    private val usersApi: UsersApi,
    private val sessionStore: SessionStore,
) {
    suspend fun getById(id: String): UserResponseDto {
        try {
            return usersApi.getById(id)
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun update(
        id: String,
        fullName: String,
        phone: String,
        address: String?,
    ): UserResponseDto {
        try {
            val updated = usersApi.update(
                id = id,
                body = UpdateUserRequestDto(
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    address = address?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
            sessionStore.updateProfile(
                fullName = updated.fullName,
                status = updated.status,
                nic = updated.nic,
            )
            return updated
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun requestDeactivation(id: String): UserResponseDto {
        try {
            return usersApi.requestDeactivation(id)
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }
}
