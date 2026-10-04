package com.gridsync.mobile.data.remote

import com.gridsync.mobile.data.remote.dto.NearbyStationResponseDto
import com.gridsync.mobile.data.remote.dto.StationResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StationsApi {
    @GET("api/stations/nearby")
    suspend fun getNearby(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double = 10.0,
    ): List<NearbyStationResponseDto>

    @GET("api/stations/{id}")
    suspend fun getById(@Path("id") id: String): StationResponseDto
}
