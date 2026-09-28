package com.gridsync.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gridsync.mobile.R

enum class BrandLogoVariant(val height: Dp) {
    Header(64.dp),
    Hero(120.dp),
    Auth(80.dp),
}

@Composable
fun BrandLogo(
    variant: BrandLogoVariant = BrandLogoVariant.Header,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.gridsync_logo),
        contentDescription = "GridSync",
        modifier = modifier.height(variant.height),
        contentScale = ContentScale.Fit,
    )
}
