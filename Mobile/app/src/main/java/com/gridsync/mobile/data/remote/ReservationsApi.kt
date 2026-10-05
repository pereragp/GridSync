package com.gridsync.mobile.data.remote

import com.gridsync.mobile.data.remote.dto.AvailableBookingSlotDto
import com.gridsync.mobile.data.remote.dto.CancelReservationRequestDto
import com.gridsync.mobile.data.remote.dto.CreateReservationRequestDto
import com.gridsync.mobile.data.remote.dto.ProsumerDashboardStatsDto
import com.gridsync.mobile.data.remote.dto.RejectReservationRequestDto
import com.gridsync.mobile.data.remote.dto.ReservationCompletionDto
import com.gridsync.mobile.data.remote.dto.ReservationDashboardStatsDto
import com.gridsync.mobile.data.remote.dto.ReservationDto
import com.gridsync.mobile.data.remote.dto.ReservationQrVerificationDto
import com.gridsync.mobile.data.remote.dto.UpdateReservationRequestDto
import com.gridsync.mobile.data.remote.dto.VerifyReservationQrRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ReservationsApi {
    // —— Prosumer ——
    @GET("api/reservations/slots")
    suspend fun getAvailableSlots(): List<AvailableBookingSlotDto>

    @GET("api/reservations/upcoming")
    suspend fun getUpcoming(
        @Query("status") status: String? = null,
    ): List<ReservationDto>

    @GET("api/reservations/history")
    suspend fun getHistory(
        @Query("status") status: String? = null,
    ): List<ReservationDto>

    @GET("api/reservations/prosumer-dashboard")
    suspend fun getProsumerDashboardStats(): ProsumerDashboardStatsDto

    @POST("api/reservations")
    suspend fun create(@Body body: CreateReservationRequestDto): ReservationDto

    @PUT("api/reservations/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: UpdateReservationRequestDto,
    ): ReservationDto

    @POST("api/reservations/{id}/cancel")
    suspend fun cancel(
        @Path("id") id: String,
        @Body body: CancelReservationRequestDto = CancelReservationRequestDto(),
    ): ReservationDto

    // —— Shared / Operator ——
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
