package com.gridsync.mobile.ui.components

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.gridsync.mobile.ui.theme.Grid100
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QrCameraPreview(
    enabled: Boolean,
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val handled = remember { AtomicBoolean(false) }
    val callbackRef = remember { object { var value: (String) -> Unit = onQrDetected } }
    callbackRef.value = onQrDetected

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    DisposableEffect(enabled, lifecycleOwner, previewView) {
        val view = previewView
        if (view == null) {
            return@DisposableEffect onDispose { }
        }

        handled.set(false)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = BarcodeScanning.getClient(options)

        val bindRunnable = Runnable {
            val provider = cameraProviderFuture.get()
            cameraProvider = provider
            provider.unbindAll()
            if (!enabled) return@Runnable

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = view.surfaceProvider
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            ),
                        )
                        .build(),
                )
                .build()

            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                if (!enabled || handled.get()) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val input = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees,
                )

                scanner.process(input)
                    .addOnSuccessListener { barcodes ->
                        if (!enabled || handled.get()) return@addOnSuccessListener
                        val payload = barcodes
                            .asSequence()
                            .mapNotNull { it.rawValue?.trim() }
                            .firstOrNull { it.isNotBlank() }
                        if (payload != null && handled.compareAndSet(false, true)) {
                            mainExecutor.execute {
                                callbackRef.value(payload)
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }

            runCatching {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }
        }

        // Wait until PreviewView is attached so analysis frames actually flow.
        val attachAndBind = {
            if (view.isAttachedToWindow) {
                cameraProviderFuture.addListener(bindRunnable, mainExecutor)
            } else {
                view.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: android.view.View) {
                        view.removeOnAttachStateChangeListener(this)
                        cameraProviderFuture.addListener(bindRunnable, mainExecutor)
                    }

                    override fun onViewDetachedFromWindow(v: android.view.View) = Unit
                })
            }
        }
        attachAndBind()

        onDispose {
            runCatching { cameraProvider?.unbindAll() }
            runCatching {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            }
            scanner.close()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Grid100, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (previewView !== view) {
                    previewView = view
                }
            },
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .border(2.dp, Grid100.copy(alpha = 0.9f), RoundedCornerShape(18.dp)),
        )
    }
}
