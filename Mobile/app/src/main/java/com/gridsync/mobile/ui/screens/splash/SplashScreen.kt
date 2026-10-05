package com.gridsync.mobile.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridsync.mobile.ui.components.BrandLogo
import com.gridsync.mobile.ui.components.BrandLogoVariant
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid800
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.OutfitFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Animation state
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.82f) }
    val taglineAlpha = remember { Animatable(0f) }
    val taglineOffset = remember { Animatable(16f) }

    LaunchedEffect(Unit) {
        // Logo fades + scales in
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(600, easing = EaseOutCubic))
        }
        launch {
            logoScale.animateTo(1f, animationSpec = tween(600, easing = EaseOutCubic))
        }
        // Tagline appears shortly after
        delay(400)
        launch {
            taglineAlpha.animateTo(1f, animationSpec = tween(500, easing = EaseOut))
        }
        launch {
            taglineOffset.animateTo(0f, animationSpec = tween(500, easing = EaseOut))
        }
        // Hold, then navigate
        delay(1400)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Grid900,
                        Grid800,
                        Grid700,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            BrandLogo(
                variant = BrandLogoVariant.Hero,
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Smart energy, on your terms.",
                color = Grid100.copy(alpha = taglineAlpha.value),
                fontSize = 16.sp,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .alpha(taglineAlpha.value)
                    .padding(top = taglineOffset.value.dp),
            )
        }

        // Bottom wordmark
        Text(
            text = "GridSync",
            color = Color.White.copy(alpha = taglineAlpha.value * 0.25f),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = OutfitFontFamily,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
        )
    }
}
