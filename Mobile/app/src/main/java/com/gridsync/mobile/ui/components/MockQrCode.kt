package com.gridsync.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.Slate600
import kotlin.math.abs

/**
 * Visual QR placeholder for UI demos. Replace with a real encoder (e.g. ZXing)
 * when wiring the approved reservation QR payload from the API.
 */
@Composable
fun MockQrCode(
    payload: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
) {
    val modules = rememberQrModules(payload, dimension = 21)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Transaction QR",
            color = Grid900,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Show this at the hub for verification",
            color = Slate600,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(Grid50)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(size - 20.dp)) {
                val cell = this.size.minDimension / modules.size
                modules.forEachIndexed { row, cols ->
                    cols.forEachIndexed { col, filled ->
                        if (filled) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(col * cell, row * cell),
                                size = Size(cell, cell),
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = payload,
            color = Slate600,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun rememberQrModules(payload: String, dimension: Int): List<BooleanArray> {
    return androidx.compose.runtime.remember(payload, dimension) {
        buildPseudoQr(payload, dimension)
    }
}

private fun buildPseudoQr(payload: String, dimension: Int): List<BooleanArray> {
    val seed = payload.fold(17) { acc, c -> acc * 31 + c.code }
    val grid = Array(dimension) { BooleanArray(dimension) }

    fun setFinder(ox: Int, oy: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val onBorder = r == 0 || c == 0 || r == 6 || c == 6
                val inCenter = r in 2..4 && c in 2..4
                grid[oy + r][ox + c] = onBorder || inCenter
            }
        }
    }

    setFinder(0, 0)
    setFinder(dimension - 7, 0)
    setFinder(0, dimension - 7)

    var state = abs(seed)
    for (r in 0 until dimension) {
        for (c in 0 until dimension) {
            if (grid[r][c]) continue
            // Skip finder quiet zones roughly
            val inFinder =
                (r < 8 && c < 8) ||
                    (r < 8 && c >= dimension - 8) ||
                    (r >= dimension - 8 && c < 8)
            if (inFinder) continue
            state = (state * 1103515245 + 12345) and 0x7fffffff
            grid[r][c] = state % 3 != 0
        }
    }
    return grid.toList()
}
