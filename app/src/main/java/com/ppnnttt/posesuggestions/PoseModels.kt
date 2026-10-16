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

/** Avoid accepting faces, empty photos or low-confidence inferred bodies as templates. */
fun DetectedPose.hasUsableBody(): Boolean {
    val visible = landmarks.filter { it.inFrameLikelihood >= .5f }.map { it.type }.toSet()
    return listOf(11, 12, 23, 24).all { it in visible } &&
        visible.count { it in listOf(13, 14, 15, 16, 25, 26, 27, 28) } >= 4
}

fun DetectedPose.mirrored(): DetectedPose = copy(landmarks = landmarks.map { it.copy(x = imageWidth - it.x) })
