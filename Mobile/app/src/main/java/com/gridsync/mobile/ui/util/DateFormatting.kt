package com.gridsync.mobile.ui.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val DisplayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")

private val LocalEditFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private val TimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm")

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
fun parseLocalDateTime(value: String): LocalDateTime {
    val trimmed = value.trim()
    return runCatching {
        LocalDateTime.parse(trimmed, LocalEditFormatter)
    }.recoverCatching {
        LocalDateTime.parse(trimmed.replace(' ', 'T'))
    }.getOrElse {
        throw IllegalArgumentException("Use format YYYY-MM-DD HH:mm")
    }
}

fun localDateTimeToIsoUtc(value: String): String {
    val local = parseLocalDateTime(value)
    return local.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toInstant().toString()
}

/**
 * Ensures the typed visit window sits on one working day and inside station open/close hours.
 * [openTime]/[closeTime] are `HH:mm`; [workingDays] use Mon…Sun abbreviations.
 */
fun validateVisitWithinStationHours(
    visitStartLocal: String,
    visitEndLocal: String,
    openTime: String,
    closeTime: String,
    workingDays: List<String>,
) {
    val start = parseLocalDateTime(visitStartLocal)
    val end = parseLocalDateTime(visitEndLocal)
    if (!end.isAfter(start)) {
        throw IllegalArgumentException("Visit end must be after visit start.")
    }
    if (start.toLocalDate() != end.toLocalDate()) {
        throw IllegalArgumentException(
            "Visit must stay on a single day within station hours $openTime–$closeTime.",
        )
    }

    val days = workingDays.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    if (days.isEmpty()) {
        throw IllegalArgumentException("Station has no working days configured.")
    }
    val dayKey = dayAbbreviation(start.dayOfWeek)
    if (days.none { it.equals(dayKey, ignoreCase = true) }) {
        throw IllegalArgumentException(
            "Station is closed on $dayKey. Working days: ${days.joinToString(", ")}.",
        )
    }

    val open = parseHourMinute(openTime, "open")
    val close = parseHourMinute(closeTime, "close")
    if (!open.isBefore(close)) {
        throw IllegalArgumentException("Station schedule is invalid (open must be before close).")
    }

    val startTime = start.toLocalTime()
    val endTime = end.toLocalTime()
    if (startTime.isBefore(open) || endTime.isAfter(close)) {
        throw IllegalArgumentException(
            "Visit must be within station hours $openTime–$closeTime " +
                "(requested ${startTime.format(TimeFormatter)}–${endTime.format(TimeFormatter)}).",
        )
    }
}

private fun parseHourMinute(value: String, label: String): LocalTime {
    return try {
        LocalTime.parse(value.trim(), TimeFormatter)
    } catch (_: DateTimeParseException) {
        throw IllegalArgumentException("Station $label time is invalid.")
    }
}

private fun dayAbbreviation(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
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
