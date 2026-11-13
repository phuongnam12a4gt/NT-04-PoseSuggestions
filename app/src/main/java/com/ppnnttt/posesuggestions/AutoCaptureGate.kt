package com.ppnnttt.posesuggestions

/** One shot per held pose. Step out of the pose before another automatic shot. */
class AutoCaptureGate(private val threshold: Float = 85f) {
    private var armed = true
    fun canStart(score: Float): Boolean {
        if (score < threshold - 5f) armed = true
        return armed && score >= threshold
    }
    fun captured() { armed = false }
    fun reset() { armed = true }
}
