package com.gridsync.mobile.ui.screens.operator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gridsync.mobile.GridSyncApp
import com.gridsync.mobile.data.remote.dto.ReservationDashboardStatsDto
import com.gridsync.mobile.data.remote.dto.ReservationDto
import com.gridsync.mobile.ui.components.BrandLogo
import com.gridsync.mobile.ui.components.BrandLogoVariant
import com.gridsync.mobile.ui.theme.Amber50
import com.gridsync.mobile.ui.theme.Amber200
import com.gridsync.mobile.ui.theme.Amber800
import com.gridsync.mobile.ui.theme.Amber950
import com.gridsync.mobile.ui.theme.ErrorRed200
import com.gridsync.mobile.ui.theme.ErrorRed50
import com.gridsync.mobile.ui.theme.ErrorRed800
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid800
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.Slate600
import com.gridsync.mobile.ui.theme.SourceSerifFontFamily
import com.gridsync.mobile.ui.util.reservationTypeLabel
import kotlinx.coroutines.launch

private const val OPERATOR_HERO_IMAGE =
    "https://images.unsplash.com/photo-1466611653911-95081537e5b7?auto=format&fit=crop&w=2000&q=80"

@Composable
fun OperatorDashboardScreen(
    userName: String = "Operator",
    onReviewBookings: () -> Unit = {},
    onOpenStations: () -> Unit = {},
    onScanQr: () -> Unit = {},
    onBookingClick: (ReservationDto) -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as GridSyncApp
    val scope = rememberCoroutineScope()

    var stats by remember { mutableStateOf<ReservationDashboardStatsDto?>(null) }
    var pending by remember { mutableStateOf<List<ReservationDto>>(emptyList()) }
    var stationCount by remember { mutableStateOf(0) }
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
                val dashboard = app.reservationRepository.getDashboardStats()
                val pendingList = app.reservationRepository.getManaged("Pending")
                val stations = runCatching { app.stationRepository.getAll() }.getOrDefault(emptyList())
                stats = dashboard
                pending = pendingList
                stationCount = stations.size
            } catch (e: Exception) {
                error = e.message ?: "Failed to load overview"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(reloadToken) { load() }

    val firstName = userName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "Operator" }
    val pendingCount = stats?.pendingReservations ?: pending.size.toLong()
    val approvedCount = stats?.approvedUpcomingReservations ?: 0L
    val awaiting = pending.take(4)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid50),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            OperatorHero(
                firstName = firstName,
                pendingCount = pendingCount.toInt(),
                onReviewBookings = onReviewBookings,
                onOpenStations = onOpenStations,
                onScanQr = onScanQr,
            )
        }

        if (error != null) {
            item {
                StatusBanner(
                    text = error.orEmpty(),
                    isError = true,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    onDismiss = { error = null },
                )
            }
        }
        if (message != null) {
            item {
                StatusBanner(
                    text = message.orEmpty(),
                    isError = false,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    onDismiss = { message = null },
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Your console",
                    color = Grid900,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Review trading bookings and keep battery availability accurate.",
                    color = Slate600,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(14.dp))
                PathCard(
                    eyebrow = "Trading",
                    title = "Power bookings",
                    detail = "Approve or reject Charging and Drop-off requests.",
                    meta = if (loading) "…" else "$pendingCount pending · $approvedCount approved upcoming",
                    accent = Amber400Bar,
                    onClick = onReviewBookings,
                )
                Spacer(modifier = Modifier.height(10.dp))
                PathCard(
                    eyebrow = "Stations",
                    title = "Battery availability",
                    detail = "Close or reopen batteries to keep slot capacity accurate.",
                    meta = if (loading) "…" else "$stationCount stations",
                    accent = Grid700,
                    onClick = onOpenStations,
                )
            }
        }

        item {
            PendingQueueHeader(
                pendingCount = pendingCount.toInt(),
                loading = loading,
                onOpenFull = onReviewBookings,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Grid700, strokeWidth = 2.dp)
                }
            }
        } else if (pending.isEmpty()) {
            item {
                EmptyQueueCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
        } else {
            items(awaiting, key = { it.id }) { reservation ->
                PendingBookingRow(
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
                                message = "Approved ${reservation.reservationCode}. QR is ready for the prosumer."
                                reloadToken += 1
                            } catch (e: Exception) {
                                error = e.message ?: "Approval failed"
                            } finally {
                                busyId = null
                            }
                        }
                    },
                    onReject = { rejectTarget = reservation },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                )
            }
            if (pending.size > 4) {
                item {
                    Text(
                        text = "View all ${pending.size} pending bookings →",
                        color = Grid700,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .clickable(onClick = onReviewBookings),
                    )
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

private val Amber400Bar = Color(0xFFFBBF24)

@Composable
private fun OperatorHero(
    firstName: String,
    pendingCount: Int,
    onReviewBookings: () -> Unit,
    onOpenStations: () -> Unit,
    onScanQr: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        AsyncImage(
            model = OPERATOR_HERO_IMAGE,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Grid900.copy(alpha = 0.88f),
                            Grid800.copy(alpha = 0.72f),
                            Grid700.copy(alpha = 0.45f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, Grid50),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            BrandLogo(variant = BrandLogoVariant.Header)
            Column {
                Text(
                    text = "GridSync · Grid Operator",
                    color = Grid100.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ready when you are, $firstName",
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = SourceSerifFontFamily,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Review bookings, update battery availability, and complete on-site transfers.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                )

            }
        }
    }
}

@Composable
private fun PathCard(
    eyebrow: String,
    title: String,
    detail: String,
    meta: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = eyebrow.uppercase(),
            color = Slate600,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = Grid900,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = detail,
            color = Slate600,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "$meta →",
            color = Grid700,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun PendingQueueHeader(
    pendingCount: Int,
    loading: Boolean,
    onOpenFull: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(
                Brush.verticalGradient(listOf(Amber50, Color.White)),
            )
            .border(1.dp, Amber200, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "NEEDS ATTENTION",
            color = Amber800,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Pending booking requests",
            color = Amber950,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (loading) "…" else "$pendingCount waiting",
                color = Amber800,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Amber200.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Text(
                text = "Open full queue",
                color = Amber950,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenFull)
                    .padding(4.dp),
            )
        }
    }
}

@Composable
private fun EmptyQueueCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(Color.White)
            .border(1.dp, Amber200, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Queue is clear",
            color = Grid900,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "No pending Charging or Drop-off requests right now.",
            color = Slate600,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PendingBookingRow(
    reservation: ReservationDto,
    busy: Boolean,
    onClick: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = reservation.reservationCode.ifBlank { reservation.id },
                color = Grid900,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TypePill(type = reservation.reservationType)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = listOfNotNull(
                reservation.stationName?.takeIf { it.isNotBlank() },
                reservation.prosumerNic?.takeIf { it.isNotBlank() },
                "${reservation.energyKwh} kWh",
            ).joinToString(" · "),
            color = Slate600,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
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
                    Text("Approve", style = MaterialTheme.typography.labelLarge)
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

@Composable
internal fun TypePill(type: String) {
    val isDrop = type == "DropOff"
    Text(
        text = reservationTypeLabel(type).uppercase(),
        color = if (isDrop) Color(0xFF075985) else Amber800,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isDrop) Color(0xFFF0F9FF) else Amber50)
            .border(
                1.dp,
                if (isDrop) Color(0xFFBAE6FD) else Amber200,
                RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
internal fun StatusBanner(
    text: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    Text(
        text = text,
        color = if (isError) ErrorRed800 else Grid800,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isError) ErrorRed50 else Grid100)
            .border(
                1.dp,
                if (isError) ErrorRed200 else Grid100,
                RoundedCornerShape(10.dp),
            )
            .then(
                if (onDismiss != null) {
                    Modifier.clickable(onClick = onDismiss)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}
