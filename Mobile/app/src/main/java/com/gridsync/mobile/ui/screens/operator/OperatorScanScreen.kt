package com.gridsync.mobile.ui.screens.operator

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.core.content.ContextCompat
import com.gridsync.mobile.GridSyncApp
import com.gridsync.mobile.data.remote.dto.ReservationQrVerificationDto
import com.gridsync.mobile.ui.components.QrCameraPreview
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.Slate600
import com.gridsync.mobile.ui.util.formatApiDateTime
import com.gridsync.mobile.ui.util.reservationTypeLabel
import kotlinx.coroutines.launch

@Composable
fun OperatorScanScreen(
    onTransferCompleted: (successMessage: String) -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as GridSyncApp
    val scope = rememberCoroutineScope()

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    var scanEnabled by remember { mutableStateOf(true) }
    var verified by remember { mutableStateOf<ReservationQrVerificationDto?>(null) }
    var verifying by remember { mutableStateOf(false) }
    var completing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraGranted = granted
        permissionRequested = true
        if (!granted) {
            error = "Camera permission is required to scan booking QR codes."
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun verifyPayload(payload: String) {
        if (verifying || completing) return
        scope.launch {
            verifying = true
            scanEnabled = false
            error = null
            message = null
            verified = null
            try {
                verified = app.reservationRepository.verifyQr(payload)
            } catch (e: Exception) {
                error = e.message ?: "QR verification failed"
                scanEnabled = true
            } finally {
                verifying = false
            }
        }
    }

    fun resetScanner() {
        verified = null
        error = null
        message = null
        scanEnabled = true
    }

    // Keep the live camera outside verticalScroll — scrolling breaks CameraX analysis.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid50)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text = "Scan QR",
            color = Grid900,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Point the camera at the prosumer QR to verify, then complete the transfer.",
            color = Slate600,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (error != null) {
            StatusBanner(text = error.orEmpty(), isError = true, onDismiss = { error = null })
            Spacer(modifier = Modifier.height(10.dp))
        }
        if (message != null) {
            StatusBanner(text = message.orEmpty(), isError = false, onDismiss = { message = null })
            Spacer(modifier = Modifier.height(10.dp))
        }

        when {
            !cameraGranted -> {
                CameraPermissionCard(
                    denied = permissionRequested,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                )
            }

            verified == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Grid100, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        QrCameraPreview(
                            enabled = scanEnabled && !verifying,
                            onQrDetected = ::verifyPayload,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (verifying) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Verifying booking…",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (scanEnabled) {
                            "Camera is live. Align the QR inside the frame."
                        } else {
                            "Scanner paused."
                        },
                        color = Slate600,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        verified?.let { result ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Grid100, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Verified booking",
                    color = Grid900,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(6.dp))
                MetaChip(label = "Code", value = result.reservationCode.ifBlank { result.reservationId })
                MetaChip(label = "Station", value = result.stationName ?: "Unavailable")
                MetaChip(label = "Prosumer NIC", value = result.prosumerNic.ifBlank { "—" })
                MetaChip(
                    label = "Transfer",
                    value = "${reservationTypeLabel(result.reservationType)} · ${result.energyKwh} kWh",
                )
                MetaChip(
                    label = "Slot",
                    value = "${formatApiDateTime(result.slotStart)} → ${formatApiDateTime(result.slotEnd)}",
                )
                MetaChip(label = "Status", value = result.status)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        scope.launch {
                            completing = true
                            error = null
                            message = null
                            try {
                                val completed = app.reservationRepository.complete(result.reservationId)
                                val code = completed.reservationCode.ifBlank { completed.reservationId }
                                onTransferCompleted(
                                    "Transfer completed for $code. Battery inventory updated.",
                                )
                            } catch (e: Exception) {
                                error = e.message ?: "Completion failed"
                            } finally {
                                completing = false
                            }
                        }
                    },
                    enabled = !completing && !verifying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Grid700,
                        contentColor = Color.White,
                    ),
                ) {
                    if (completing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Complete transfer", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = ::resetScanner,
                    enabled = !completing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Scan another QR", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionCard(
    denied: Boolean,
    onRequest: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (denied) "Camera access blocked" else "Camera access needed",
            color = Grid900,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (denied) {
                "Enable camera permission in system settings, then return here to scan booking QR codes."
            } else {
                "Allow camera access so GridSync can scan the prosumer booking QR."
            },
            color = Slate600,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onRequest,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Grid700,
                contentColor = Color.White,
            ),
        ) {
            Text("Allow camera", style = MaterialTheme.typography.labelLarge)
        }
    }
}
