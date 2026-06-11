package com.example.posesuggestions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.pose.PoseLandmark

class CoordinateMapper(
    private val imageWidth: Int,
    private val imageHeight: Int,
    private val screenWidth: Float,
    private val screenHeight: Float,
) {
    fun mapX(x: Float): Float = x * (screenWidth / imageWidth)
    fun mapY(y: Float): Float = y * (screenHeight / imageHeight)
    fun mapOffset(x: Float, y: Float): Offset = Offset(mapX(x), mapY(y))
}

class PoseRenderer(
    private val mapper: CoordinateMapper,
) {
    fun DrawScope.drawPose(
        pose: DetectedPose,
        pointColor: Color = Color.Cyan,
        lineBrush: Brush = Brush.linearGradient(listOf(Color.White, Color.White))
    ) {
        val landmarks = pose.landmarks.associateBy { it.type }

        // Draw Skeleton Lines with Gradient
        drawSkeletonConnections(landmarks, lineBrush)

        // Draw Landmark Points with Glow Effect
        pose.landmarks.forEach { landmark ->
            if (landmark.inFrameLikelihood > 0.5f) {
                val center = mapper.mapOffset(landmark.x, landmark.y)
                
                // Outer Glow
                drawCircle(
                    color = pointColor.copy(alpha = 0.3f),
                    radius = 8.dp.toPx(),
                    center = center
                )
                
                // Main Point
                drawCircle(
                    color = pointColor,
                    radius = 4.dp.toPx(),
                    center = center
                )
                
                // Inner Shine
                drawCircle(
                    color = Color.White,
                    radius = 1.5.dp.toPx(),
                    center = center
                )
            }
        }
    }

    private fun DrawScope.drawSkeletonConnections(
        landmarks: Map<Int, PoseLandmarkData>,
        brush: Brush
    ) {
        val connections = listOf(
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
        )

        connections.forEach { (start, end) ->
            drawConnection(landmarks[start], landmarks[end], brush)
        }
    }

    private fun DrawScope.drawConnection(
        start: PoseLandmarkData?,
        end: PoseLandmarkData?,
        brush: Brush
    ) {
        if (start != null && end != null && (start.inFrameLikelihood > 0.5f) && (end.inFrameLikelihood > 0.5f)) {
            drawLine(
                brush = brush,
                start = mapper.mapOffset(start.x, start.y),
                end = mapper.mapOffset(end.x, end.y),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun SkeletonOverlay(
    modifier: Modifier = Modifier,
    detectedPose: DetectedPose? = null,
    detectedPosePartner: DetectedPose? = null,
    templatePose: PoseTemplate? = null,
    replayPose: DetectedPose? = null,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val activePose = detectedPose ?: replayPose ?: templatePose?.toDetectedPose(1, 1)
        val mapper = activePose?.let {
            CoordinateMapper(it.imageWidth, it.imageHeight, size.width, size.height)
        }

        mapper?.let { m ->
            val renderer = PoseRenderer(m)
            
            // Draw Templates (Ghost)
            if (replayPose == null) {
                templatePose?.let { template ->
                    with(renderer) {
                        // Person 1 Template
                        drawPose(
                            template.toDetectedPose(1, 1),
                            pointColor = Color.Magenta.copy(alpha = 0.2f),
                            lineBrush = Brush.linearGradient(
                                listOf(Color.Blue.copy(alpha = 0.1f), Color.Magenta.copy(alpha = 0.1f))
                            )
                        )
                        // Person 2 Template (if couple)
                        template.toPartnerDetectedPose(1, 1)?.let { partnerTemplate ->
                             drawPose(
                                partnerTemplate,
                                pointColor = Color.Yellow.copy(alpha = 0.2f),
                                lineBrush = Brush.linearGradient(
                                    listOf(Color.Green.copy(alpha = 0.1f), Color.Yellow.copy(alpha = 0.1f))
                                )
                            )
                        }
                    }
                }
            }

            // Draw Replay Pose
            replayPose?.let { pose ->
                with(renderer) {
                    drawPose(
                        pose,
                        pointColor = Color.Yellow,
                        lineBrush = Brush.linearGradient(listOf(Color.Yellow, Color.White))
                    )
                }
            }

            // Draw Detected Poses
            with(renderer) {
                detectedPose?.let { pose ->
                    drawPose(
                        pose,
                        pointColor = Color.Cyan,
                        lineBrush = Brush.linearGradient(listOf(Color.Cyan, Color(0xFFE91E63)))
                    )
                }
                detectedPosePartner?.let { pose ->
                    drawPose(
                        pose,
                        pointColor = Color.Green,
                        lineBrush = Brush.linearGradient(listOf(Color.Green, Color.Yellow))
                    )
                }
            }
        }
    }
}
