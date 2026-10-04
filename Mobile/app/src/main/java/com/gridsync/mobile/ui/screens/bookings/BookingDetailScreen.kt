package com.gridsync.mobile.ui.screens.bookings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridsync.mobile.GridSyncApp
import com.gridsync.mobile.ui.util.toast
import com.gridsync.mobile.data.remote.dto.ReservationDto
import com.gridsync.mobile.ui.components.MockQrCode
import com.gridsync.mobile.ui.theme.ErrorRed200
import com.gridsync.mobile.ui.theme.ErrorRed50
import com.gridsync.mobile.ui.theme.ErrorRed800
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid600
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.GridSyncMobileTheme
import com.gridsync.mobile.ui.theme.OutfitFontFamily
import com.gridsync.mobile.ui.theme.Slate300
import com.gridsync.mobile.ui.theme.Slate600
import com.gridsync.mobile.ui.theme.Slate700
import com.gridsync.mobile.ui.theme.Slate900
import com.gridsync.mobile.ui.util.formatApiDateTime
import com.gridsync.mobile.ui.util.formatApiDateTimeForEdit
import com.gridsync.mobile.ui.util.localDateTimeToIsoUtc
import com.gridsync.mobile.ui.util.reservationTypeLabel
import kotlinx.coroutines.launch

@Composable
fun BookingDetailScreen(
    reservationId: String,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as GridSyncApp
    val scope = rememberCoroutineScope()

    var reservation by remember { mutableStateOf<ReservationDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            loadError = null
            try {
                reservation = app.reservationRepository.getById(reservationId)
            } catch (e: Exception) {
                loadError = e.message ?: "Could not load booking"
                reservation = null
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(reservationId) {
        load()
    }

    when {
        loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Grid50)
                    .statusBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Grid700, strokeWidth = 2.dp)
            }
        }
        reservation == null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Grid50)
                    .statusBarsPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = loadError ?: "Booking not found",
                    color = Grid900,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onBack) {
                    Text("Go back")
                }
                if (loadError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { load() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Grid700,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        else -> {
            BookingDetailContent(
                reservation = reservation!!,
                onBack = onBack,
                onUpdated = { reservation = it },
            )
        }
    }
}

@Composable
private fun BookingDetailContent(
    reservation: ReservationDto,
    onBack: () -> Unit,
    onUpdated: (ReservationDto) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as GridSyncApp
    val scope = rememberCoroutineScope()

    val editable = reservation.status.equals("Pending", ignoreCase = true)
    val cancellable =
        reservation.status.equals("Pending", ignoreCase = true) ||
            reservation.status.equals("Approved", ignoreCase = true)
    val hasQr =
        reservation.status.equals("Approved", ignoreCase = true) &&
            !reservation.qrPayload.isNullOrBlank()

    var type by remember(reservation.id, reservation.updatedAt) {
        mutableStateOf(reservation.reservationType.ifBlank { "Charging" })
    }
    var energy by remember(reservation.id, reservation.updatedAt) {
        mutableStateOf(reservation.energyKwh.toString())
    }
    var start by remember(reservation.id, reservation.updatedAt) {
        mutableStateOf(formatApiDateTimeForEdit(reservation.slotStart))
    }
    var end by remember(reservation.id, reservation.updatedAt) {
        mutableStateOf(formatApiDateTimeForEdit(reservation.slotEnd))
    }
    var showQr by remember(reservation.id, reservation.qrPayload) { mutableStateOf(hasQr) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var cancelling by remember { mutableStateOf(false) }

    fun save() {
        error = null
        message = null
        val energyValue = energy.toDoubleOrNull()
        when {
            !editable -> error = "Only Pending bookings can be updated"
            energyValue == null || energyValue <= 0 -> error = "Enter a valid energy amount"
            start.isBlank() || end.isBlank() -> error = "Visit start and end are required"
            else -> {
                val startIso = try {
                    localDateTimeToIsoUtc(start)
                } catch (e: IllegalArgumentException) {
                    error = e.message
                    return
                }
                val endIso = try {
                    localDateTimeToIsoUtc(end)
                } catch (e: IllegalArgumentException) {
                    error = e.message
                    return
                }
                saving = true
                scope.launch {
                    try {
                        val updated = app.reservationRepository.update(
                            id = reservation.id,
                            reservationType = type,
                            energyKwh = energyValue,
                            slotStartIso = startIso,
                            slotEndIso = endIso,
                        )
                        message = "Booking updated."
                        app.toast("Booking updated.")
                        onUpdated(updated)
                    } catch (e: Exception) {
                        error = e.message ?: "Could not update this booking"
                        app.toast(error.orEmpty(), long = true)
                    } finally {
                        saving = false
                    }
                }
            }
        }
    }

    fun cancel() {
        error = null
        message = null
        cancelling = true
        scope.launch {
            try {
                val updated = app.reservationRepository.cancel(reservation.id)
                message = "Booking cancelled."
                app.toast("Booking cancelled.")
                showQr = false
                onUpdated(updated)
            } catch (e: Exception) {
                error = e.message ?: "Could not cancel this booking"
                app.toast(error.orEmpty(), long = true)
            } finally {
                cancelling = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid50)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "← Back",
                color = Grid700,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(vertical = 4.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reservation.reservationCode.ifBlank { reservation.id },
                        color = Grid900,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reservation.stationName?.ifBlank { "Station" } ?: "Station",
                        color = Slate600,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                ProsumerStatusBadge(status = reservation.status)
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Banner(text = error!!, error = true)
            }
            if (message != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Banner(text = message!!, error = false)
            }

            if (!reservation.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Banner(text = "Rejected: ${reservation.rejectionReason}", error = true)
            }
            if (!reservation.cancellationReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Banner(text = "Cancelled: ${reservation.cancellationReason}", error = false)
            }

            Spacer(modifier = Modifier.height(18.dp))
            SectionCard {
                InfoRow("Type", reservationTypeLabel(reservation.reservationType))
                DividerPad()
                InfoRow("Energy", "${reservation.energyKwh} kWh")
                DividerPad()
                InfoRow("Status", reservation.status.ifBlank { "Unknown" })
                DividerPad()
                InfoRow("Visit start", formatApiDateTime(reservation.slotStart))
                DividerPad()
                InfoRow("Visit end", formatApiDateTime(reservation.slotEnd))
                if (!reservation.completedAt.isNullOrBlank()) {
                    DividerPad()
                    InfoRow("Completed", formatApiDateTime(reservation.completedAt))
                }
                if (!reservation.cancelledAt.isNullOrBlank()) {
                    DividerPad()
                    InfoRow("Cancelled", formatApiDateTime(reservation.cancelledAt))
                }
                if (!reservation.rejectedAt.isNullOrBlank()) {
                    DividerPad()
                    InfoRow("Rejected", formatApiDateTime(reservation.rejectedAt))
                }
            }

            if (editable) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Update booking",
                    color = Grid900,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pending bookings can be edited. The API enforces the 12-hour notice rule.",
                    color = Slate600,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "TYPE",
                    color = Slate700,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Charging", "DropOff").forEach { option ->
                        val selected = type == option
                        Text(
                            text = reservationTypeLabel(option),
                            color = if (selected) Color.White else Slate700,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Grid700 else Color.White)
                                .border(1.dp, if (selected) Grid700 else Slate300, RoundedCornerShape(10.dp))
                                .clickable {
                                    type = option
                                    error = null
                                    message = null
                                }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Grid600,
                    unfocusedBorderColor = Slate300,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Grid700,
                    focusedTextColor = Slate900,
                    unfocusedTextColor = Slate900,
                )

                Spacer(modifier = Modifier.height(12.dp))
                FieldLabel("Energy (kWh)")
                OutlinedTextField(
                    value = energy,
                    onValueChange = {
                        energy = it
                        error = null
                        message = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors,
                )
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Visit start (YYYY-MM-DD HH:mm)")
                OutlinedTextField(
                    value = start,
                    onValueChange = {
                        start = it
                        error = null
                        message = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors,
                )
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Visit end (YYYY-MM-DD HH:mm)")
                OutlinedTextField(
                    value = end,
                    onValueChange = {
                        end = it
                        error = null
                        message = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors,
                )
            }

            if (hasQr) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Transaction QR",
                        color = Grid900,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Text(
                        text = if (showQr) "Hide" else "Show",
                        color = Grid700,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showQr = !showQr }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                if (showQr) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MockQrCode(
                        payload = reservation.qrPayload.orEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(BorderStroke(1.dp, Grid100))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (editable) {
                Button(
                    onClick = ::save,
                    enabled = !saving && !cancelling,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Grid700,
                        contentColor = Color.White,
                        disabledContainerColor = Grid700.copy(alpha = 0.45f),
                    ),
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving…", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Text("Save changes", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (cancellable) {
                OutlinedButton(
                    onClick = ::cancel,
                    enabled = !saving && !cancelling,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ErrorRed200),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed800),
                ) {
                    if (cancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ErrorRed800,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancelling…", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Text("Cancel booking", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (!editable && !cancellable) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Slate300),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                ) {
                    Text("Back to bookings", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = { content() },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = Slate600, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            color = Slate900,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun DividerPad() {
    HorizontalDivider(
        color = Color(0xFFE2E8F0),
        modifier = Modifier.padding(vertical = 10.dp),
    )
}

@Composable
private fun Banner(text: String, error: Boolean) {
    Text(
        text = text,
        color = if (error) ErrorRed800 else Grid700,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (error) ErrorRed50 else Grid50)
            .border(1.dp, if (error) ErrorRed200 else Grid100, RoundedCornerShape(10.dp))
            .padding(12.dp),
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = Slate700,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Medium,
        ),
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun BookingDetailPreview() {
    GridSyncMobileTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Grid50),
            contentAlignment = Alignment.Center,
        ) {
            Text("BookingDetailScreen", color = Grid900)
        }
    }
}
