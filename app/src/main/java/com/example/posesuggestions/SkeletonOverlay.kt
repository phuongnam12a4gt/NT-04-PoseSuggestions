package com.example.posesuggestions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.pose.PoseLandmark

/**
 * Maps image coordinates to screen coordinates based on the Canvas size.
 */
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

/**
 * Handles the drawing logic for the pose skeleton and landmarks.
 */
class PoseRenderer(
    private val mapper: CoordinateMapper,
) {
    fun DrawScope.drawPose(pose: DetectedPose) {
        val landmarks = pose.landmarks.associateBy { it.type }

        // Draw Skeleton Lines
        drawSkeletonConnections(landmarks)

        // Draw Landmark Points
        pose.landmarks.forEach { landmark ->
            if (landmark.inFrameLikelihood > 0.5f) {
                drawCircle(
                    color = Color.Cyan,
                    radius = 5.dp.toPx(),
                    center = mapper.mapOffset(landmark.x, landmark.y)
                )
            }
        }
    }

    private fun DrawScope.drawSkeletonConnections(landmarks: Map<Int, PoseLandmarkData>) {
        val connections = listOf(
            // Torso
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            
            // Left Arm
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            
            // Right Arm
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            
            // Left Leg
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            
            // Right Leg
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
        )

        connections.forEach { (start, end) ->
            drawConnection(landmarks[start], landmarks[end])
        }
    }

    private fun DrawScope.drawConnection(start: PoseLandmarkData?, end: PoseLandmarkData?) {
        if (start != null && end != null && (start.inFrameLikelihood > 0.5f) && (end.inFrameLikelihood > 0.5f)) {
            drawLine(
                color = Color.White,
                start = mapper.mapOffset(start.x, start.y),
                end = mapper.mapOffset(end.x, end.y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun SkeletonOverlay(
    detectedPose: DetectedPose?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        detectedPose?.let { pose ->
            val mapper = CoordinateMapper(
                imageWidth = pose.imageWidth,
                imageHeight = pose.imageHeight,
                screenWidth = size.width,
                screenHeight = size.height
            )
            val renderer = PoseRenderer(mapper)
            with(renderer) {
                drawPose(pose)
            }
        }
    }
}
