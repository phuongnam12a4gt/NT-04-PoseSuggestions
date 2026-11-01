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

/** Live scoring requires confident, in-frame torso and limb observations. */
fun DetectedPose.hasTrackableBody(): Boolean {
    if (imageWidth <= 0 || imageHeight <= 0) return false
    val visible = landmarks.filter {
        it.inFrameLikelihood >= .75f && it.x in 0f..imageWidth.toFloat() && it.y in 0f..imageHeight.toFloat()
    }
    val types = visible.map { it.type }.toSet()
    if (!listOf(11, 12, 23, 24).all { it in types }) return false
    if (types.count { it in listOf(13, 14, 15, 16, 25, 26, 27, 28) } < 4) return false
    val body = visible.filter { it.type in 11..16 || it.type in 23..28 }
    return body.maxOf { it.y } - body.minOf { it.y } >= imageHeight * .35f &&
        body.maxOf { it.x } - body.minOf { it.x } >= imageWidth * .1f
}
