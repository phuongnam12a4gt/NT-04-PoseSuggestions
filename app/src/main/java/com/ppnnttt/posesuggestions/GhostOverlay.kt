package com.ppnnttt.posesuggestions

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
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
    
    val frameSize = 320.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale *= zoom
                    offset += pan
                }
            }
    ) {
        // Lớp phủ màu đen với lỗ thủng ở giữa
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            // 1. Vẽ nền đen mờ toàn màn hình
            drawRect(color = Color.Black.copy(alpha = 0.7f))

            // 2. Tính toán kích thước và vị trí lỗ thủng dựa trên offset và scale
            val w = frameSize.toPx() * scale
            val h = frameSize.toPx() * scale
            val x = (size.width - w) / 2 + offset.x
            val y = (size.height - h) / 2 + offset.y

            // 3. Đục lỗ thủng (Clear mode)
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(x, y),
                size = Size(w, h),
                cornerRadius = CornerRadius(24.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // 4. Hiển thị Ảnh thật của người mẫu (nếu có)
            if (template.isCustom) {
                val bitmap = BitmapFactory.decodeFile(template.previewImage)?.asImageBitmap()
                if (bitmap != null) {
                    withTransform({
                        translate(left = x, top = y)
                    }) {
                        drawImage(
                            image = bitmap,
                            dstSize = IntSize(w.toInt(), h.toInt()),
                            alpha = opacity * 0.8f
                        )
                    }
                }
            }

            // 5. Vẽ bộ xương mẫu đè lên ảnh
            val p = 20.dp.toPx()
            withTransform({
                translate(left = x + p, top = y + p)
                clipRect(left = -p, top = -p, right = w - p, bottom = h - p) // Cắt bỏ mọi thứ nằm ngoài viền khung
            }) {
                val innerW = w - 2 * p
                val innerH = h - 2 * p
                
                val innerMapper = CoordinateMapper(1, 1, innerW, innerH)
                val innerRenderer = PoseRenderer(innerMapper)
                
                with(innerRenderer) {
                    drawPose(
                        template.toDetectedPose(1, 1),
                        pointColor = Color.Cyan.copy(alpha = opacity),
                        lineBrush = Brush.linearGradient(listOf(Color.Cyan.copy(alpha = opacity), Color.Blue.copy(alpha = opacity)))
                    )
                }
            }

            // 5. Vẽ viền lỗ thủng
            drawRoundRect(
                color = Color.Cyan.copy(alpha = 0.5f),
                topLeft = Offset(x, y),
                size = Size(w, h),
                style = Stroke(width = 2.dp.toPx()),
                cornerRadius = CornerRadius(24.dp.toPx())
            )
        }

        // Hướng dẫn chữ hiện lên phía trên lỗ thủng
        Box(
            modifier = Modifier
                .offset {
                    val x = offset.x.roundToInt()
                    val y = offset.y.roundToInt() - (frameSize.toPx() * scale / 2).roundToInt() - 40
                    IntOffset(x, y)
                }
                .align(Alignment.Center)
        ) {
            Text(
                "ĐẶT CƠ THỂ VÀO ĐÂY",
                color = Color.Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(color = Color.Black, blurRadius = 8f)
                )
            )
        }
    }
}
