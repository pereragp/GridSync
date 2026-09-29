package com.gridsync.mobile.ui.screens.station

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gridsync.mobile.ui.components.AUTH_HERO_IMAGE_URL
import com.gridsync.mobile.ui.data.MockBattery
import com.gridsync.mobile.ui.data.MockStationDetail
import com.gridsync.mobile.ui.data.MockStationRepository
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid600
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid800
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.GridSyncMobileTheme
import com.gridsync.mobile.ui.theme.OutfitFontFamily
import com.gridsync.mobile.ui.theme.Slate300
import com.gridsync.mobile.ui.theme.Slate600
import com.gridsync.mobile.ui.theme.Slate700
import com.gridsync.mobile.ui.theme.Slate900
import com.gridsync.mobile.ui.theme.SourceSerifFontFamily

@Composable
fun StationDetailScreen(
    stationId: String,
    onBack: () -> Unit = {},
    onReserve: (stationId: String) -> Unit = {},
) {
    val station = MockStationRepository.findById(stationId)

    if (station == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Grid50)
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Station not found", color = Slate900, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onBack) {
                Text("Go back")
            }
        }
        return
    }

    StationDetailContent(
        station = station,
        onBack = onBack,
        onReserve = { onReserve(station.id) },
    )
}

@Composable
private fun StationDetailContent(
    station: MockStationDetail,
    onBack: () -> Unit,
    onReserve: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid50)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            StationHero(station = station, onBack = onBack)

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = station.description,
                    color = Slate700,
                    style = MaterialTheme.typography.bodyLarge,
                )

                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle("Capacity")
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricChip(
                        label = "Batteries",
                        value = station.availableBatterySlots.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    MetricChip(
                        label = "Per battery",
                        value = "${station.batteryCapacityKwh.toInt()} kWh",
                        modifier = Modifier.weight(1f),
                    )
                    MetricChip(
                        label = "Total",
                        value = "${station.totalCapacityKwh.toInt()} kWh",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
                SectionTitle("Schedule")
                Spacer(modifier = Modifier.height(10.dp))
                InfoCard {
                    InfoRow("Hours", "${station.openTime} – ${station.closeTime}")
                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 10.dp))
                    InfoRow("Working days", station.workingDays.joinToString(" · "))
                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 10.dp))
                    InfoRow("Location", String.format("%.4f, %.4f", station.latitude, station.longitude))
                }

                Spacer(modifier = Modifier.height(22.dp))
                SectionTitle("Batteries on site")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${station.batteries.size} units with live Charging / Drop-off availability.",
                    color = Slate600,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                station.batteries.forEach { battery ->
                    BatteryCard(batteryIndex = battery.batteryIndex, battery = battery)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(Color.White)
                .border(BorderStroke(1.dp, Grid100))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Slate300),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
            ) {
                Text("Back", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onReserve,
                enabled = station.status == "Active",
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Grid700,
                    contentColor = Color.White,
                    disabledContainerColor = Grid700.copy(alpha = 0.45f),
                ),
            ) {
                Text("Reserve energy", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun StationHero(
    station: MockStationDetail,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        AsyncImage(
            model = AUTH_HERO_IMAGE_URL,
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
                            Grid900.copy(alpha = 0.75f),
                            Grid800.copy(alpha = 0.55f),
                            Grid50,
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("← Back", style = MaterialTheme.typography.labelLarge)
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(station.status)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format("%.1f km away", station.distanceKm),
                        color = Grid100.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = station.name,
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = SourceSerifFontFamily,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = station.code,
                    color = Grid100.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    Text(
        text = status.uppercase(),
        color = Color.White,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Grid600)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Grid900,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = Grid600,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = Slate900,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
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
private fun BatteryCard(
    batteryIndex: Int,
    battery: MockBattery,
) {
    val fillPct = if (battery.capacityKwh > 0) {
        ((battery.actualEnergyKwh / battery.capacityKwh) * 100).toInt().coerceIn(0, 100)
    } else {
        0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "Battery #$batteryIndex",
            color = Grid900,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Stored", color = Slate600, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "${battery.actualEnergyKwh} / ${battery.capacityKwh} kWh",
                color = Slate700,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillPct / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Grid600)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat("Charge avail", "${battery.availableChargingKwh} kWh", Modifier.weight(1f))
            MiniStat("Drop-off avail", "${battery.availableDropOffKwh} kWh", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Grid50)
            .border(1.dp, Grid100, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(text = label, color = Slate600, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Grid800,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StationDetailPreview() {
    GridSyncMobileTheme {
        StationDetailScreen(stationId = "1")
    }
}
