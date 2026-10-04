package com.gridsync.mobile.data.reservation

import com.gridsync.mobile.data.remote.ApiException
import com.gridsync.mobile.data.remote.ReservationsApi
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
import com.gridsync.mobile.data.remote.toUserMessage

class ReservationRepository(
    private val reservationsApi: ReservationsApi,
) {
    // —— Prosumer ——
    suspend fun getAvailableSlots(): List<AvailableBookingSlotDto> {
        try {
            return reservationsApi.getAvailableSlots()
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun getUpcoming(status: String? = null): List<ReservationDto> {
        try {
            return reservationsApi.getUpcoming(status?.takeIf { it.isNotBlank() })
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun getHistory(status: String? = null): List<ReservationDto> {
        try {
            return reservationsApi.getHistory(status?.takeIf { it.isNotBlank() })
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun getProsumerDashboardStats(): ProsumerDashboardStatsDto {
        try {
            return reservationsApi.getProsumerDashboardStats()
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun create(
        slotId: String,
        reservationType: String,
        energyKwh: Double,
        slotStartIso: String,
        slotEndIso: String,
    ): ReservationDto {
        try {
            return reservationsApi.create(
                CreateReservationRequestDto(
                    slotId = slotId,
                    reservationType = reservationType,
                    energyKwh = energyKwh,
                    slotStart = slotStartIso,
                    slotEnd = slotEndIso,
                )
            )
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun update(
        id: String,
        reservationType: String,
        energyKwh: Double,
        slotStartIso: String,
        slotEndIso: String,
    ): ReservationDto {
        try {
            return reservationsApi.update(
                id = id,
                body = UpdateReservationRequestDto(
                    reservationType = reservationType,
                    energyKwh = energyKwh,
                    slotStart = slotStartIso,
                    slotEnd = slotEndIso,
                ),
            )
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun cancel(id: String, reason: String? = null): ReservationDto {
        try {
            return reservationsApi.cancel(
                id = id,
                body = CancelReservationRequestDto(reason = reason?.takeIf { it.isNotBlank() }),
            )
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    // —— Shared / Operator ——
    suspend fun getManaged(status: String? = null): List<ReservationDto> {
        try {
            return reservationsApi.getManaged(status?.takeIf { it.isNotBlank() })
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun getDashboardStats(): ReservationDashboardStatsDto {
        try {
            return reservationsApi.getDashboardStats()
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun getById(id: String): ReservationDto {
        try {
            return reservationsApi.getById(id)
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun approve(id: String): ReservationDto {
        try {
            return reservationsApi.approve(id)
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun reject(id: String, reason: String): ReservationDto {
        try {
            return reservationsApi.reject(id, RejectReservationRequestDto(reason = reason))
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun verifyQr(qrPayload: String): ReservationQrVerificationDto {
        try {
            return reservationsApi.verifyQr(VerifyReservationQrRequestDto(qrPayload = qrPayload))
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun complete(id: String): ReservationCompletionDto {
        try {
            return reservationsApi.complete(id)
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }
}
