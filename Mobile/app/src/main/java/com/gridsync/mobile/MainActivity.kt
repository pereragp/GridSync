package com.gridsync.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.gridsync.mobile.ui.screens.login.LoginScreen
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.GridSyncMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GridSyncMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Grid900,
                ) {
                    LoginScreen(
                        onForgotPassword = {
                            // Wire navigation later
                        },
                        onRegister = {
                            // Wire navigation later
                        },
                        onLoginSuccess = {
                            // Wire auth + navigation later
                        },
                    )
                }
            }
        }
    }
}
