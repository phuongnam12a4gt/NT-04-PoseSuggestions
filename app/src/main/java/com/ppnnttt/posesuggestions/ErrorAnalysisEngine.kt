package com.ppnnttt.posesuggestions

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

enum class ErrorLevel {
    CORRECT, // Green
    SLIGHTLY_INCORRECT, // Yellow
    INCORRECT // Red
}

class ErrorAnalysisEngine {

    data class ConnectionError(
        val startJoint: Int,
        val endJoint: Int,
        val level: ErrorLevel
    )

    fun analyzeErrors(userPose: DetectedPose, template: PoseTemplate): List<ConnectionError> {
        val userLandmarks = userPose.landmarks.associateBy { it.type }
        val templateLandmarks = template.landmarks.associateBy { it.type }

        val anglesToCompare = listOf(
            Triple(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST) to (PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW),
            Triple(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST) to (PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW),
            Triple(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE) to (PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE),
            Triple(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE) to (PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE),
            Triple(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP) to (PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_SHOULDER),
            Triple(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP) to (PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_SHOULDER)
        )

        return anglesToCompare.mapNotNull { (angleTriple, connection) ->
            val userAngle = calculateAngle(
                userLandmarks[angleTriple.first],
                userLandmarks[angleTriple.second],
                userLandmarks[angleTriple.third]
            )
            val templateAngle = calculateAngleFromTemplate(
                templateLandmarks[angleTriple.first],
                templateLandmarks[angleTriple.second],
                templateLandmarks[angleTriple.third]
            )

            if (userAngle != null && templateAngle != null) {
                val diff = abs(userAngle - templateAngle)
                val level = when {
                    diff < 15f -> ErrorLevel.CORRECT
                    diff < 35f -> ErrorLevel.SLIGHTLY_INCORRECT
                    else -> ErrorLevel.INCORRECT
                }
                ConnectionError(connection.first, connection.second, level)
            } else null
        }
    }

    private fun calculateAngle(first: PoseLandmarkData?, mid: PoseLandmarkData?, last: PoseLandmarkData?): Float? {
        if (first == null || mid == null || last == null || mid.inFrameLikelihood < 0.5f) return null
        return calculateAngleBetweenPoints(first.x, first.y, mid.x, mid.y, last.x, last.y)
    }

    private fun calculateAngleFromTemplate(first: LandmarkTemplate?, mid: LandmarkTemplate?, last: LandmarkTemplate?): Float? {
        if (first == null || mid == null || last == null) return null
        return calculateAngleBetweenPoints(first.x, first.y, mid.x, mid.y, last.x, last.y)
    }

    private fun calculateAngleBetweenPoints(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
        val angle = Math.toDegrees(
            (atan2(y3 - y2, x3 - x2) - atan2(y1 - y2, x1 - x2)).toDouble()
        ).toFloat()
        var result = abs(angle)
        if (result > 180) result = 360 - result
        return result
    }
}
