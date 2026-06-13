package com.example.posesuggestions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun GhostOverlay(
    template: PoseTemplate?,
    opacity: Float,
    modifier: Modifier = Modifier
) {
    if (template == null) return

    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale *= zoom
                    offset += pan
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                )
                .alpha(opacity)
                .size(320.dp)
                .border(1.dp, Color.Cyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Vẽ chính bộ xương mẫu từ JSON vào đây
            Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                val mapper = CoordinateMapper(
                    imageWidth = 1,
                    imageHeight = 1,
                    screenWidth = this.size.width,
                    screenHeight = this.size.height
                )
                val renderer = PoseRenderer(mapper)
                with(renderer) {
                    drawPose(
                        template.toDetectedPose(1, 1),
                        pointColor = Color.Cyan,
                        lineBrush = Brush.linearGradient(listOf(Color.Cyan, Color.Blue))
                    )
                }
            }

            // Hướng dẫn nhỏ cho User
            Text(
                "GHOST GUIDE",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                color = Color.Cyan.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Corner markers
            Box(Modifier.size(20.dp).align(Alignment.TopStart).border(2.dp, Color.Cyan, RoundedCornerShape(topStart = 4.dp)))
            Box(Modifier.size(20.dp).align(Alignment.BottomEnd).border(2.dp, Color.Cyan, RoundedCornerShape(bottomEnd = 4.dp)))
        }
    }
}
