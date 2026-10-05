package com.gridsync.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.EvStation
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.Slate600

enum class OperatorTab(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(
        label = "Home",
        route = AppRoutes.OperatorDashboard,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    Bookings(
        label = "Bookings",
        route = AppRoutes.operatorBookings(),
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
    ),
    Stations(
        label = "Stations",
        route = AppRoutes.OperatorStations,
        selectedIcon = Icons.Filled.EvStation,
        unselectedIcon = Icons.Outlined.EvStation,
    ),
    Scan(
        label = "Scan",
        route = AppRoutes.OperatorScan,
        selectedIcon = Icons.Filled.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner,
    ),
    Profile(
        label = "Profile",
        route = AppRoutes.OperatorProfile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline,
    ),
}

fun operatorBottomBarVisibleFor(route: String?): Boolean {
    if (route == null) return false
    return route == AppRoutes.OperatorDashboard ||
        route == AppRoutes.OperatorBookings ||
        route.startsWith("operator_bookings?") ||
        route == AppRoutes.OperatorStations ||
        route == AppRoutes.OperatorScan ||
        route == AppRoutes.OperatorProfile
}

fun selectedOperatorTabFor(route: String?): OperatorTab? {
    if (route == null) return null
    return when {
        route == AppRoutes.OperatorDashboard -> OperatorTab.Home
        route == AppRoutes.OperatorBookings ||
            route.startsWith("operator_bookings?") ||
            route.startsWith("operator_bookings/") -> OperatorTab.Bookings
        route == AppRoutes.OperatorStations || route.startsWith("operator_stations/") -> OperatorTab.Stations
        route == AppRoutes.OperatorScan -> OperatorTab.Scan
        route == AppRoutes.OperatorProfile -> OperatorTab.Profile
        else -> null
    }
}

@Composable
fun OperatorBottomBar(
    currentRoute: String?,
    onTabSelected: (OperatorTab) -> Unit,
) {
    val selected = selectedOperatorTabFor(currentRoute)

    NavigationBar(
        containerColor = Color.White,
        contentColor = Grid900,
        tonalElevation = 0.dp,
    ) {
        OperatorTab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Grid700,
                    selectedTextColor = Grid700,
                    unselectedIconColor = Slate600,
                    unselectedTextColor = Slate600,
                    indicatorColor = Grid100,
                ),
            )
        }
    }
}
