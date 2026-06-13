package com.example.posesuggestions

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PoseTemplateRepository(application)
    private val poseProcessor = PoseProcessor()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _extractedPose = MutableStateFlow<DetectedPose?>(null)
    val extractedPose = _extractedPose.asStateFlow()

    fun extractPoseFromImage(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val bitmap = getBitmapFromUri(uri)
                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    poseProcessor.processImage(image)
                        .addOnSuccessListener { pose ->
                            val detected = pose.toDetectedPose(bitmap.width, bitmap.height)
                            _extractedPose.value = detected
                            _isProcessing.value = false
                        }
                        .addOnFailureListener {
                            _isProcessing.value = false
                        }
                }
            } catch (e: Exception) {
                _isProcessing.value = false
            }
        }
    }

    fun saveAsTemplate(name: String, category: String) {
        val pose = _extractedPose.value ?: return
        val template = PoseTemplate(
            id = "custom_${UUID.randomUUID()}",
            name = name,
            category = category,
            difficulty = "Easy",
            previewImage = "custom_pose", // Placeholder
            landmarks = pose.landmarks.map { LandmarkTemplate(it.type, it.x / pose.imageWidth, it.y / pose.imageHeight) }
        )
        repository.saveCustomTemplate(template)
        _extractedPose.value = null
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(getApplication<Application>().contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                MediaStore.Images.Media.getBitmap(getApplication<Application>().contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        poseProcessor.stop()
    }
}
