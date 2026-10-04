package com.gridsync.mobile.data.remote

import com.gridsync.mobile.data.remote.dto.BookingSlotDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookingSlotsApi {
    @GET("api/bookingslots")
    suspend fun getAll(
        @Query("stationId") stationId: String? = null,
        @Query("status") status: String? = null,
    ): List<BookingSlotDto>

    @GET("api/bookingslots/{id}")
    suspend fun getById(@Path("id") id: String): BookingSlotDto

    @POST("api/bookingslots/{id}/close")
    suspend fun close(@Path("id") id: String): BookingSlotDto

    @POST("api/bookingslots/{id}/reopen")
    suspend fun reopen(@Path("id") id: String): BookingSlotDto
}
