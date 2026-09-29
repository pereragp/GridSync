package com.gridsync.mobile.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gridsync.mobile.ui.components.AUTH_HERO_IMAGE_URL
import com.gridsync.mobile.ui.components.BrandLogo
import com.gridsync.mobile.ui.components.BrandLogoVariant
import com.gridsync.mobile.ui.data.MockReservationRepository
import com.gridsync.mobile.ui.data.MockStationRepository
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid500
import com.gridsync.mobile.ui.theme.Grid600
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid800
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.GridSyncMobileTheme
import com.gridsync.mobile.ui.theme.OutfitFontFamily
import com.gridsync.mobile.ui.theme.Slate600
import com.gridsync.mobile.ui.theme.Slate700
import com.gridsync.mobile.ui.theme.Slate900
import com.gridsync.mobile.ui.theme.SourceSerifFontFamily

data class NearbyStationUi(
    val id: String,
    val name: String,
    val code: String,
    val distanceKm: Double,
    val availableSlots: Int,
    val status: String,
    /** Relative map pin position 0..1 */
    val mapX: Float,
    val mapY: Float,
)

private val MockNearbyStations = MockStationRepository.stations.map { station ->
    NearbyStationUi(
        id = station.id,
        name = station.name,
        code = station.code,
        distanceKm = station.distanceKm,
        availableSlots = station.availableBatterySlots,
        status = station.status,
        mapX = station.mapX,
        mapY = station.mapY,
    )
}

@Composable
fun ProsumerDashboardScreen(
    userName: String = "Prosumer",
    pendingCount: Int = MockReservationRepository.pendingCount(),
    activeCount: Int = MockReservationRepository.approvedCount(),
    nearbyStations: List<NearbyStationUi> = MockNearbyStations,
    onBookEnergy: () -> Unit = {},
    onMyBookings: () -> Unit = {},
    onStationClick: (NearbyStationUi) -> Unit = {},
) {
    var selectedStationId by remember { mutableStateOf(nearbyStations.firstOrNull()?.id) }
    val firstName = userName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "there" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid50),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            DashboardHero(
                firstName = firstName,
                onBookEnergy = onBookEnergy,
                onMyBookings = onMyBookings,
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your activity",
                            color = Grid900,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Live counts from your open energy reservations.",
                            color = Slate600,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = "View all →",
                        color = Grid700,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onMyBookings)
                            .padding(4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        label = "Pending",
                        value = pendingCount.toString(),
                        hint = "Awaiting review",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onMyBookings),
                    )
                    StatCard(
                        label = "Active",
                        value = activeCount.toString(),
                        hint = "Approved bookings",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onMyBookings),
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nearby grid nodes",
                            color = Grid900,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mock map preview — GPS + API wiring comes next.",
                            color = Slate600,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = "${nearbyStations.size} nearby",
                        color = Grid700,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                NearbyMapPreview(
                    stations = nearbyStations,
                    selectedStationId = selectedStationId,
                    onSelectStation = { selectedStationId = it.id },
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        items(nearbyStations, key = { it.id }) { station ->
            NearbyStationRow(
                station = station,
                selected = station.id == selectedStationId,
                onClick = {
                    selectedStationId = station.id
                    onStationClick(station)
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun DashboardHero(
    firstName: String,
    onBookEnergy: () -> Unit,
    onMyBookings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
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
                            Grid900.copy(alpha = 0.88f),
                            Grid800.copy(alpha = 0.72f),
                            Grid700.copy(alpha = 0.45f),
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Grid50,
                        )
                    )
                )
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
                    text = "Welcome back, $firstName.",
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = SourceSerifFontFamily,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reserve Charging or Drop-off energy, track approvals, and present your QR at the hub.",
                    color = Grid100.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onBookEnergy,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Grid800,
                        ),
                    ) {
                        Text("Book energy", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onMyBookings,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) {
                        Text("My bookings", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = Grid600,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = Grid900,
            style = MaterialTheme.typography.displayMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = hint,
            color = Slate600,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun NearbyMapPreview(
    stations: List<NearbyStationUi>,
    selectedStationId: String?,
    onSelectStation: (NearbyStationUi) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Grid100, RoundedCornerShape(16.dp))
    ) {
        // Stylized map base — swap for Google Maps when API key is ready
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFD7E8DC),
                        Color(0xFFB7D4C2),
                        Color(0xFF9BC4AE),
                    )
                )
            )
            val roadColor = Color.White.copy(alpha = 0.55f)
            drawPath(
                path = Path().apply {
                    moveTo(0f, size.height * 0.35f)
                    cubicTo(
                        size.width * 0.25f, size.height * 0.2f,
                        size.width * 0.55f, size.height * 0.55f,
                        size.width, size.height * 0.4f,
                    )
                },
                color = roadColor,
                style = Stroke(width = 10.dp.toPx()),
            )
            drawPath(
                path = Path().apply {
                    moveTo(size.width * 0.1f, size.height)
                    cubicTo(
                        size.width * 0.35f, size.height * 0.7f,
                        size.width * 0.6f, size.height * 0.85f,
                        size.width * 0.95f, size.height * 0.55f,
                    )
                },
                color = roadColor,
                style = Stroke(width = 7.dp.toPx()),
            )
            val grid = Color(0xFF0C3D25).copy(alpha = 0.06f)
            for (i in 1..5) {
                val x = size.width * i / 6f
                val y = size.height * i / 6f
                drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            val you = Offset(size.width * 0.48f, size.height * 0.55f)
            drawCircle(Color.White, radius = 10.dp.toPx(), center = you)
            drawCircle(Grid500, radius = 6.dp.toPx(), center = you)
            drawCircle(
                color = Grid500.copy(alpha = 0.2f),
                radius = 22.dp.toPx(),
                center = you,
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            stations.forEach { station ->
                val selected = station.id == selectedStationId
                val pinSize = if (selected) 28.dp else 22.dp
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            start = (maxWidth * station.mapX - pinSize / 2).coerceAtLeast(0.dp),
                            top = (maxHeight * station.mapY - pinSize / 2).coerceAtLeast(0.dp),
                        )
                ) {
                    MapPin(
                        selected = selected,
                        onClick = { onSelectStation(station) },
                    )
                }
            }
        }

        Text(
            text = "YOU",
            color = Grid800,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )

        Text(
            text = "Map preview",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Grid900.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun MapPin(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(if (selected) 28.dp else 22.dp)
            .clip(CircleShape)
            .background(if (selected) Grid700 else Color.White)
            .border(2.dp, if (selected) Color.White else Grid700, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else Grid700)
        )
    }
}

@Composable
private fun NearbyStationRow(
    station: NearbyStationUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Grid100.copy(alpha = 0.55f) else Color.White)
            .border(
                width = 1.dp,
                color = if (selected) Grid500.copy(alpha = 0.45f) else Grid100,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = Slate900,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = station.code,
                    color = Slate600,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = String.format("%.1f km", station.distanceKm),
                color = Grid700,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFFE2E8F0))
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${station.availableSlots} slots",
                color = Slate700,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Grid100)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = station.status,
                    color = Grid700,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProsumerDashboardPreview() {
    GridSyncMobileTheme {
        ProsumerDashboardScreen(userName = "Ayesha Perera")
    }
}
