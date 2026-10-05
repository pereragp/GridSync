package com.gridsync.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GridSyncColorScheme = lightColorScheme(
    primary = Grid700,
    onPrimary = Color.White,
    primaryContainer = Grid100,
    onPrimaryContainer = Grid900,
    secondary = Grid600,
    onSecondary = Color.White,
    secondaryContainer = Grid50,
    onSecondaryContainer = Grid800,
    tertiary = Grid500,
    onTertiary = Color.White,
    background = Grid900,
    onBackground = Color.White,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Grid50,
    onSurfaceVariant = Slate700,
    outline = Slate300,
    error = ErrorRed800,
    onError = Color.White,
    errorContainer = ErrorRed50,
    onErrorContainer = ErrorRed800,
)

@Composable
fun GridSyncMobileTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GridSyncColorScheme,
        typography = Typography,
        content = content
    )
}
