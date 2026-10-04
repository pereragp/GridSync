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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gridsync.mobile.GridSyncApp
import com.gridsync.mobile.data.remote.dto.BookingSlotDto
import com.gridsync.mobile.data.remote.dto.StationResponseDto
import com.gridsync.mobile.ui.theme.Amber400
import com.gridsync.mobile.ui.theme.Emerald500
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid600
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.Slate300
import com.gridsync.mobile.ui.theme.Slate600
import com.gridsync.mobile.ui.theme.Slate700
import com.gridsync.mobile.ui.theme.Slate900
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun OperatorStationBatteriesScreen(
    stationId: String,
    onBack: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as GridSyncApp
    val scope = rememberCoroutineScope()

    var station by remember { mutableStateOf<StationResponseDto?>(null) }
    var batteries by remember { mutableStateOf<List<BookingSlotDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                station = app.stationRepository.getById(stationId)
                batteries = app.bookingSlotRepository.getForStation(stationId)
                    .sortedBy { it.batteryIndex }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load batteries"
                batteries = emptyList()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(stationId, reloadToken) { load() }

    val availableCount = batteries.count { !it.status.equals("Closed", ignoreCase = true) }

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
                text = station?.name ?: "Battery management",
                color = Grid900,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Close a battery to stop new bookings, or reopen when ready.",
                color = Slate600,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (loading) "…" else "${batteries.size} batteries · $availableCount available",
                color = Grid700,
                style = MaterialTheme.typography.labelLarge,
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(10.dp))
                StatusBanner(text = error.orEmpty(), isError = true, onDismiss = { error = null })
            }
            if (message != null) {
                Spacer(modifier = Modifier.height(10.dp))
                StatusBanner(text = message.orEmpty(), isError = false, onDismiss = { message = null })
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

            batteries.isEmpty() -> {
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
                        text = "No batteries configured",
                        color = Grid900,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This station has no booking slots yet.",
                        color = Slate600,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(batteries, key = { it.id }) { battery ->
                        BatteryCard(
                            battery = battery,
                            busy = busyId == battery.id,
                            onToggle = {
                                scope.launch {
                                    busyId = battery.id
                                    message = null
                                    error = null
                                    try {
                                        if (battery.status.equals("Closed", ignoreCase = true)) {
                                            app.bookingSlotRepository.reopen(battery.id)
                                            message = "Battery #${battery.batteryIndex} reopened."
                                        } else {
                                            app.bookingSlotRepository.close(battery.id)
                                            message = "Battery #${battery.batteryIndex} closed."
                                        }
                                        reloadToken += 1
                                    } catch (e: Exception) {
                                        error = e.message ?: "Battery update failed"
                                    } finally {
                                        busyId = null
                                    }
                                }
                            },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BatteryCard(
    battery: BookingSlotDto,
    busy: Boolean,
    onToggle: () -> Unit,
) {
    val closed = battery.status.equals("Closed", ignoreCase = true)
    val capacity = battery.capacityKwh
    val actual = battery.actualEnergyKwh
    val fillPct = if (capacity > 0) min(100, ((actual / capacity) * 100).roundToInt()) else 0
    val barColor = when {
        closed -> Slate300
        fillPct >= 85 -> Emerald500
        fillPct >= 40 -> Grid600
        else -> Amber400
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, if (closed) Slate300 else Grid100, RoundedCornerShape(16.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(barColor),
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Battery ${battery.batteryIndex}",
                        color = Slate900,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = "${"%.1f".format(capacity)} kWh capacity",
                        color = Slate600,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = if (closed) "Closed" else "Available",
                    color = if (closed) Slate700 else Grid700,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (closed) Color(0xFFE2E8F0) else Grid100)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "STORED",
                    color = Slate600,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = "${"%.2f".format(actual)} kWh · $fillPct%",
                    color = Grid900,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFF1F5F9)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (fillPct / 100f).coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(barColor),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeterChip(
                    label = "Charging space",
                    value = "${"%.2f".format(battery.availableChargingKwh)} kWh",
                    modifier = Modifier.weight(1f),
                )
                MeterChip(
                    label = "Drop-off avail",
                    value = "${"%.2f".format(battery.availableDropOffKwh)} kWh",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            if (closed) {
                Button(
                    onClick = onToggle,
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
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
                        Text("Reopen battery", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onToggle,
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate300),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Grid700,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Close battery", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun MeterChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Grid50)
            .padding(10.dp),
    ) {
        Text(text = label, color = Slate600, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Grid900,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        )
    }
}
