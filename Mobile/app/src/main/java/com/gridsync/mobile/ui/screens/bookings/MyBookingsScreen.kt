package com.gridsync.mobile.ui.screens.bookings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gridsync.mobile.GridSyncApp
import com.gridsync.mobile.data.remote.dto.ReservationDto
import com.gridsync.mobile.ui.theme.ErrorRed200
import com.gridsync.mobile.ui.theme.ErrorRed50
import com.gridsync.mobile.ui.theme.ErrorRed800
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
import com.gridsync.mobile.ui.util.formatApiDateTime
import com.gridsync.mobile.ui.util.reservationTypeLabel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private enum class BookingsTab(val label: String) {
    Upcoming("Upcoming"),
    History("History"),
}

@Composable
fun MyBookingsScreen(
    showBack: Boolean = true,
    initialTabHistory: Boolean = false,
    onBack: () -> Unit = {},
    onBookingClick: (ReservationDto) -> Unit = {},
    onCreateBooking: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as GridSyncApp
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var tab by remember {
        mutableStateOf(if (initialTabHistory) BookingsTab.History else BookingsTab.Upcoming)
    }
    var search by remember { mutableStateOf("") }
    var upcomingStatusFilter by remember { mutableStateOf<String?>(null) }
    var historyStatusFilter by remember { mutableStateOf<String?>(null) }
    var upcoming by remember { mutableStateOf<List<ReservationDto>>(emptyList()) }
    var history by remember { mutableStateOf<List<ReservationDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var upcomingError by remember { mutableStateOf<String?>(null) }
    var historyError by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            upcomingError = null
            historyError = null
            try {
                coroutineScope {
                    val upcomingDeferred = async {
                        runCatching { app.reservationRepository.getUpcoming() }
                    }
                    val historyDeferred = async {
                        runCatching { app.reservationRepository.getHistory() }
                    }

                    val upcomingResult = upcomingDeferred.await()
                    val historyResult = historyDeferred.await()

                    upcoming = upcomingResult.getOrElse {
                        upcomingError = it.message ?: "Could not load upcoming bookings"
                        emptyList()
                    }
                    history = historyResult.getOrElse {
                        historyError = it.message ?: "Could not load booking history"
                        emptyList()
                    }
                }
            } finally {
                loading = false
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val statusFilter = if (tab == BookingsTab.Upcoming) upcomingStatusFilter else historyStatusFilter
    val source = if (tab == BookingsTab.Upcoming) upcoming else history
    val tabError = if (tab == BookingsTab.Upcoming) upcomingError else historyError

    val filtered = source.filter { reservation ->
        val matchesStatus = statusFilter == null ||
            reservation.status.equals(statusFilter, ignoreCase = true)
        if (!matchesStatus) return@filter false
        if (search.isBlank()) return@filter true
        listOf(
            reservation.reservationCode,
            reservation.stationName.orEmpty(),
            reservation.reservationType,
            reservation.status,
            reservation.rejectionReason.orEmpty(),
            reservation.cancellationReason.orEmpty(),
        ).any { it.contains(search, ignoreCase = true) }
    }

    val statusOptions = if (tab == BookingsTab.Upcoming) {
        listOf(null, "Pending", "Approved")
    } else {
        listOf(null, "Completed", "Cancelled", "Rejected", "Expired")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid50)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            if (showBack) {
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
            }
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
                        text = if (tab == BookingsTab.History) {
                            "Past transfers, cancellations, and expired slots."
                        } else {
                            "Upcoming visits awaiting transfer or approval."
                        },
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
                upcomingCount = upcoming.size,
                historyCount = history.size,
                onSelect = { tab = it },
            )
            Spacer(modifier = Modifier.height(12.dp))
            SearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = if (tab == BookingsTab.History) {
                    "Search history by code, station, status…"
                } else {
                    "Search code, station, type…"
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
            StatusFilterRow(
                options = statusOptions,
                selected = statusFilter,
                onSelect = { selected ->
                    if (tab == BookingsTab.Upcoming) {
                        upcomingStatusFilter = selected
                    } else {
                        historyStatusFilter = selected
                    }
                },
            )
        }

        when {
            loading && source.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Grid700, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (tab == BookingsTab.History) {
                                "Loading booking history…"
                            } else {
                                "Loading upcoming bookings…"
                            },
                            color = Slate600,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            tabError != null && source.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = tabError,
                        color = ErrorRed800,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ErrorRed50)
                            .border(1.dp, ErrorRed200, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { reload() },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Retry")
                    }
                }
            }
            filtered.isEmpty() -> {
                EmptyBookings(
                    tab = tab,
                    hasSourceItems = source.isNotEmpty(),
                    onCreateBooking = onCreateBooking,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (tabError != null) {
                        item {
                            Text(
                                text = tabError,
                                color = ErrorRed800,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ErrorRed50)
                                    .border(1.dp, ErrorRed200, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (tab == BookingsTab.History) {
                                    "History · showing ${filtered.size} of ${source.size}"
                                } else {
                                    "Showing ${filtered.size} of ${source.size}"
                                },
                                color = Slate600,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Grid700,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = "Refresh",
                                    color = Grid700,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { reload() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(filtered, key = { it.id }) { reservation ->
                        BookingListCard(
                            reservation = reservation,
                            showHistoryDetails = tab == BookingsTab.History,
                            onClick = { onBookingClick(reservation) },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TabRow(
    selected: BookingsTab,
    upcomingCount: Int,
    historyCount: Int,
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
            val count = if (tab == BookingsTab.Upcoming) upcomingCount else historyCount
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
                    text = "${tab.label} ($count)",
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
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(placeholder, color = Slate600.copy(alpha = 0.7f))
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
    options: List<String?>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { status ->
            val label = status ?: "All"
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
    hasSourceItems: Boolean,
    onCreateBooking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = when {
                tab == BookingsTab.History && hasSourceItems -> "No history matches this filter"
                tab == BookingsTab.History -> "No booking history yet"
                hasSourceItems -> "No upcoming matches this filter"
                else -> "No upcoming reservations"
            },
            color = Grid900,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                tab == BookingsTab.History && !hasSourceItems ->
                    "Completed, cancelled, rejected, and expired bookings will appear here."
                hasSourceItems -> "Try another status filter or clear search."
                else -> "Create a booking to get started."
            },
            color = Slate600,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (tab == BookingsTab.Upcoming && !hasSourceItems) {
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
    reservation: ReservationDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showHistoryDetails: Boolean = false,
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
                    text = reservation.reservationCode.ifBlank { reservation.id },
                    color = Slate900,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = reservation.stationName?.ifBlank { "Station" } ?: "Station",
                    color = Slate600,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            ProsumerStatusBadge(status = reservation.status)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${reservationTypeLabel(reservation.reservationType)} · ${reservation.energyKwh} kWh",
            color = Slate700,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${formatApiDateTime(reservation.slotStart)} – ${formatApiDateTime(reservation.slotEnd)}",
            color = Slate600,
            style = MaterialTheme.typography.bodySmall,
        )

        if (showHistoryDetails) {
            val detailLine = when {
                reservation.status.equals("Completed", ignoreCase = true) &&
                    !reservation.completedAt.isNullOrBlank() ->
                    "Completed ${formatApiDateTime(reservation.completedAt)}"
                reservation.status.equals("Rejected", ignoreCase = true) &&
                    !reservation.rejectionReason.isNullOrBlank() ->
                    "Reason: ${reservation.rejectionReason}"
                reservation.status.equals("Cancelled", ignoreCase = true) &&
                    !reservation.cancellationReason.isNullOrBlank() ->
                    "Reason: ${reservation.cancellationReason}"
                reservation.status.equals("Cancelled", ignoreCase = true) ->
                    "Cancelled by you or the system"
                reservation.status.equals("Expired", ignoreCase = true) ->
                    "Visit window passed without completion"
                else -> null
            }
            if (detailLine != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = detailLine,
                    color = if (reservation.status.equals("Rejected", ignoreCase = true)) {
                        ErrorRed800
                    } else {
                        Slate600
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (reservation.status.equals("Approved", ignoreCase = true) &&
            !reservation.qrPayload.isNullOrBlank()
        ) {
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
fun ProsumerStatusBadge(status: String) {
    val (bg, fg, border) = when (status) {
        "Pending" -> Triple(Color(0xFFFFF7ED), Color(0xFF9A3412), Color(0xFFFDBA74))
        "Approved" -> Triple(Grid50, Grid700, Grid100)
        "Completed" -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), Color(0xFFBFDBFE))
        "Cancelled",
        "Rejected",
        "Expired",
        -> Triple(Color(0xFFF8FAFC), Slate700, Slate300)
        else -> Triple(Color(0xFFF8FAFC), Slate700, Slate300)
    }
    Text(
        text = status.ifBlank { "Unknown" },
        color = fg,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MyBookingsPreview() {
    GridSyncMobileTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Grid50),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Grid700,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("MyBookingsScreen", color = Grid900)
            }
        }
    }
}
