package com.gridsync.mobile.data.booking

import com.gridsync.mobile.data.remote.ApiException
import com.gridsync.mobile.data.remote.BookingSlotsApi
import com.gridsync.mobile.data.remote.dto.BookingSlotDto
import com.gridsync.mobile.data.remote.toUserMessage

class BookingSlotRepository(
    private val bookingSlotsApi: BookingSlotsApi,
) {
    suspend fun getForStation(stationId: String): List<BookingSlotDto> {
        try {
            return bookingSlotsApi.getAll(stationId = stationId)
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun close(id: String): BookingSlotDto {
        try {
            return bookingSlotsApi.close(id)
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }

    suspend fun reopen(id: String): BookingSlotDto {
        try {
            return bookingSlotsApi.reopen(id)
        } catch (e: Exception) {
            throw ApiException(e.toUserMessage())
        }
    }
}
