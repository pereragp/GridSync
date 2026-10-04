package com.gridsync.mobile.ui.screens.operator

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.gridsync.mobile.GridSyncApp
import com.gridsync.mobile.data.remote.dto.ReservationDto
import com.gridsync.mobile.ui.theme.Amber50
import com.gridsync.mobile.ui.theme.Amber200
import com.gridsync.mobile.ui.theme.Amber800
import com.gridsync.mobile.ui.theme.Emerald50
import com.gridsync.mobile.ui.theme.Emerald200
import com.gridsync.mobile.ui.theme.Emerald900
import com.gridsync.mobile.ui.theme.ErrorRed200
import com.gridsync.mobile.ui.theme.ErrorRed50
import com.gridsync.mobile.ui.theme.ErrorRed800
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid600
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.Slate300
import com.gridsync.mobile.ui.theme.Slate600
import com.gridsync.mobile.ui.theme.Slate700
import com.gridsync.mobile.ui.theme.Slate900
import com.gridsync.mobile.ui.util.formatApiDateTime
import com.gridsync.mobile.ui.util.reservationTypeLabel
import kotlinx.coroutines.launch

private data class StatusFilter(val value: String, val label: String)

private val StatusFilters = listOf(
    StatusFilter("Pending", "Pending"),
    StatusFilter("Approved", "Approved"),
    StatusFilter("Completed", "Completed"),
    StatusFilter("Rejected", "Rejected"),
    StatusFilter("Cancelled", "Cancelled"),
    StatusFilter("Expired", "Expired"),
    StatusFilter("", "All"),
)

@Composable
fun OperatorBookingsScreen(
    onBookingClick: (ReservationDto) -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as GridSyncApp
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("Pending") }
    var search by remember { mutableStateOf("") }
    var reservations by remember { mutableStateOf<List<ReservationDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var rejectTarget by remember { mutableStateOf<ReservationDto?>(null) }
    var reloadToken by remember { mutableStateOf(0) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                reservations = app.reservationRepository.getManaged(status.takeIf { it.isNotBlank() })
            } catch (e: Exception) {
                error = e.message ?: "Failed to load reservations"
                reservations = emptyList()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(status, reloadToken) { load() }

    val visible = remember(reservations, search) {
        val q = search.trim().lowercase()
        if (q.isBlank()) {
            reservations
        } else {
            reservations.filter { reservation ->
                listOf(
                    reservation.reservationCode,
                    reservation.stationName,
                    reservation.prosumerNic,
                    reservation.prosumerId,
                    reservation.reservationType,
                ).filterNotNull().any { it.lowercase().contains(q) }
            }
        }
    }

    val activeLabel = StatusFilters.firstOrNull { it.value == status }?.label ?: "Pending"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid50)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = "Power bookings",
                color = Grid900,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Approve to issue a QR. Energy changes only when the transfer is completed.",
                color = Slate600,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (error != null) {
                StatusBanner(text = error.orEmpty(), isError = true, onDismiss = { error = null })
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (message != null) {
                StatusBanner(text = message.orEmpty(), isError = false, onDismiss = { message = null })
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("Search code, station, NIC, or type…", color = Slate600.copy(alpha = 0.7f))
                },
                shape = RoundedCornerShape(12.dp),
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
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusFilters.forEach { filter ->
                    val active = status == filter.value
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) Grid700 else Color.White)
                            .border(1.dp, if (active) Grid700 else Grid100, RoundedCornerShape(12.dp))
                            .clickable {
                                if (status != filter.value) {
                                    status = filter.value
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = filter.label,
                            color = if (active) Color.White else Slate700,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (loading) "Loading…" else "${visible.size} ${activeLabel.lowercase()} result(s)",
                    color = Slate600,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Refresh",
                    color = Grid700,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !loading) { reloadToken += 1 }
                        .padding(4.dp),
                )
            }
        }

        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Grid700, strokeWidth = 2.dp)
                }
            }

            visible.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Grid100, RoundedCornerShape(16.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Nothing in this queue",
                        color = Grid900,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (search.isNotBlank()) {
                            "Try a different search, or clear the box."
                        } else {
                            "No ${activeLabel.lowercase()} bookings right now."
                        },
                        color = Slate600,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(visible, key = { it.id }) { reservation ->
                        OperatorBookingCard(
                            reservation = reservation,
                            busy = busyId == reservation.id,
                            onClick = { onBookingClick(reservation) },
                            onApprove = {
                                scope.launch {
                                    busyId = reservation.id
                                    message = null
                                    error = null
                                    try {
                                        app.reservationRepository.approve(reservation.id)
                                        message =
                                            "Approved ${reservation.reservationCode} — QR is ready for the prosumer."
                                        reloadToken += 1
                                    } catch (e: Exception) {
                                        error = e.message ?: "Approval failed"
                                    } finally {
                                        busyId = null
                                    }
                                }
                            },
                            onReject = { rejectTarget = reservation },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }

    rejectTarget?.let { target ->
        RejectReasonDialog(
            reservationCode = target.reservationCode.ifBlank { target.id },
            onDismiss = { rejectTarget = null },
            onConfirm = { reason ->
                rejectTarget = null
                scope.launch {
                    busyId = target.id
                    message = null
                    error = null
                    try {
                        app.reservationRepository.reject(target.id, reason)
                        message = "Rejected ${target.reservationCode.ifBlank { target.id }}."
                        reloadToken += 1
                    } catch (e: Exception) {
                        error = e.message ?: "Rejection failed"
                    } finally {
                        busyId = null
                    }
                }
            },
        )
    }
}

@Composable
private fun OperatorBookingCard(
    reservation: ReservationDto,
    busy: Boolean,
    onClick: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val pending = reservation.status.equals("Pending", ignoreCase = true)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = reservation.reservationCode.ifBlank { reservation.id },
                color = Grid900,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
            )
            TypePill(type = reservation.reservationType)
            StatusBadge(status = reservation.status)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaChip(
                label = "Station",
                value = reservation.stationName?.ifBlank { "Unavailable" } ?: "Unavailable",
                modifier = Modifier.weight(1f),
            )
            MetaChip(
                label = "NIC",
                value = reservation.prosumerNic?.ifBlank { "—" } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        MetaChip(
            label = "Energy",
            value = "${reservation.energyKwh} kWh · ${reservationTypeLabel(reservation.reservationType)}",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${formatApiDateTime(reservation.slotStart)} → ${formatApiDateTime(reservation.slotEnd)}",
            color = Slate600,
            style = MaterialTheme.typography.bodySmall,
        )
        if (reservation.status.equals("Rejected", ignoreCase = true) &&
            !reservation.rejectionReason.isNullOrBlank()
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Reason: ${reservation.rejectionReason}",
                color = ErrorRed800,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ErrorRed50)
                    .padding(10.dp),
            )
        }
        if (pending) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    enabled = !busy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Grid700,
                        contentColor = Color.White,
                    ),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Approve & issue QR", style = MaterialTheme.typography.labelLarge)
                    }
                }
                OutlinedButton(
                    onClick = onReject,
                    enabled = !busy,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ErrorRed200),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed800),
                ) {
                    Text("Reject", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
internal fun MetaChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Grid50)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = Slate600,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Slate900,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

@Composable
internal fun StatusBadge(status: String) {
    val (bg, fg, border) = when (status) {
        "Pending" -> Triple(Amber50, Amber800, Amber200)
        "Approved" -> Triple(Emerald50, Emerald900, Emerald200)
        "Completed" -> Triple(Grid100, Grid700, Grid100)
        "Rejected" -> Triple(ErrorRed50, ErrorRed800, ErrorRed200)
        else -> Triple(Color(0xFFF1F5F9), Slate700, Slate300)
    }
    Text(
        text = status,
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
