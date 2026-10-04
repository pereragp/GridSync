package com.gridsync.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
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

enum class ProsumerTab(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(
        label = "Home",
        route = AppRoutes.ProsumerDashboard,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    Bookings(
        label = "Bookings",
        route = AppRoutes.MyBookings,
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
    ),
    Book(
        label = "Book",
        route = AppRoutes.createReservation(),
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline,
    ),
    Profile(
        label = "Profile",
        route = AppRoutes.Profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline,
    ),
}

/** Bottom bar on main tabs only — hidden on auth, wizard, and detail screens. */
fun bottomBarVisibleFor(route: String?): Boolean {
    if (route == null) return false
    return route == AppRoutes.ProsumerDashboard ||
        route == AppRoutes.MyBookings ||
        route == AppRoutes.Profile
}

fun selectedTabFor(route: String?): ProsumerTab? {
    if (route == null) return null
    return when {
        route == AppRoutes.ProsumerDashboard -> ProsumerTab.Home
        route == AppRoutes.MyBookings || route.startsWith("bookings/") -> ProsumerTab.Bookings
        route.startsWith("book/") -> ProsumerTab.Book
        route == AppRoutes.Profile -> ProsumerTab.Profile
        route.startsWith("station/") -> ProsumerTab.Home
        else -> null
    }
}

@Composable
fun ProsumerBottomBar(
    currentRoute: String?,
    onTabSelected: (ProsumerTab) -> Unit,
) {
    val selected = selectedTabFor(currentRoute)

    NavigationBar(
        containerColor = Color.White,
        contentColor = Grid900,
        tonalElevation = 0.dp,
    ) {
        ProsumerTab.entries.forEach { tab ->
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
                            fontSize = 11.sp,
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
