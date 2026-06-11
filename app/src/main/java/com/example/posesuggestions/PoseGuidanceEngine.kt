package com.example.posesuggestions

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

class PoseGuidanceEngine {

    data class Guidance(
        val message: String,
        val jointType: Int,
        val errorMagnitude: Float
    )

    fun getGuidance(userPose: DetectedPose, template: PoseTemplate): Guidance? {
        val userLandmarks = userPose.landmarks.associateBy { it.type }
        val templateLandmarks = template.landmarks.associateBy { it.type }

        val jointsToAnalyze = listOf(
            JointAngle(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, "left arm"),
            JointAngle(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, "right arm"),
            JointAngle(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE, "left leg"),
            JointAngle(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE, "right leg"),
            JointAngle(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, "left shoulder"),
            JointAngle(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, "right shoulder")
        )

        var maxError = 15f // Threshold to provide guidance
        var bestGuidance: Guidance? = null

        for (joint in jointsToAnalyze) {
            val userAngle = calculateAngle(
                userLandmarks[joint.p1],
                userLandmarks[joint.p2],
                userLandmarks[joint.p3]
            )
            val templateAngle = calculateAngleFromTemplate(
                templateLandmarks[joint.p1],
                templateLandmarks[joint.p2],
                templateLandmarks[joint.p3]
            )

            if (userAngle != null && templateAngle != null) {
                val diff = templateAngle - userAngle
                if (abs(diff) > maxError) {
                    maxError = abs(diff)
                    val action = if (diff > 0) "Bend" else "Straighten"
                    bestGuidance = Guidance(
                        message = "$action your ${joint.name}",
                        jointType = joint.p2,
                        errorMagnitude = abs(diff)
                    )
                }
            }
        }

        return bestGuidance
    }

    private data class JointAngle(val p1: Int, val p2: Int, val p3: Int, val name: String)

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
