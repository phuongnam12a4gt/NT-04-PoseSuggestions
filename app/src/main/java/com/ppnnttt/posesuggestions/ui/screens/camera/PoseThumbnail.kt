package com.ppnnttt.posesuggestions.ui.screens.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.ppnnttt.posesuggestions.*

@Composable
fun PoseThumbnail(template: PoseTemplate, modifier: Modifier = Modifier) {
    val resource = template.artworkResource()
    if (template.isCustom || resource != 0) {
        AsyncImage(model = if (template.isCustom) java.io.File(template.previewImage) else resource,
            contentDescription = template.name, modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        Canvas(modifier) { drawHumanMannequin(template.toDetectedPose(1000, 1000), 1f) }
    }
}
