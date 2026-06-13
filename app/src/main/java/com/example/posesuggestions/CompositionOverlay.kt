package com.example.posesuggestions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CompositionOverlay(
    modifier: Modifier = Modifier,
    showGrid: Boolean = true
) {
    if (!showGrid) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 0.5.dp.toPx()
        val color = Color.White.copy(alpha = 0.3f)

        // Vertical lines
        drawLine(
            color = color,
            start = Offset(size.width / 3, 0f),
            end = Offset(size.width / 3, size.height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = Offset(2 * size.width / 3, 0f),
            end = Offset(2 * size.width / 3, size.height),
            strokeWidth = strokeWidth
        )

        // Horizontal lines
        drawLine(
            color = color,
            start = Offset(0f, size.height / 3),
            end = Offset(size.width, size.height / 3),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = Offset(0f, 2 * size.height / 3),
            end = Offset(size.width, 2 * size.height / 3),
            strokeWidth = strokeWidth
        )
    }
}
