package com.ppnnttt.posesuggestions

import androidx.annotation.DrawableRes

@DrawableRes
fun PoseTemplate.artworkResource(): Int = when (previewImage) {
    "cool_lean_preview" -> R.drawable.cool_lean_preview
    "mirror_selfie_preview" -> R.drawable.mirror_selfie_preview
    "mountain_pose_preview" -> R.drawable.mountain_pose_preview
    "bicep_flex_preview" -> R.drawable.bicep_flex_preview
    else -> 0
}
