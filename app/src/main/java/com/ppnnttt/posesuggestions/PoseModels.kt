package com.ppnnttt.posesuggestions

import com.google.mlkit.vision.pose.Pose

data class PoseLandmarkData(
    val type: Int,
    val x: Float,
    val y: Float,
    val inFrameLikelihood: Float
)

data class DetectedPose(
    val landmarks: List<PoseLandmarkData>,
    val imageWidth: Int,
    val imageHeight: Int
)

fun Pose.toDetectedPose(width: Int, height: Int): DetectedPose {
    val landmarksData = allPoseLandmarks.map {
        PoseLandmarkData(
            type = it.landmarkType,
            x = it.position.x,
            y = it.position.y,
            inFrameLikelihood = it.inFrameLikelihood
        )
    }
    return DetectedPose(landmarksData, width, height)
}
