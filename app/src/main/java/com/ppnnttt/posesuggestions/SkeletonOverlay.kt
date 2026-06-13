package com.ppnnttt.posesuggestions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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

        // 1. Vẽ các khối cơ thể (Torso, Pelvis) để rõ dáng người hơn
        drawBodyBlocks(landmarks, pointColor.copy(alpha = 0.2f))

        // 2. Vẽ Skeleton Lines với đường nét dày hơn, bo tròn
        drawSkeletonConnections(landmarks, lineBrush)

        // 3. Vẽ các khớp xương (Landmarks)
        pose.landmarks.forEach { landmark ->
            if (landmark.inFrameLikelihood > 0.5f) {
                val center = mapper.mapOffset(landmark.x, landmark.y)
                drawCircle(color = pointColor, radius = 4.dp.toPx(), center = center)
                drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = center)
            }
        }
    }

    private fun DrawScope.drawBodyBlocks(landmarks: Map<Int, PoseLandmarkData>, color: Color) {
        val lShoulder = landmarks[PoseLandmark.LEFT_SHOULDER]
        val rShoulder = landmarks[PoseLandmark.RIGHT_SHOULDER]
        val lHip = landmarks[PoseLandmark.LEFT_HIP]
        val rHip = landmarks[PoseLandmark.RIGHT_HIP]

        if (lShoulder != null && rShoulder != null && lHip != null && rHip != null) {
            val path = Path().apply {
                moveTo(mapper.mapX(lShoulder.x), mapper.mapY(lShoulder.y))
                lineTo(mapper.mapX(rShoulder.x), mapper.mapY(rShoulder.y))
                lineTo(mapper.mapX(rHip.x), mapper.mapY(rHip.y))
                lineTo(mapper.mapX(lHip.x), mapper.mapY(lHip.y))
                close()
            }
            drawPath(path = path, color = color)
        }
    }

    private fun DrawScope.drawSkeletonConnections(
        landmarks: Map<Int, PoseLandmarkData>,
        brush: Brush
    ) {
        val connections = listOf(
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
            PoseLandmark.LEFT_SHOULDER to rShoulderId(),
            PoseLandmark.LEFT_HIP to rHipId(),
            PoseLandmark.LEFT_SHOULDER to lHipId(),
            PoseLandmark.RIGHT_SHOULDER to rHipId()
        )

        connections.forEach { (start, end) ->
            val s = landmarks[start]
            val e = landmarks[end]
            if (s != null && e != null && s.inFrameLikelihood > 0.5f && e.inFrameLikelihood > 0.5f) {
                drawLine(
                    brush = brush,
                    start = mapper.mapOffset(s.x, s.y),
                    end = mapper.mapOffset(e.x, e.y),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }

    private fun lShoulderId() = PoseLandmark.LEFT_SHOULDER
    private fun rShoulderId() = PoseLandmark.RIGHT_SHOULDER
    private fun lHipId() = PoseLandmark.LEFT_HIP
    private fun rHipId() = PoseLandmark.RIGHT_HIP
}

@Composable
fun SkeletonOverlay(
    modifier: Modifier = Modifier,
    detectedPose: DetectedPose? = null,
    detectedPosePartner: DetectedPose? = null,
    templatePose: PoseTemplate? = null,
    replayPose: DetectedPose? = null,
    currentScore: Float = 0f
) {
    // Smart HUD: Fade out guide when pose is nearly perfect
    val guideAlpha by animateFloatAsState(
        targetValue = if (currentScore > 90f) 0.1f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "GuideAlpha"
    )

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
                            pointColor = Color.Magenta.copy(alpha = 0.2f * guideAlpha),
                            lineBrush = Brush.linearGradient(
                                listOf(
                                    Color.Blue.copy(alpha = 0.1f * guideAlpha),
                                    Color.Magenta.copy(alpha = 0.1f * guideAlpha)
                                )
                            )
                        )
                        // Person 2 Template (if couple)
                        template.toPartnerDetectedPose(1, 1)?.let { partnerTemplate ->
                             drawPose(
                                partnerTemplate,
                                pointColor = Color.Yellow.copy(alpha = 0.2f * guideAlpha),
                                lineBrush = Brush.linearGradient(
                                    listOf(
                                        Color.Green.copy(alpha = 0.1f * guideAlpha),
                                        Color.Yellow.copy(alpha = 0.1f * guideAlpha)
                                    )
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
                        pointColor = Color.Yellow.copy(alpha = guideAlpha),
                        lineBrush = Brush.linearGradient(
                            listOf(
                                Color.Yellow.copy(alpha = guideAlpha),
                                Color.White.copy(alpha = guideAlpha)
                            )
                        )
                    )
                }
            }

            // Draw Detected Poses
            with(renderer) {
                detectedPose?.let { pose ->
                    drawPose(
                        pose,
                        pointColor = Color.Cyan.copy(alpha = guideAlpha),
                        lineBrush = Brush.linearGradient(
                            listOf(
                                Color.Cyan.copy(alpha = guideAlpha),
                                Color(0xFFE91E63).copy(alpha = guideAlpha)
                            )
                        )
                    )
                }
                detectedPosePartner?.let { pose ->
                    drawPose(
                        pose,
                        pointColor = Color.Green.copy(alpha = guideAlpha),
                        lineBrush = Brush.linearGradient(
                            listOf(
                                Color.Green.copy(alpha = guideAlpha),
                                Color.Yellow.copy(alpha = guideAlpha)
                            )
                        )
                    )
                }
            }
        }
    }
}
