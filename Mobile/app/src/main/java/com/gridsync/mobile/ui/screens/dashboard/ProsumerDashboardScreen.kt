package com.gridsync.mobile.ui.screens.dashboard

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.gridsync.mobile.GridSyncApp
import com.gridsync.mobile.ui.util.toast
import com.gridsync.mobile.data.location.LatLngPoint
import com.gridsync.mobile.data.location.UserLocationProvider
import com.gridsync.mobile.data.remote.dto.NearbyStationResponseDto
import com.gridsync.mobile.data.station.StationRepository
import com.gridsync.mobile.ui.components.AUTH_HERO_IMAGE_URL
import com.gridsync.mobile.ui.components.BrandLogo
import com.gridsync.mobile.ui.components.BrandLogoVariant
import com.gridsync.mobile.ui.data.MockStationRepository
import com.gridsync.mobile.ui.theme.ErrorRed200
import com.gridsync.mobile.ui.theme.ErrorRed50
import com.gridsync.mobile.ui.theme.ErrorRed800
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
import kotlinx.coroutines.launch

private sealed interface MapCameraFocus {
    data object User : MapCameraFocus
    data class Station(val id: String) : MapCameraFocus
    data object FitAll : MapCameraFocus
}

data class NearbyStationUi(
    val id: String,
    val name: String,
    val code: String,
    val distanceKm: Double,
    val availableSlots: Int,
    val status: String,
    val latitude: Double,
    val longitude: Double,
)

private val PreviewNearbyStations = MockStationRepository.stations.map { station ->
    NearbyStationUi(
        id = station.id,
        name = station.name,
        code = station.code,
        distanceKm = station.distanceKm,
        availableSlots = station.availableBatterySlots,
        status = station.status,
        latitude = station.latitude,
        longitude = station.longitude,
    )
}

private fun NearbyStationResponseDto.toUi() = NearbyStationUi(
    id = id,
    name = name,
    code = stationCode,
    distanceKm = distanceKm,
    availableSlots = availableBatterySlots,
    status = status,
    latitude = latitude,
    longitude = longitude,
)

@Composable
fun ProsumerDashboardScreen(
    userName: String = "Prosumer",
    onBookEnergy: () -> Unit = {},
    onMyBookings: () -> Unit = {},
    onStationClick: (NearbyStationUi) -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as GridSyncApp
    val scope = rememberCoroutineScope()

    var nearbyStations by remember { mutableStateOf<List<NearbyStationUi>>(emptyList()) }
    var userLocation by remember { mutableStateOf(UserLocationProvider.FALLBACK_COLOMBO) }
    var locationPermissionGranted by remember {
        mutableStateOf(app.userLocationProvider.hasLocationPermission())
    }
    var locationServicesEnabled by remember {
        mutableStateOf(app.userLocationProvider.isLocationEnabled())
    }
    var usingFallbackLocation by remember { mutableStateOf(false) }
    var isLoadingNearby by remember { mutableStateOf(true) }
    var isLocatingUser by remember { mutableStateOf(false) }
    var nearbyError by remember { mutableStateOf<String?>(null) }
    var selectedStationId by remember { mutableStateOf<String?>(null) }
    var cameraFocus by remember { mutableStateOf<MapCameraFocus>(MapCameraFocus.FitAll) }
    var reloadToken by remember { mutableStateOf(0) }
    var didRequestPermission by remember { mutableStateOf(false) }
    var pendingCount by remember { mutableStateOf(0) }
    var activeCount by remember { mutableStateOf(0) }
    var statsLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        statsLoading = true
        try {
            val stats = app.reservationRepository.getProsumerDashboardStats()
            pendingCount = stats.pendingReservations.toInt()
            activeCount = stats.activeReservations.toInt()
        } catch (_: Exception) {
            pendingCount = 0
            activeCount = 0
        } finally {
            statsLoading = false
        }
    }

    fun loadNearby() {
        scope.launch {
            isLoadingNearby = true
            nearbyError = null
            try {
                locationPermissionGranted = app.userLocationProvider.hasLocationPermission()
                locationServicesEnabled = app.userLocationProvider.isLocationEnabled()

                val resolved = app.userLocationProvider.resolve()
                usingFallbackLocation = resolved.isFallback
                userLocation = resolved.point

                val stations = app.stationRepository.getNearby(
                    latitude = resolved.point.latitude,
                    longitude = resolved.point.longitude,
                    radiusKm = StationRepository.DEFAULT_RADIUS_KM,
                ).map { it.toUi() }

                nearbyStations = stations
                if (selectedStationId == null || stations.none { it.id == selectedStationId }) {
                    selectedStationId = stations.firstOrNull()?.id
                }
                // Keep focus on user if they just tapped locate-me; otherwise fit markers.
                if (cameraFocus !is MapCameraFocus.User) {
                    cameraFocus = if (stations.isEmpty()) {
                        MapCameraFocus.User
                    } else {
                        MapCameraFocus.FitAll
                    }
                }
            } catch (e: Exception) {
                nearbyError = e.message ?: "Could not load nearby stations."
                app.toast(nearbyError.orEmpty(), long = true)
                nearbyStations = emptyList()
            } finally {
                isLoadingNearby = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        locationPermissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationServicesEnabled = app.userLocationProvider.isLocationEnabled()
        if (!locationPermissionGranted) {
            app.toast("Location permission denied. Showing stations near Colombo.", long = true)
        }
        reloadToken += 1
    }

    fun centerOnMyLocation() {
        scope.launch {
            locationPermissionGranted = app.userLocationProvider.hasLocationPermission()
            locationServicesEnabled = app.userLocationProvider.isLocationEnabled()

            if (!locationPermissionGranted) {
                didRequestPermission = true
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
                return@launch
            }

            if (!locationServicesEnabled) {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                return@launch
            }

            isLocatingUser = true
            try {
                val resolved = app.userLocationProvider.resolve(highAccuracy = true)
                userLocation = resolved.point
                usingFallbackLocation = resolved.isFallback
                selectedStationId = null
                cameraFocus = MapCameraFocus.User
                if (resolved.isFallback) {
                    app.toast("Couldn't get your GPS location. Showing the Colombo area.", long = true)
                }

                if (!resolved.isFallback) {
                    // Refresh nearby around the real GPS point.
                    reloadToken += 1
                }
            } finally {
                isLocatingUser = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!app.userLocationProvider.hasLocationPermission() && !didRequestPermission) {
            didRequestPermission = true
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    LaunchedEffect(reloadToken) {
        loadNearby()
    }

    val firstName = userName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "there" }
    val subtitle = when {
        isLoadingNearby -> "Finding stations near you…"
        nearbyError != null -> "Could not refresh nearby stations."
        !locationPermissionGranted -> "Allow location access to see hubs near you."
        !locationServicesEnabled -> "Turn on Location in system settings, then tap the locate button."
        usingFallbackLocation -> "GPS unavailable — showing Colombo area. Tap locate to retry."
        else -> "Active hubs within ${StationRepository.DEFAULT_RADIUS_KM.toInt()} km of you."
    }

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
                        value = if (statsLoading) "…" else pendingCount.toString(),
                        hint = "Awaiting review",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onMyBookings),
                    )
                    StatCard(
                        label = "Active",
                        value = if (statsLoading) "…" else activeCount.toString(),
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
                            text = subtitle,
                            color = if (nearbyError != null) ErrorRed800 else Slate600,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = if (isLoadingNearby) "…" else "${nearbyStations.size} nearby",
                        color = Grid700,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isLoadingNearby) { reloadToken += 1 }
                            .padding(4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                NearbyStationsMap(
                    stations = nearbyStations,
                    userLocation = userLocation,
                    selectedStationId = selectedStationId,
                    cameraFocus = cameraFocus,
                    locationPermissionGranted = locationPermissionGranted,
                    locationServicesEnabled = locationServicesEnabled,
                    isLoading = isLoadingNearby,
                    isLocatingUser = isLocatingUser,
                    error = nearbyError,
                    onSelectStation = { station ->
                        selectedStationId = station.id
                        cameraFocus = MapCameraFocus.Station(station.id)
                    },
                    onMyLocationClick = { centerOnMyLocation() },
                    onRetry = { reloadToken += 1 },
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        if (!isLoadingNearby && nearbyError == null && nearbyStations.isEmpty()) {
            item {
                Text(
                    text = "No active stations within ${StationRepository.DEFAULT_RADIUS_KM.toInt()} km. Try refreshing or widening your search area later.",
                    color = Slate600,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }

        items(nearbyStations, key = { it.id }) { station ->
            NearbyStationRow(
                station = station,
                selected = station.id == selectedStationId,
                onClick = {
                    selectedStationId = station.id
                    cameraFocus = MapCameraFocus.Station(station.id)
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
private fun NearbyStationsMap(
    stations: List<NearbyStationUi>,
    userLocation: LatLngPoint,
    selectedStationId: String?,
    cameraFocus: MapCameraFocus,
    locationPermissionGranted: Boolean,
    locationServicesEnabled: Boolean,
    isLoading: Boolean,
    isLocatingUser: Boolean,
    error: String?,
    onSelectStation: (NearbyStationUi) -> Unit,
    onMyLocationClick: () -> Unit,
    onRetry: () -> Unit,
) {
    val userLatLng = remember(userLocation) {
        LatLng(userLocation.latitude, userLocation.longitude)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLatLng, 12f)
    }
    // CameraUpdateFactory is only safe after the Maps SDK has loaded the map.
    var mapReady by remember { mutableStateOf(false) }
    val showMyLocationLayer = locationPermissionGranted && locationServicesEnabled

    LaunchedEffect(mapReady, userLocation, stations, cameraFocus) {
        if (!mapReady) return@LaunchedEffect

        try {
            when (cameraFocus) {
                is MapCameraFocus.User -> {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(userLatLng, 15f)
                    )
                }

                is MapCameraFocus.Station -> {
                    val selected = stations.firstOrNull { it.id == cameraFocus.id }
                    if (selected != null) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(selected.latitude, selected.longitude),
                                13.5f,
                            )
                        )
                    }
                }

                MapCameraFocus.FitAll -> {
                    if (stations.isEmpty()) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(userLatLng, 12f)
                        )
                    } else {
                        val bounds = LatLngBounds.builder().apply {
                            include(userLatLng)
                            stations.forEach { include(LatLng(it.latitude, it.longitude)) }
                        }.build()
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(bounds, 80)
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Avoid crashing Home if camera animation races map init.
            cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 12f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Grid100, RoundedCornerShape(16.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = showMyLocationLayer),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                // Custom locate button — Maps' built-in control is unreliable inside LazyColumn.
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = false,
            ),
            onMapLoaded = { mapReady = true },
        ) {
            stations.forEach { station ->
                key(station.id) {
                    val selected = station.id == selectedStationId
                    val markerState = rememberMarkerState(
                        position = LatLng(station.latitude, station.longitude),
                    )
                    Marker(
                        state = markerState,
                        title = station.name,
                        snippet = String.format(
                            "%.1f km · %d slots",
                            station.distanceKm,
                            station.availableSlots,
                        ),
                        zIndex = if (selected) 1f else 0f,
                        onClick = {
                            onSelectStation(station)
                            false
                        },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Grid100, CircleShape)
                .clickable(enabled = !isLocatingUser, onClick = onMyLocationClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isLocatingUser) {
                CircularProgressIndicator(
                    color = Grid700,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "My location",
                    tint = if (showMyLocationLayer) Grid700 else Slate600,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Grid700, modifier = Modifier.size(28.dp))
            }
        }

        if (error != null && !isLoading) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ErrorRed50)
                    .border(1.dp, ErrorRed200, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = error,
                    color = ErrorRed800,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to retry",
                    color = Grid700,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onRetry)
                        .padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun NearbyStationRow(
    station: NearbyStationUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val hasSlots = station.availableSlots > 0
    val isActive = station.status.equals("Active", ignoreCase = true)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (selected) 6.dp else 2.dp, shape, ambientColor = Grid700, spotColor = Grid700)
            .clip(shape)
            .background(if (selected) Grid50 else Color.White)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Grid600 else Grid100,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Distance badge
        Column(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Grid800, Grid500))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = String.format("%.1f", station.distanceKm),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = "km away",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StationChip(
                    text = if (hasSlots) "${station.availableSlots} slots free" else "No slots",
                    background = if (hasSlots) Grid100 else Color(0xFFFEF3C7),
                    content = if (hasSlots) Grid800 else Color(0xFF92400E),
                )
                StationChip(
                    text = station.status,
                    background = if (isActive) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                    content = if (isActive) Color(0xFF166534) else Slate700,
                    dot = true,
                )
            }
        }

        Text(
            text = "›",
            color = if (selected) Grid700 else Slate600,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun StationChip(
    text: String,
    background: Color,
    content: Color,
    dot: Boolean = false,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(content)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProsumerDashboardPreview() {
    GridSyncMobileTheme {
        // Preview uses empty nearby state; map requires device Google Play services.
        Column(Modifier.background(Grid50).padding(20.dp)) {
            Text("Nearby preview stations", color = Grid900)
            Spacer(Modifier.height(12.dp))
            PreviewNearbyStations.take(2).forEach { station ->
                NearbyStationRow(
                    station = station,
                    selected = station.id == "1",
                    onClick = {},
                    modifier = Modifier.padding(vertical = 5.dp),
                )
            }
        }
    }
}
