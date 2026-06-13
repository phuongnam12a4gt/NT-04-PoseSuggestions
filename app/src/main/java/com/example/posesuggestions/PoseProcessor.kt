package com.example.posesuggestions

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

class PoseProcessor {
    private val optionsStream = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .setPreferredHardwareConfigs(AccuratePoseDetectorOptions.CPU_GPU) // Sử dụng GPU để nhanh hơn
        .build()

    private val optionsStatic = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
        .setPreferredHardwareConfigs(AccuratePoseDetectorOptions.CPU_GPU)
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
