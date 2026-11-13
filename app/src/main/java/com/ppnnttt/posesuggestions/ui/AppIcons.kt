package com.ppnnttt.posesuggestions.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    val Camera = ImageVector.Builder("Camera", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(9f, 3f); lineTo(7f, 6f); lineTo(3f, 6f); lineTo(3f, 21f)
            lineTo(21f, 21f); lineTo(21f, 6f); lineTo(17f, 6f); lineTo(15f, 3f); close()
            moveTo(12f, 8f); curveTo(5f, 8f, 5f, 19f, 12f, 19f)
            curveTo(19f, 19f, 19f, 8f, 12f, 8f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 10f); curveTo(7.5f, 10f, 7.5f, 17f, 12f, 17f)
            curveTo(16.5f, 17f, 16.5f, 10f, 12f, 10f); close()
        }
    }.build()

    val FlipCamera = ImageVector.Builder("Flip camera", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 7f); lineTo(17f, 7f); lineTo(17f, 4f); lineTo(22f, 8f)
            lineTo(17f, 12f); lineTo(17f, 9f); lineTo(4f, 9f); close()
            moveTo(20f, 15f); lineTo(7f, 15f); lineTo(7f, 12f); lineTo(2f, 16f)
            lineTo(7f, 20f); lineTo(7f, 17f); lineTo(20f, 17f); close()
        }
    }.build()
}
