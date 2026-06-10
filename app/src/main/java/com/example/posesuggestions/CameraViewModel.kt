package com.example.posesuggestions

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraViewModel : ViewModel() {
    private val _detectedPose = MutableStateFlow<DetectedPose?>(null)
    val detectedPose = _detectedPose.asStateFlow()

    private val poseProcessor = PoseProcessor()
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView
    ) {
        val context = previewView.context
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, PoseAnalyzer(poseProcessor) { pose, width, height ->
                        _detectedPose.value = pose.toDetectedPose(width, height)
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                // Handle binding failure
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun onCleared() {
        super.onCleared()
        poseProcessor.stop()
        cameraExecutor.shutdown()
    }
}
