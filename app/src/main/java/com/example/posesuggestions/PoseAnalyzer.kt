package com.example.posesuggestions

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose

class PoseAnalyzer(
    private val processor: PoseProcessor,
    private val onPoseDetected: (Pose, Int, Int) -> Unit
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val width = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
            val height = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height
            
            val image = InputImage.fromMediaImage(mediaImage, rotation)
            processor.processImage(image)
                .addOnSuccessListener { pose ->
                    onPoseDetected(pose, width, height)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
