package com.example.posesuggestions

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

class PoseSimilarityEngine {

    /**
     * Calculates a similarity score between 0 and 100.
     */
    fun calculateSimilarity(userPose: DetectedPose, template: PoseTemplate): Float {
        val userLandmarks = userPose.landmarks.associateBy { it.type }
        val templateLandmarks = template.landmarks.associateBy { it.type }

        val anglesToCompare = listOf(
            Triple(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
            Triple(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
            Triple(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
            Triple(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE),
            Triple(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
            Triple(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP),
            Triple(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE),
            Triple(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        )

        var totalScore = 0f
        var validAngles = 0

        for (triple in anglesToCompare) {
            val userAngle = calculateAngle(
                userLandmarks[triple.first],
                userLandmarks[triple.second],
                userLandmarks[triple.third]
            )
            val templateAngle = calculateAngleFromTemplate(
                templateLandmarks[triple.first],
                templateLandmarks[triple.second],
                templateLandmarks[triple.third]
            )

            if (userAngle != null && templateAngle != null) {
                val diff = abs(userAngle - templateAngle)
                // Normalize difference: 0 degrees diff = 100%, 180 degrees diff = 0%
                // But practically, 45 degrees is already quite a big difference.
                val angleScore = (1f - (diff / 180f)).coerceIn(0f, 1f)
                totalScore += angleScore
                validAngles++
            }
        }

        if (validAngles == 0) return 0f

        val rawScore = (totalScore / validAngles) * 100f
        return normalizeScore(rawScore)
    }

    private fun calculateAngle(first: PoseLandmarkData?, mid: PoseLandmarkData?, last: PoseLandmarkData?): Float? {
        if (first == null || mid == null || last == null) return null
        if (first.inFrameLikelihood < 0.5f || mid.inFrameLikelihood < 0.5f || last.inFrameLikelihood < 0.5f) return null
        
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
        if (result > 180) {
            result = 360 - result
        }
        return result
    }

    /**
     * Map the raw average similarity to a more user-friendly 0-100 score.
     * Usually users find it hard to get 100%, so we can scale it.
     */
    private fun normalizeScore(rawScore: Float): Float {
        // Example: If raw score is above 95, treat as 100. If below 60, it's quite bad.
        return when {
            rawScore > 95f -> 100f
            rawScore < 60f -> (rawScore / 60f) * 50f // Scale lower scores more aggressively
            else -> 50f + (rawScore - 60f) * (50f / 35f)
        }.coerceIn(0f, 100f)
    }
}
