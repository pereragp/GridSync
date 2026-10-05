package com.gridsync.mobile.ui.screens.operator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.gridsync.mobile.GridSyncApp
import com.gridsync.mobile.ui.util.toast
import com.gridsync.mobile.data.remote.dto.ReservationDto
import com.gridsync.mobile.ui.theme.ErrorRed200
import com.gridsync.mobile.ui.theme.ErrorRed50
import com.gridsync.mobile.ui.theme.ErrorRed800
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.Slate600
import com.gridsync.mobile.ui.util.formatApiDateTime
import com.gridsync.mobile.ui.util.reservationTypeLabel
import kotlinx.coroutines.launch

@Composable
fun OperatorBookingDetailScreen(
    reservationId: String,
    onBack: () -> Unit = {},
    onOpenScan: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as GridSyncApp
    val scope = rememberCoroutineScope()

    var reservation by remember { mutableStateOf<ReservationDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showReject by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableStateOf(0) }

    LaunchedEffect(reservationId, reloadToken) {
        loading = true
        error = null
        try {
            reservation = app.reservationRepository.getById(reservationId)
        } catch (e: Exception) {
            error = e.message ?: "Could not load booking"
            reservation = null
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid50)
            .statusBarsPadding()
            .navigationBarsPadding(),
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
            Text(
                text = "Booking detail",
                color = Grid900,
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        when {
            loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = Grid700, strokeWidth = 2.dp)
                }
            }

            reservation == null -> {
                Column(modifier = Modifier.padding(20.dp)) {
                    StatusBanner(
                        text = error ?: "Booking not found",
                        isError = true,
                    )
                }
            }

            else -> {
                val item = reservation!!
                val pending = item.status.equals("Pending", ignoreCase = true)
                val approved = item.status.equals("Approved", ignoreCase = true)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                ) {
                    if (error != null) {
                        StatusBanner(text = error.orEmpty(), isError = true, onDismiss = { error = null })
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    if (message != null) {
                        StatusBanner(text = message.orEmpty(), isError = false, onDismiss = { message = null })
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Grid100, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.reservationCode.ifBlank { item.id },
                                color = Grid900,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                            )
                            TypePill(type = item.reservationType)
                            StatusBadge(status = item.status)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        DetailRow("Station", item.stationName ?: "Unavailable")
                        DetailRow("Prosumer NIC", item.prosumerNic ?: "Unavailable")
                        DetailRow(
                            "Type / energy",
                            "${reservationTypeLabel(item.reservationType)} · ${item.energyKwh} kWh",
                        )
                        DetailRow("Slot start", formatApiDateTime(item.slotStart))
                        DetailRow("Slot end", formatApiDateTime(item.slotEnd))
                        if (!item.rejectionReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Rejection reason: ${item.rejectionReason}",
                                color = ErrorRed800,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ErrorRed50)
                                    .padding(10.dp),
                            )
                        }
                    }

                    if (pending) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    message = null
                                    error = null
                                    try {
                                        app.reservationRepository.approve(item.id)
                                        message = "Approved — QR is ready for the prosumer."
                                        app.toast("Booking approved. QR issued.")
                                        reloadToken += 1
                                    } catch (e: Exception) {
                                        error = e.message ?: "Approval failed"
                                        app.toast(error.orEmpty(), long = true)
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Grid700,
                                contentColor = Color.White,
                            ),
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Approve & issue QR", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showReject = true },
                            enabled = !busy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ErrorRed200),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed800),
                        ) {
                            Text("Reject", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    if (approved) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenScan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Grid700,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("Verify QR & complete", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ask the prosumer to present their QR, then verify and complete the transfer on Scan.",
                            color = Slate600,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    if (showReject && reservation != null) {
        val target = reservation!!
        RejectReasonDialog(
            reservationCode = target.reservationCode.ifBlank { target.id },
            onDismiss = { showReject = false },
            onConfirm = { reason ->
                showReject = false
                scope.launch {
                    busy = true
                    message = null
                    error = null
                    try {
                        app.reservationRepository.reject(target.id, reason)
                        message = "Booking rejected."
                        app.toast("Booking rejected.")
                        reloadToken += 1
                    } catch (e: Exception) {
                        error = e.message ?: "Rejection failed"
                        app.toast(error.orEmpty(), long = true)
                    } finally {
                        busy = false
                    }
                }
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label.uppercase(),
            color = Slate600,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Grid900,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
