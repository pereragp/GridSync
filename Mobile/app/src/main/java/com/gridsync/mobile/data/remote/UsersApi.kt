package com.gridsync.mobile.data.remote

import com.gridsync.mobile.data.remote.dto.UpdateUserRequestDto
import com.gridsync.mobile.data.remote.dto.UserResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UsersApi {
    @GET("api/users/{id}")
    suspend fun getById(@Path("id") id: String): UserResponseDto

    @PUT("api/users/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: UpdateUserRequestDto,
    ): UserResponseDto

    @POST("api/users/{id}/request-deactivation")
    suspend fun requestDeactivation(@Path("id") id: String): UserResponseDto
}
