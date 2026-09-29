package com.gridsync.mobile.ui.screens.bookings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gridsync.mobile.ui.data.MockReservation
import com.gridsync.mobile.ui.data.MockReservationRepository
import com.gridsync.mobile.ui.data.MockReservationStatus
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid600
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.GridSyncMobileTheme
import com.gridsync.mobile.ui.theme.Slate300
import com.gridsync.mobile.ui.theme.Slate600
import com.gridsync.mobile.ui.theme.Slate700
import com.gridsync.mobile.ui.theme.Slate900

private enum class BookingsTab(val label: String) {
    Upcoming("Upcoming"),
    History("History"),
}

@Composable
fun MyBookingsScreen(
    initialTabHistory: Boolean = false,
    onBack: () -> Unit = {},
    onBookingClick: (MockReservation) -> Unit = {},
    onCreateBooking: () -> Unit = {},
) {
    var tab by remember {
        mutableStateOf(if (initialTabHistory) BookingsTab.History else BookingsTab.Upcoming)
    }
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<MockReservationStatus?>(null) }

    val upcoming = MockReservationRepository.upcoming()
    val history = MockReservationRepository.history()
    val source = if (tab == BookingsTab.Upcoming) upcoming else history

    val filtered = source.filter { reservation ->
        val matchesSearch = search.isBlank() ||
            listOf(
                reservation.reservationCode,
                reservation.stationName,
                reservation.reservationType,
            ).any { it.contains(search, ignoreCase = true) }
        val matchesStatus = statusFilter == null || reservation.status == statusFilter
        matchesSearch && matchesStatus
    }

    val statusOptions = if (tab == BookingsTab.Upcoming) {
        listOf(null, MockReservationStatus.Pending, MockReservationStatus.Approved)
    } else {
        listOf(
            null,
            MockReservationStatus.Completed,
            MockReservationStatus.Cancelled,
            MockReservationStatus.Rejected,
            MockReservationStatus.Expired,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid50)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = "← Back",
                color = Grid700,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(vertical = 4.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My bookings",
                        color = Grid900,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Upcoming visits and past energy transfers.",
                        color = Slate600,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    onClick = onCreateBooking,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Grid700,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("Book", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TabRow(
                selected = tab,
                onSelect = {
                    tab = it
                    statusFilter = null
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            SearchField(
                value = search,
                onValueChange = { search = it },
            )
            Spacer(modifier = Modifier.height(10.dp))
            StatusFilterRow(
                options = statusOptions,
                selected = statusFilter,
                onSelect = { statusFilter = it },
            )
        }

        if (filtered.isEmpty()) {
            EmptyBookings(
                tab = tab,
                onCreateBooking = onCreateBooking,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        text = "Showing ${filtered.size} of ${source.size}",
                        color = Slate600,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(filtered, key = { it.id }) { reservation ->
                    BookingListCard(
                        reservation = reservation,
                        onClick = { onBookingClick(reservation) },
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun TabRow(
    selected: BookingsTab,
    onSelect: (BookingsTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BookingsTab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) Grid700 else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    color = if (active) Color.White else Slate700,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text("Search code, station, type…", color = Slate600.copy(alpha = 0.7f))
        },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Grid600,
            unfocusedBorderColor = Slate300,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = Grid700,
            focusedTextColor = Slate900,
            unfocusedTextColor = Slate900,
        ),
    )
}

@Composable
private fun StatusFilterRow(
    options: List<MockReservationStatus?>,
    selected: MockReservationStatus?,
    onSelect: (MockReservationStatus?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { status ->
            val label = status?.name ?: "All"
            val active = selected == status
            Text(
                text = label,
                color = if (active) Color.White else Slate700,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) Grid700 else Color.White)
                    .border(1.dp, if (active) Grid700 else Slate300, RoundedCornerShape(999.dp))
                    .clickable { onSelect(status) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun EmptyBookings(
    tab: BookingsTab,
    onCreateBooking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (tab == BookingsTab.Upcoming) {
                "No upcoming reservations"
            } else {
                "No past reservations"
            },
            color = Grid900,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Try another filter, or create a new booking.",
            color = Slate600,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (tab == BookingsTab.Upcoming) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateBooking,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Grid700,
                    contentColor = Color.White,
                ),
            ) {
                Text("Book energy", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun BookingListCard(
    reservation: MockReservation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reservation.reservationCode,
                    color = Slate900,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = reservation.stationName,
                    color = Slate600,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            StatusBadge(status = reservation.status)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${typeLabel(reservation.reservationType)} · ${reservation.energyKwh} kWh · Battery #${reservation.batteryIndex}",
            color = Slate700,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${reservation.slotStart} – ${reservation.slotEnd}",
            color = Slate600,
            style = MaterialTheme.typography.bodySmall,
        )
        if (reservation.status == MockReservationStatus.Approved && reservation.qrPayload != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "QR ready →",
                color = Grid700,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
fun StatusBadge(status: MockReservationStatus) {
    val (bg, fg, border) = when (status) {
        MockReservationStatus.Pending -> Triple(Color(0xFFFFF7ED), Color(0xFF9A3412), Color(0xFFFDBA74))
        MockReservationStatus.Approved -> Triple(Grid50, Grid700, Grid100)
        MockReservationStatus.Completed -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), Color(0xFFBFDBFE))
        MockReservationStatus.Cancelled,
        MockReservationStatus.Rejected,
        MockReservationStatus.Expired,
        -> Triple(Color(0xFFF8FAFC), Slate700, Slate300)
    }
    Text(
        text = status.name,
        color = fg,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

fun typeLabel(type: String): String =
    if (type == "DropOff") "Drop-off" else type

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MyBookingsPreview() {
    GridSyncMobileTheme {
        MyBookingsScreen()
    }
}
