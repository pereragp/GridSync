package com.gridsync.mobile.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.gridsync.mobile.ui.theme.Grid800
import com.gridsync.mobile.ui.theme.Grid900

const val AUTH_HERO_IMAGE_URL =
    "https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=2000&q=80"

@Composable
fun AuthHeroBackground(
    modifier: Modifier = Modifier,
) {
    val pan = rememberInfiniteTransition(label = "heroPan")
    val scale by pan.animateFloat(
        initialValue = 1.05f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroScale",
    )

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = AUTH_HERO_IMAGE_URL,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Grid900.copy(alpha = 0.92f),
                            Grid900.copy(alpha = 0.75f),
                            Grid800.copy(alpha = 0.45f),
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
                            Grid900.copy(alpha = 0.30f),
                            Color.Transparent,
                            Grid900.copy(alpha = 0.80f),
                        )
                    )
                )
        )
    }
}
