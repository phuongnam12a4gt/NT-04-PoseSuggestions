package com.ppnnttt.posesuggestions

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

class PoseSimilarityEngine {

    /**
     * Calculates a similarity score between 0 and 100.
     * Updated to handle aspect ratio normalization.
     */
    fun calculateSimilarity(userPose: DetectedPose, template: PoseTemplate): Float {
        val userLandmarks = userPose.landmarks.associateBy { it.type }
        val templateLandmarks = template.landmarks.associateBy { it.type }

        // We use the user's image dimensions as the reference for both
        val targetWidth = userPose.imageWidth.toFloat()
        val targetHeight = userPose.imageHeight.toFloat()

        val anglesToCompare = listOf(
            Triple(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
            Triple(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
            Triple(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
            Triple(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE),
            Triple(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
            Triple(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
        )

        var totalScore = 0f
        var validAngles = 0

        for (triple in anglesToCompare) {
            val userAngle = calculateAngle(
                userLandmarks[triple.first],
                userLandmarks[triple.second],
                userLandmarks[triple.third]
            )
            
            // IMPORTANT: Multiply template (0..1) by target dimensions to get consistent pixel-space angles
            val templateAngle = calculateAngleFromTemplate(
                templateLandmarks[triple.first],
                templateLandmarks[triple.second],
                templateLandmarks[triple.third],
                targetWidth,
                targetHeight
            )

            if (userAngle != null && templateAngle != null) {
                val diff = abs(userAngle - templateAngle)
                val angleScore = (1f - (diff / 45f)).coerceIn(0f, 1f) // 45 degrees diff = 0%
                totalScore += angleScore
                validAngles++
            }
        }

        if (validAngles == 0) return 0f
        val rawScore = (totalScore / validAngles) * 100f
        return normalizeScore(rawScore)
    }

    private fun calculateAngle(first: PoseLandmarkData?, mid: PoseLandmarkData?, last: PoseLandmarkData?): Float? {
        if (first == null || mid == null || last == null || mid.inFrameLikelihood < 0.5f) return null
        return calculateAngleBetweenPoints(first.x, first.y, mid.x, mid.y, last.x, last.y)
    }

    private fun calculateAngleFromTemplate(
        first: LandmarkTemplate?, 
        mid: LandmarkTemplate?, 
        last: LandmarkTemplate?,
        width: Float,
        height: Float
    ): Float? {
        if (first == null || mid == null || last == null) return null
        // Map to pixel space before calculating angle
        return calculateAngleBetweenPoints(
            first.x * width, first.y * height,
            mid.x * width, mid.y * height,
            last.x * width, last.y * height
        )
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
