package com.gridsync.mobile.data.station

import com.gridsync.mobile.data.remote.ApiException
import com.gridsync.mobile.data.remote.StationsApi
import com.gridsync.mobile.data.remote.dto.NearbyStationResponseDto
import com.gridsync.mobile.data.remote.dto.StationResponseDto
import com.gridsync.mobile.data.remote.toUserMessage

class StationRepository(
    private val stationsApi: StationsApi,
) {
    suspend fun getNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = DEFAULT_RADIUS_KM,
    ): List<NearbyStationResponseDto> {
        try {
            return stationsApi.getNearby(
                lat = latitude,
                lng = longitude,
                radiusKm = radiusKm,
            )
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun getById(id: String): StationResponseDto {
        try {
            return stationsApi.getById(id)
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    companion object {
        const val DEFAULT_RADIUS_KM = 10.0
    }
}
