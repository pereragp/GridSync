package com.gridsync.mobile.data.remote

import com.gridsync.mobile.data.remote.dto.ApiMessageDto
import com.gridsync.mobile.data.remote.dto.ChangePasswordRequestDto
import com.gridsync.mobile.data.remote.dto.LoginRequestDto
import com.gridsync.mobile.data.remote.dto.LoginResponseDto
import com.gridsync.mobile.data.remote.dto.RegisterProsumerRequestDto
import com.gridsync.mobile.data.remote.dto.UserResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @POST("api/users/prosumers/register")
    suspend fun registerProsumer(@Body body: RegisterProsumerRequestDto): UserResponseDto

    @POST("api/auth/logout")
    suspend fun logout(): ApiMessageDto

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequestDto): ApiMessageDto
}
