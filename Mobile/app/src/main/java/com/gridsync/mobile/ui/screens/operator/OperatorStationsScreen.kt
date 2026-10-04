package com.gridsync.mobile.ui.screens.operator

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.gridsync.mobile.data.remote.dto.StationResponseDto
import com.gridsync.mobile.ui.theme.Emerald50
import com.gridsync.mobile.ui.theme.Emerald200
import com.gridsync.mobile.ui.theme.Emerald900
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

private enum class StationFilter(val label: String) {
    All("All"),
    Active("Active"),
    Inactive("Inactive"),
}

@Composable
fun OperatorStationsScreen(
    onStationClick: (StationResponseDto) -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as GridSyncApp
    val scope = rememberCoroutineScope()

    var stations by remember { mutableStateOf<List<StationResponseDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(StationFilter.All) }
    var reloadToken by remember { mutableStateOf(0) }

    LaunchedEffect(reloadToken) {
        loading = true
        error = null
        try {
            stations = app.stationRepository.getAll()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load stations"
            stations = emptyList()
        } finally {
            loading = false
        }
    }

    val visible = remember(stations, search, filter) {
        stations.filter { station ->
            val matchesFilter = when (filter) {
                StationFilter.All -> true
                StationFilter.Active -> station.status.equals("Active", ignoreCase = true)
                StationFilter.Inactive -> station.status.equals("Inactive", ignoreCase = true)
            }
            val q = search.trim().lowercase()
            val matchesSearch = q.isBlank() || listOf(
                station.name,
                station.stationCode,
                station.description.orEmpty(),
            ).any { it.lowercase().contains(q) }
            matchesFilter && matchesSearch
        }
    }

    val activeCount = stations.count { it.status.equals("Active", ignoreCase = true) }
    val inactiveCount = stations.size - activeCount
    val totalSlots = stations.sumOf { it.availableBatterySlots }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid50)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = "Battery stations",
                color = Grid900,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Open a hub to close or reopen batteries and keep capacity accurate.",
                color = Slate600,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (error != null) {
                StatusBanner(text = error.orEmpty(), isError = true, onDismiss = { error = null })
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatMini("Stations", stations.size.toString(), Modifier.weight(1f))
                StatMini("Active", activeCount.toString(), Modifier.weight(1f))
                StatMini("Slots", totalSlots.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("Search stations…", color = Slate600.copy(alpha = 0.7f))
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
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StationFilter.entries.forEach { option ->
                    val active = filter == option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) Grid700 else Color.White)
                            .border(1.dp, if (active) Grid700 else Grid100, RoundedCornerShape(12.dp))
                            .clickable { filter = option }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = option.label,
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
            ) {
                Text(
                    text = if (loading) "Loading…" else "${visible.size} shown · $inactiveCount inactive",
                    color = Slate600,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Refresh",
                    color = Grid700,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !loading) {
                            scope.launch { reloadToken += 1 }
                        }
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
                        text = "No stations found",
                        color = Grid900,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try another filter or clear search.",
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
                    items(visible, key = { it.id }) { station ->
                        StationCard(
                            station = station,
                            onClick = { onStationClick(station) },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatMini(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(text = label, color = Slate600, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Grid900,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun StationCard(
    station: StationResponseDto,
    onClick: () -> Unit,
) {
    val active = station.status.equals("Active", ignoreCase = true)
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = Grid900,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = station.stationCode,
                    color = Slate600,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = station.status.ifBlank { "Unknown" },
                color = if (active) Emerald900 else Slate700,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) Emerald50 else Color(0xFFF1F5F9))
                    .border(
                        1.dp,
                        if (active) Emerald200 else Slate300,
                        RoundedCornerShape(999.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaChip(
                label = "Battery slots",
                value = "${station.availableBatterySlots}",
                modifier = Modifier.weight(1f),
            )
            MetaChip(
                label = "Capacity",
                value = "${station.batteryCapacityKwh} kWh",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Manage batteries →",
            color = Grid700,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
