package com.gridsync.mobile.ui.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DisplayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")

private val LocalEditFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

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

/** Convert API UTC/ISO datetime to editable local `yyyy-MM-dd HH:mm`. */
fun formatApiDateTimeForEdit(value: String?): String {
    if (value.isNullOrBlank()) return ""
    val zone = ZoneId.systemDefault()
    return runCatching {
        Instant.parse(value).atZone(zone).format(LocalEditFormatter)
    }.recoverCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(zone).format(LocalEditFormatter)
    }.recoverCatching {
        val cleaned = value.substringBefore('.').replace(' ', 'T')
        LocalDateTime.parse(cleaned).atZone(zone).format(LocalEditFormatter)
    }.getOrElse { value }
}

/**
 * Parse local `yyyy-MM-dd HH:mm` (or `yyyy-MM-dd'T'HH:mm`) as device timezone
 * and emit ISO-8601 UTC for the API.
 */
fun localDateTimeToIsoUtc(value: String): String {
    val trimmed = value.trim()
    val local = runCatching {
        LocalDateTime.parse(trimmed, LocalEditFormatter)
    }.recoverCatching {
        LocalDateTime.parse(trimmed.replace(' ', 'T'))
    }.getOrElse {
        throw IllegalArgumentException("Use format YYYY-MM-DD HH:mm")
    }
    return local.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toInstant().toString()
}

fun defaultVisitStartLocal(): String {
    val start = LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0)
    return start.format(LocalEditFormatter)
}

fun defaultVisitEndLocal(): String {
    val end = LocalDateTime.now().plusDays(1).plusHours(1).withMinute(0).withSecond(0).withNano(0)
    return end.format(LocalEditFormatter)
}

fun reservationTypeLabel(type: String?): String {
    return when (type) {
        "DropOff" -> "Drop-off"
        "Charging" -> "Charging"
        else -> type?.ifBlank { "Booking" } ?: "Booking"
    }
}
