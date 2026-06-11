package com.example.posesuggestions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.pose.PoseLandmark

@Composable
fun PoseHeatmapOverlay(
    detectedPose: DetectedPose?,
    template: PoseTemplate?,
    errorAnalysisEngine: ErrorAnalysisEngine,
    modifier: Modifier = Modifier
) {
    if (detectedPose == null || template == null) return

    val errors = errorAnalysisEngine.analyzeErrors(detectedPose, template)
    val errorMap = errors.associate { (it.startJoint to it.endJoint) to it.level }

    Canvas(modifier = modifier.fillMaxSize()) {
        val mapper = CoordinateMapper(
            imageWidth = detectedPose.imageWidth,
            imageHeight = detectedPose.imageHeight,
            screenWidth = size.width,
            screenHeight = size.height
        )

        val landmarks = detectedPose.landmarks.associateBy { it.type }

        fun drawConnectionWithHeat(startType: Int, endType: Int) {
            val start = landmarks[startType]
            val end = landmarks[endType]
            
            if (start != null && end != null && start.inFrameLikelihood > 0.5f && end.inFrameLikelihood > 0.5f) {
                // Try both directions for the key in errorMap
                val level = errorMap[startType to endType] ?: errorMap[endType to startType] ?: ErrorLevel.CORRECT
                
                val color = when (level) {
                    ErrorLevel.CORRECT -> Color.Green.copy(alpha = 0.4f)
                    ErrorLevel.SLIGHTLY_INCORRECT -> Color.Yellow.copy(alpha = 0.6f)
                    ErrorLevel.INCORRECT -> Color.Red.copy(alpha = 0.7f)
                }

                // Draw a thicker "glow" line for errors
                if (level != ErrorLevel.CORRECT) {
                    drawLine(
                        color = color,
                        start = mapper.mapOffset(start.x, start.y),
                        end = mapper.mapOffset(end.x, end.y),
                        strokeWidth = 12.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Draw critical connections that we analyze for errors
        val criticalConnections = listOf(
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
        )

        criticalConnections.forEach { (start, end) ->
            drawConnectionWithHeat(start, end)
        }
    }
}
