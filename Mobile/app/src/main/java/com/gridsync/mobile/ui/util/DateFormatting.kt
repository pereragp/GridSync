package com.gridsync.mobile.ui.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DisplayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")

fun formatApiDateTime(value: String?): String {
    if (value.isNullOrBlank()) return "Unknown time"
    val zone = ZoneId.systemDefault()
    return runCatching {
        Instant.parse(value).atZone(zone).format(DisplayFormatter)
    }.recoverCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(zone).format(DisplayFormatter)
    }.recoverCatching {
        val cleaned = value.substringBefore('.').replace(' ', 'T')
        LocalDateTime.parse(cleaned).atZone(zone).format(DisplayFormatter)
    }.getOrElse { value }
}

fun reservationTypeLabel(type: String?): String {
    return when (type) {
        "DropOff" -> "Drop-off"
        "Charging" -> "Charging"
        else -> type?.ifBlank { "Booking" } ?: "Booking"
    }
}
