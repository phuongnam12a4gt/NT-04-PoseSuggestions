package com.example.posesuggestions

import kotlinx.serialization.Serializable

@Serializable
data class LandmarkTemplate(
    val type: Int,
    val x: Float,
    val y: Float
)

@Serializable
data class PoseTemplate(
    val id: String,
    val name: String,
    val category: String,
    val previewImage: String, // Resource name or URL
    val difficulty: String, // "Easy", "Medium", "Hard"
    val landmarks: List<LandmarkTemplate>
)

@Serializable
data class PoseTemplatesConfig(
    val templates: List<PoseTemplate>
)

fun PoseTemplate.toDetectedPose(width: Int, height: Int): DetectedPose {
    return DetectedPose(
        landmarks = landmarks.map {
            PoseLandmarkData(
                type = it.type,
                x = it.x * width,
                y = it.y * height,
                inFrameLikelihood = 1.0f
            )
        },
        imageWidth = width,
        imageHeight = height
    )
}
