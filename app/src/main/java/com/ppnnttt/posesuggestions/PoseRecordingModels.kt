package com.ppnnttt.posesuggestions

import kotlinx.serialization.Serializable

@Serializable
data class PoseFrame(
    val timestamp: Long,
    val landmarks: List<LandmarkTemplate>
)

@Serializable
data class PoseRecording(
    val id: String,
    val name: String,
    val templateId: String?,
    val frames: List<PoseFrame>,
    val durationMillis: Long
)
