package com.example.posesuggestions

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class PoseProcessor {
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector: PoseDetector = PoseDetection.getClient(options)

    fun processImage(image: InputImage): Task<Pose> =
        detector.process(image)

    fun stop() {
        detector.close()
    }
}
