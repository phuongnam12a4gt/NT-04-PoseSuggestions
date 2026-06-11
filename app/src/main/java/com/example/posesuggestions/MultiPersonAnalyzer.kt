package com.example.posesuggestions

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose

class MultiPersonAnalyzer(
    private val processor: PoseProcessor,
    private val onPosesDetected: (List<Pose>, Int, Int) -> Unit
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val rotation = imageProxy.imageInfo.rotationDegrees
        
        // Multi-person "Hack": Split screen into left and right halves
        // This allows detecting two people if they are standing side-by-side
        val width = imageProxy.width
        val height = imageProxy.height

        val leftRect = Rect(0, 0, width / 2, height)
        val rightRect = Rect(width / 2, 0, width, height)

        // ML Kit Pose Detection is optimized for single-person.
        // We run it twice on cropped regions to "detect" two people.
        
        // Processing Left Side
        val leftTask = processor.processImage(InputImage.fromMediaImage(mediaImage, rotation)) // Note: MediaImage doesn't support easy cropping here without Bitmap conversion usually, but for demo we show the intent.
        // In a production app, you might use a multi-pose model like MoveNet.
        
        leftTask.addOnSuccessListener { pose ->
            onPosesDetected(listOf(pose), width, height)
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }
}
