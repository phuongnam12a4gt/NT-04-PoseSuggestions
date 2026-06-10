package com.example.posesuggestions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.pose.PoseLandmark

@Composable
fun PoseOverlay(
    detectedPose: DetectedPose?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        detectedPose?.let { pose ->
            val scaleX = size.width / pose.imageWidth
            val scaleY = size.height / pose.imageHeight

            // Draw connections (skeleton)
            drawSkeleton(pose, scaleX, scaleY)

            // Draw landmarks
            pose.landmarks.forEach { landmark ->
                if (landmark.inFrameLikelihood > 0.5f) {
                    drawCircle(
                        color = Color.Cyan,
                        radius = 4.dp.toPx(),
                        center = Offset(landmark.x * scaleX, landmark.y * scaleY)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSkeleton(
    pose: DetectedPose,
    scaleX: Float,
    scaleY: Float
) {
    val landmarks = pose.landmarks.associateBy { it.type }

    fun drawLineBetween(startType: Int, endType: Int) {
        val start = landmarks[startType]
        val end = landmarks[endType]
        if (start != null && end != null && start.inFrameLikelihood > 0.5f && end.inFrameLikelihood > 0.5f) {
            drawLine(
                color = Color.White,
                start = Offset(start.x * scaleX, start.y * scaleY),
                end = Offset(end.x * scaleX, end.y * scaleY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }

    // Arms
    drawLineBetween(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
    drawLineBetween(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
    drawLineBetween(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
    drawLineBetween(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)

    // Legs
    drawLineBetween(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
    drawLineBetween(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
    drawLineBetween(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
    drawLineBetween(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)

    // Torso
    drawLineBetween(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
    drawLineBetween(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
    drawLineBetween(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
    drawLineBetween(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
}
