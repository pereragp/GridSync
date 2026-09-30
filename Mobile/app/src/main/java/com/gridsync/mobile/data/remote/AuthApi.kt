package com.gridsync.mobile.data.remote

import com.gridsync.mobile.data.remote.dto.LoginRequestDto
import com.gridsync.mobile.data.remote.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto
}
