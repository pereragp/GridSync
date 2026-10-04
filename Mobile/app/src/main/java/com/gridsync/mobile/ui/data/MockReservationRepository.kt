package com.gridsync.mobile.ui.data

enum class MockReservationStatus {
    Pending,
    Approved,
    Rejected,
    Cancelled,
    Completed,
    Expired,
}

data class MockReservation(
    val id: String,
    val reservationCode: String,
    val stationId: String,
    val stationName: String,
    val batteryIndex: Int,
    val reservationType: String, // Charging | DropOff
    val energyKwh: Double,
    val slotStart: String,
    val slotEnd: String,
    val status: MockReservationStatus,
    val qrPayload: String? = null,
    val canModify: Boolean = false,
)

object MockReservationRepository {
    private val seed = listOf(
        MockReservation(
            id = "r1",
            reservationCode = "RSV-24091A",
            stationId = "1",
            stationName = "Colombo Fort Hub",
            batteryIndex = 1,
            reservationType = "Charging",
            energyKwh = 5.0,
            slotStart = "2026-04-02 09:00",
            slotEnd = "2026-04-02 10:00",
            status = MockReservationStatus.Pending,
            canModify = true,
        ),
        MockReservation(
            id = "r2",
            reservationCode = "RSV-24088B",
            stationId = "2",
            stationName = "Bambalapitiya Node",
            batteryIndex = 2,
            reservationType = "DropOff",
            energyKwh = 3.5,
            slotStart = "2026-04-03 14:00",
            slotEnd = "2026-04-03 15:00",
            status = MockReservationStatus.Approved,
            qrPayload = "GS|RSV-24088B|2|DropOff|3.5",
            canModify = false,
        ),
        MockReservation(
            id = "r3",
            reservationCode = "RSV-24070C",
            stationId = "3",
            stationName = "Nugegoda Microgrid",
            batteryIndex = 1,
            reservationType = "Charging",
            energyKwh = 8.0,
            slotStart = "2026-03-20 11:00",
            slotEnd = "2026-03-20 12:00",
            status = MockReservationStatus.Completed,
        ),
        MockReservation(
            id = "r4",
            reservationCode = "RSV-24055D",
            stationId = "1",
            stationName = "Colombo Fort Hub",
            batteryIndex = 3,
            reservationType = "DropOff",
            energyKwh = 2.0,
            slotStart = "2026-03-12 16:00",
            slotEnd = "2026-03-12 17:00",
            status = MockReservationStatus.Cancelled,
        ),
        MockReservation(
            id = "r5",
            reservationCode = "RSV-24040E",
            stationId = "4",
            stationName = "Dehiwala Battery Park",
            batteryIndex = 1,
            reservationType = "Charging",
            energyKwh = 4.0,
            slotStart = "2026-03-01 10:00",
            slotEnd = "2026-03-01 11:00",
            status = MockReservationStatus.Rejected,
        ),
        MockReservation(
            id = "r6",
            reservationCode = "RSV-24092F",
            stationId = "3",
            stationName = "Nugegoda Microgrid",
            batteryIndex = 2,
            reservationType = "Charging",
            energyKwh = 6.0,
            slotStart = "2026-04-05 08:00",
            slotEnd = "2026-04-05 09:00",
            status = MockReservationStatus.Pending,
            canModify = true,
        ),
    )

    private val items = seed.toMutableList()

    fun upcoming(): List<MockReservation> =
        items.filter {
            it.status == MockReservationStatus.Pending ||
                it.status == MockReservationStatus.Approved
        }.sortedBy { it.slotStart }

    fun history(): List<MockReservation> =
        items.filter {
            it.status == MockReservationStatus.Completed ||
                it.status == MockReservationStatus.Cancelled ||
                it.status == MockReservationStatus.Rejected ||
                it.status == MockReservationStatus.Expired
        }.sortedByDescending { it.slotStart }

    fun findById(id: String): MockReservation? = items.find { it.id == id }

    fun update(
        id: String,
        reservationType: String,
        energyKwh: Double,
        slotStart: String,
        slotEnd: String,
    ): MockReservation? {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return null
        val current = items[index]
        if (current.status != MockReservationStatus.Pending) return null
        val updated = current.copy(
            reservationType = reservationType,
            energyKwh = energyKwh,
            slotStart = slotStart,
            slotEnd = slotEnd,
        )
        items[index] = updated
        return updated
    }

    fun cancel(id: String): MockReservation? {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return null
        val current = items[index]
        if (current.status != MockReservationStatus.Pending &&
            current.status != MockReservationStatus.Approved
        ) {
            return null
        }
        val updated = current.copy(
            status = MockReservationStatus.Cancelled,
            qrPayload = null,
            canModify = false,
        )
        items[index] = updated
        return updated
    }

    fun pendingCount(): Int =
        items.count { it.status == MockReservationStatus.Pending }

    fun approvedCount(): Int =
        items.count { it.status == MockReservationStatus.Approved }
}
