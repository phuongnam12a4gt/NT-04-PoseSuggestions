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
    val previewImage: String, // Path to local file or resource
    val difficulty: String, // "Easy", "Medium", "Hard"
    val landmarks: List<LandmarkTemplate>,
    val landmarksPartner: List<LandmarkTemplate>? = null, // For couple poses
    val recommendationMetadata: RecommendationMetadata? = null,
    val isCustom: Boolean = false // Để biết đây là pose từ Studio hay có sẵn
) {
    val isCouple: Boolean get() = landmarksPartner != null
}

fun PoseTemplate.toPartnerDetectedPose(width: Int, height: Int): DetectedPose? {
    val partnerLandmarks = landmarksPartner ?: return null
    return DetectedPose(
        landmarks = partnerLandmarks.map {
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

@Serializable
data class RecommendationMetadata(
    val cameraAngle: String,
    val bodyOrientation: String,
    val handPlacement: String,
    val tags: List<String> // moods, locations, outfit styles
)

@Serializable
data class PoseTemplatesConfig(
    val templates: List<PoseTemplate>
)

/**
 * Thuật toán căn giữa và tối ưu hóa bộ xương để nằm gọn trong khung 1:1
 */
fun normalizeLandmarks(landmarks: List<PoseLandmarkData>, width: Int, height: Int): List<LandmarkTemplate> {
    if (landmarks.isEmpty()) return emptyList()

    val normX = landmarks.map { it.x / width }
    val normY = landmarks.map { it.y / height }
    
    val minX = normX.min()
    val maxX = normX.max()
    val minY = normY.min()
    val maxY = normY.max()
    
    val poseWidth = maxX - minX
    val poseHeight = maxY - minY
    val centerX = (minX + maxX) / 2f
    val centerY = (minY + maxY) / 2f
    
    // Scale để bộ xương chiếm 80% diện tích khung hình (tránh chạm mép)
    val scale = 0.8f / maxOf(poseWidth, poseHeight, 0.001f)

    return landmarks.map {
        LandmarkTemplate(
            type = it.type,
            x = (it.x / width - centerX) * scale + 0.5f,
            y = (it.y / height - centerY) * scale + 0.5f
        )
    }
}

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
