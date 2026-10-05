package com.gridsync.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.gridsync.mobile.ui.navigation.GridSyncNavHost
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.GridSyncMobileTheme

class MainActivity : ComponentActivity() {
    private val localNetworkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocalNetworkAccessIfNeeded()
        // Start with light system icons (login is dark green)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
        )
        setContent {
            GridSyncMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Grid900,
                ) {
                    GridSyncNavHost()
                }
            }
        }
    }

    /**
     * Android 17 (API 37)+ blocks app traffic to local addresses (including the
     * emulator host alias 10.0.2.2) unless ACCESS_LOCAL_NETWORK is granted.
     * Without it, OkHttp hangs until timeout with no useful error.
     */
    private fun requestLocalNetworkAccessIfNeeded() {
        if (Build.VERSION.SDK_INT < 36) return
        val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
        val granted = ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            localNetworkPermissionLauncher.launch(permission)
        }
    }
}
