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
import androidx.compose.ui.res.stringResource
import kotlin.math.roundToInt

@Composable
fun GhostOverlay(
    template: PoseTemplate?,
    opacity: Float,
    modifier: Modifier = Modifier,
    isMirrored: Boolean = false
) {
    if (template == null) return

    var offset by remember(template.id) { mutableStateOf(Offset.Zero) }
    var scale by remember(template.id) { mutableFloatStateOf(1f) }
    
    val customImage = remember(template.id, template.previewImage) {
        if (template.isCustom) BitmapFactory.decodeFile(template.previewImage)?.asImageBitmap() else null
    }
    val displayTemplate = if (isMirrored) template.copy(landmarks = template.landmarks.map { it.copy(x = 1f - it.x) }) else template
    val frameSize = 320.dp
    val placeBodyText = stringResource(R.string.place_body_here)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.3f, 3f)
                    offset += pan
                }
            }
    ) {
        // Lớp phủ màu đen với lỗ thủng ở giữa
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            // 1. Vẽ nền đen mờ toàn màn hình
            drawRect(color = Color.Black.copy(alpha = 0.25f))

            // 2. Tính toán kích thước và vị trí lỗ thủng dựa trên offset và scale
            val aspect = template.sourceAspectRatio.coerceIn(.3f, 3f)
            val w = frameSize.toPx() * scale * minOf(1f, aspect)
            val h = frameSize.toPx() * scale / maxOf(1f, aspect)
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

            val p = 20.dp.toPx()
            // 4. Hiển thị Ảnh thật của người mẫu (nếu có)
            if (template.isCustom) {
                val bitmap = customImage
                if (bitmap != null) {
                    withTransform({
                        translate(left = if (isMirrored) x + w - p else x + p, top = y + p)
                        if (isMirrored) scale(-1f, 1f, pivot = Offset.Zero)
                    }) {
                        drawImage(
                            image = bitmap,
                            dstSize = IntSize((w - 2*p).toInt(), (h - 2*p).toInt()),
                            alpha = opacity * 0.8f
                        )
                    }
                }
            }

            // 5. Vẽ bộ xương mẫu đè lên ảnh
            withTransform({
                translate(left = x + p, top = y + p)
                clipRect(left = -p, top = -p, right = w - p, bottom = h - p) // Cắt bỏ mọi thứ nằm ngoài viền khung
            }) {
                val innerW = w - 2 * p
                val innerH = h - 2 * p
                
                drawHumanMannequin(
                    pose = displayTemplate.toDetectedPose(1, 1),
                    opacity = opacity,
                    targetSize = Size(innerW, innerH)
                )
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
                placeBodyText,
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
