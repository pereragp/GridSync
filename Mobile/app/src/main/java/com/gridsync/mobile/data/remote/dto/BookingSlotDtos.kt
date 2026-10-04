package com.gridsync.mobile.data.remote.dto

data class BookingSlotDto(
    val id: String = "",
    val stationId: String = "",
    val stationName: String = "",
    val batteryIndex: Int = 0,
    val capacityKwh: Double = 0.0,
    val actualEnergyKwh: Double = 0.0,
    val reservedChargingKwh: Double = 0.0,
    val reservedDropOffKwh: Double = 0.0,
    val availableChargingKwh: Double = 0.0,
    val availableDropOffKwh: Double = 0.0,
    val status: String = "",
    val notes: String? = null,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
