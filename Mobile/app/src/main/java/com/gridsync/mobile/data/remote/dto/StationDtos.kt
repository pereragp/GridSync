package com.gridsync.mobile.data.remote.dto

data class StationScheduleDto(
    val openTime: String = "08:00",
    val closeTime: String = "18:00",
    val workingDays: List<String> = emptyList(),
)

data class StationResponseDto(
    val id: String = "",
    val stationCode: String = "",
    val name: String = "",
    val description: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val batteryCapacityKwh: Double = 0.0,
    val availableBatterySlots: Int = 0,
    val totalCapacityKwh: Double = 0.0,
    val schedule: StationScheduleDto = StationScheduleDto(),
    val status: String = "",
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class NearbyStationResponseDto(
    val id: String = "",
    val stationCode: String = "",
    val name: String = "",
    val description: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val batteryCapacityKwh: Double = 0.0,
    val availableBatterySlots: Int = 0,
    val totalCapacityKwh: Double = 0.0,
    val schedule: StationScheduleDto = StationScheduleDto(),
    val status: String = "",
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val distanceKm: Double = 0.0,
)
