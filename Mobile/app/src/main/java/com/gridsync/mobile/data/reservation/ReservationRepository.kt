package com.gridsync.mobile.data.reservation

import com.gridsync.mobile.data.remote.ApiException
import com.gridsync.mobile.data.remote.ReservationsApi
import com.gridsync.mobile.data.remote.dto.RejectReservationRequestDto
import com.gridsync.mobile.data.remote.dto.ReservationCompletionDto
import com.gridsync.mobile.data.remote.dto.ReservationDashboardStatsDto
import com.gridsync.mobile.data.remote.dto.ReservationDto
import com.gridsync.mobile.data.remote.dto.ReservationQrVerificationDto
import com.gridsync.mobile.data.remote.dto.VerifyReservationQrRequestDto
import com.gridsync.mobile.data.remote.toUserMessage

class ReservationRepository(
    private val reservationsApi: ReservationsApi,
) {
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
