package com.gridsync.mobile.data.remote

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
}
