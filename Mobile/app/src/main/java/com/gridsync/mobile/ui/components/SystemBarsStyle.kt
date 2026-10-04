package com.gridsync.mobile.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Controls whether status/navigation bar icons are dark (for light backgrounds)
 * or light (for dark backgrounds).
 */
@Composable
fun SystemBarsStyle(
    lightBackground: Boolean,
) {
    val view = LocalView.current
    DisposableEffect(lightBackground) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = lightBackground
            controller.isAppearanceLightNavigationBars = lightBackground
        }
        onDispose { }
    }
}
