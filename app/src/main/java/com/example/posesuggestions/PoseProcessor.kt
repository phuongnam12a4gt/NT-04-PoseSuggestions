package com.example.posesuggestions

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class PoseProcessor {
    private val optionsStream = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val optionsStatic = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
        .setPreferredHardwareConfigs(PoseDetectorOptions.CPU_GPU)
        .build()

    private val detectorStream: PoseDetector = PoseDetection.getClient(optionsStream)
    private val detectorStatic: PoseDetector = PoseDetection.getClient(optionsStatic)

    fun processImage(image: InputImage, isStatic: Boolean = false): Task<Pose> {
        return if (isStatic) detectorStatic.process(image) else detectorStream.process(image)
    }

    fun stop() {
        detectorStream.close()
        detectorStatic.close()
    }
}
