package com.gridsync.mobile.data.remote

import com.gridsync.mobile.data.remote.dto.RejectReservationRequestDto
import com.gridsync.mobile.data.remote.dto.ReservationCompletionDto
import com.gridsync.mobile.data.remote.dto.ReservationDashboardStatsDto
import com.gridsync.mobile.data.remote.dto.ReservationDto
import com.gridsync.mobile.data.remote.dto.ReservationQrVerificationDto
import com.gridsync.mobile.data.remote.dto.VerifyReservationQrRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReservationsApi {
    @GET("api/reservations/manage")
    suspend fun getManaged(
        @Query("status") status: String? = null,
    ): List<ReservationDto>

    @GET("api/reservations/dashboard-stats")
    suspend fun getDashboardStats(): ReservationDashboardStatsDto

    @GET("api/reservations/{id}")
    suspend fun getById(@Path("id") id: String): ReservationDto

    @POST("api/reservations/{id}/approve")
    suspend fun approve(@Path("id") id: String): ReservationDto

    @POST("api/reservations/{id}/reject")
    suspend fun reject(
        @Path("id") id: String,
        @Body body: RejectReservationRequestDto,
    ): ReservationDto

    @POST("api/reservations/verify-qr")
    suspend fun verifyQr(
        @Body body: VerifyReservationQrRequestDto,
    ): ReservationQrVerificationDto

    @POST("api/reservations/{id}/complete")
    suspend fun complete(@Path("id") id: String): ReservationCompletionDto
}
