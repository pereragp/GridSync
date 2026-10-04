package com.gridsync.mobile.data.remote.dto

data class ReservationDto(
    val id: String = "",
    val reservationCode: String = "",
    val prosumerId: String = "",
    val prosumerNic: String? = null,
    val stationId: String = "",
    val slotId: String = "",
    val stationName: String? = null,
    val slotStart: String = "",
    val slotEnd: String = "",
    val reservationType: String = "",
    val energyKwh: Double = 0.0,
    val status: String = "",
    val qrPayload: String? = null,
    val qrGeneratedAt: String? = null,
    val completedAt: String? = null,
    val completedBy: String? = null,
    val cancellationReason: String? = null,
    val cancelledAt: String? = null,
    val rejectionReason: String? = null,
    val rejectedAt: String? = null,
    val rejectedBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** Battery shown to prosumers for Charging / DropOff booking. */
data class AvailableBookingSlotDto(
    val id: String = "",
    val stationId: String = "",
    val stationName: String = "",
    val batteryIndex: Int = 0,
    val capacityKwh: Double = 0.0,
    val actualEnergyKwh: Double = 0.0,
    val availableChargingKwh: Double = 0.0,
    val availableDropOffKwh: Double = 0.0,
    val status: String = "",
    val notes: String? = null,
)

data class CreateReservationRequestDto(
    val slotId: String,
    val reservationType: String,
    val energyKwh: Double,
    val slotStart: String,
    val slotEnd: String,
)

data class UpdateReservationRequestDto(
    val reservationType: String,
    val energyKwh: Double,
    val slotStart: String,
    val slotEnd: String,
)

data class CancelReservationRequestDto(
    val reason: String? = null,
)

data class ReservationDashboardStatsDto(
    val pendingReservations: Long = 0,
    val approvedUpcomingReservations: Long = 0,
    val completedTransfers: Long = 0,
    val rejectedReservations: Long = 0,
    val cancelledReservations: Long = 0,
    val expiredReservations: Long = 0,
)

data class ProsumerDashboardStatsDto(
    val pendingReservations: Long = 0,
    val activeReservations: Long = 0,
)

data class RejectReservationRequestDto(
    val reason: String,
)

data class VerifyReservationQrRequestDto(
    val qrPayload: String,
)

data class ReservationQrVerificationDto(
    val reservationId: String = "",
    val reservationCode: String = "",
    val prosumerNic: String = "",
    val stationName: String? = null,
    val slotStart: String = "",
    val slotEnd: String = "",
    val reservationType: String = "",
    val energyKwh: Double = 0.0,
    val status: String = "",
)

data class ReservationCompletionDto(
    val reservationId: String = "",
    val reservationCode: String = "",
    val status: String = "",
    val completedAt: String = "",
    val completedBy: String = "",
)
