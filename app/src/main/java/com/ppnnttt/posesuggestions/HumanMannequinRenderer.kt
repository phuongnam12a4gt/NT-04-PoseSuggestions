package com.ppnnttt.posesuggestions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.max

/** Draws a lightweight human mannequin from ML Kit's 33 pose landmarks. */
fun DrawScope.drawHumanMannequin(
    pose: DetectedPose,
    opacity: Float,
    targetSize: Size = size
) {
    if (pose.landmarks.isEmpty() || pose.imageWidth <= 0 || pose.imageHeight <= 0) return

    val points = pose.landmarks.associate { landmark ->
        landmark.type to Offset(
            landmark.x / pose.imageWidth * targetSize.width,
            landmark.y / pose.imageHeight * targetSize.height
        )
    }
    fun point(type: Int): Offset? = points[type]
    fun midpoint(a: Offset, b: Offset) = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)

    val leftShoulder = point(PoseLandmark.LEFT_SHOULDER) ?: return
    val rightShoulder = point(PoseLandmark.RIGHT_SHOULDER) ?: return
    val leftHip = point(PoseLandmark.LEFT_HIP) ?: return
    val rightHip = point(PoseLandmark.RIGHT_HIP) ?: return
    val shoulderWidth = max((leftShoulder - rightShoulder).getDistance(), targetSize.minDimension * 0.12f)
    val outline = Color(0xFF052A35).copy(alpha = opacity * 0.85f)
    val skin = Color(0xFFFFC7A3).copy(alpha = opacity)
    val skinShadow = Color(0xFFE99B79).copy(alpha = opacity)
    val shirtTop = Color(0xFF25E6E6).copy(alpha = opacity)
    val shirtBottom = Color(0xFF2878F0).copy(alpha = opacity)
    val shorts = Color(0xFF243A73).copy(alpha = opacity)

    fun limb(startType: Int, endType: Int, width: Float, color: Color) {
        val start = point(startType) ?: return
        val end = point(endType) ?: return
        drawLine(outline, start, end, width + 5f, cap = StrokeCap.Round)
        drawLine(color, start, end, width, cap = StrokeCap.Round)
    }

    val armWidth = shoulderWidth * 0.16f
    val legWidth = shoulderWidth * 0.22f
    limb(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, armWidth, skinShadow)
    limb(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, armWidth * 0.82f, skinShadow)
    limb(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, armWidth, skin)
    limb(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, armWidth * 0.82f, skin)
    limb(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, legWidth, shorts)
    limb(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE, legWidth * 0.78f, skinShadow)
    limb(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, legWidth, shorts)
    limb(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE, legWidth * 0.78f, skin)

    val torso = Path().apply {
        moveTo(leftShoulder.x, leftShoulder.y)
        quadraticTo(midpoint(leftShoulder, rightShoulder).x,
            midpoint(leftShoulder, rightShoulder).y - shoulderWidth * 0.05f,
            rightShoulder.x, rightShoulder.y)
        lineTo(rightHip.x, rightHip.y)
        quadraticTo(midpoint(leftHip, rightHip).x,
            midpoint(leftHip, rightHip).y + shoulderWidth * 0.04f,
            leftHip.x, leftHip.y)
        close()
    }
    drawPath(torso, outline, style = Stroke(width = 7f))
    drawPath(torso, Brush.verticalGradient(listOf(shirtTop, shirtBottom)))

    val shoulderCenter = midpoint(leftShoulder, rightShoulder)
    val nose = point(PoseLandmark.NOSE)
    val leftEar = point(PoseLandmark.LEFT_EAR)
    val rightEar = point(PoseLandmark.RIGHT_EAR)
    val headCenter = when {
        leftEar != null && rightEar != null -> midpoint(leftEar, rightEar)
        nose != null -> nose
        else -> Offset(shoulderCenter.x, shoulderCenter.y - shoulderWidth * 0.62f)
    }
    val headWidth = if (leftEar != null && rightEar != null) {
        max((leftEar - rightEar).getDistance() * 1.35f, shoulderWidth * 0.28f)
    } else shoulderWidth * 0.38f
    val headHeight = headWidth * 1.25f

    val neckStart = Offset(shoulderCenter.x, shoulderCenter.y - shoulderWidth * 0.02f)
    val neckEnd = Offset(headCenter.x, headCenter.y + headHeight * 0.34f)
    drawLine(outline, neckStart, neckEnd, headWidth * 0.34f + 4f, cap = StrokeCap.Round)
    drawLine(skin, neckStart, neckEnd, headWidth * 0.34f, cap = StrokeCap.Round)

    val headRect = Rect(
        left = headCenter.x - headWidth / 2f,
        top = headCenter.y - headHeight / 2f,
        right = headCenter.x + headWidth / 2f,
        bottom = headCenter.y + headHeight / 2f
    )
    drawOval(
        outline,
        headRect.topLeft - Offset(3f, 3f),
        Size(headRect.width + 6f, headRect.height + 6f)
    )
    drawOval(skin, headRect.topLeft, headRect.size)
    drawArc(Color(0xFF263238).copy(alpha = opacity), 180f, 180f, true,
        headRect.topLeft, headRect.size)
    val eyeColor = Color(0xFF49332C).copy(alpha = opacity)
    drawCircle(eyeColor, headWidth * 0.035f,
        Offset(headCenter.x - headWidth * 0.16f, headCenter.y))
    drawCircle(eyeColor, headWidth * 0.035f,
        Offset(headCenter.x + headWidth * 0.16f, headCenter.y))

    listOf(PoseLandmark.LEFT_WRIST to skin, PoseLandmark.RIGHT_WRIST to skinShadow).forEach { (type, color) ->
        point(type)?.let { drawCircle(color, armWidth * 0.55f, it) }
    }
    listOf(PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE).forEach { type ->
        point(type)?.let {
            drawOval(Color.White.copy(alpha = opacity),
                Offset(it.x - legWidth * 0.5f, it.y - legWidth * 0.22f),
                Size(legWidth, legWidth * 0.55f))
        }
    }
}
