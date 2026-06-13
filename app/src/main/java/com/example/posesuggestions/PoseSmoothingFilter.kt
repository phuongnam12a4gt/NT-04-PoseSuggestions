package com.example.posesuggestions

/**
 * Bộ lọc làm mượt chuyển động (Exponential Moving Average)
 * Giúp khung xương không bị rung (jitter) do nhiễu camera.
 */
class PoseSmoothingFilter(private val alpha: Float = 0.5f) {
    private var lastPose: DetectedPose? = null

    fun filter(currentPose: DetectedPose): DetectedPose {
        val previousPose = lastPose ?: return currentPose.also { lastPose = it }

        // Nếu kích thước ảnh thay đổi, reset bộ lọc
        if (currentPose.imageWidth != previousPose.imageWidth || currentPose.imageHeight != previousPose.imageHeight) {
            return currentPose.also { lastPose = it }
        }

        val smoothedLandmarks = currentPose.landmarks.map { current ->
            val previous = previousPose.landmarks.find { it.type == current.type }
            if (previous != null) {
                PoseLandmarkData(
                    type = current.type,
                    x = previous.x + alpha * (current.x - previous.x),
                    y = previous.y + alpha * (current.y - previous.y),
                    inFrameLikelihood = current.inFrameLikelihood
                )
            } else {
                current
            }
        }

        val result = currentPose.copy(landmarks = smoothedLandmarks)
        lastPose = result
        return result
    }

    fun reset() {
        lastPose = null
    }
}
