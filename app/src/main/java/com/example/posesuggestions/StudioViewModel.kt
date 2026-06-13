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
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PoseTemplateRepository(application)
    private val poseProcessor = PoseProcessor()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _extractedPose = MutableStateFlow<DetectedPose?>(null)
    val extractedPose = _extractedPose.asStateFlow()

    private val _sourceBitmap = MutableStateFlow<Bitmap?>(null)
    val sourceBitmap = _sourceBitmap.asStateFlow()

    fun extractPoseFromImage(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val bitmap = getBitmapFromUri(uri)
                if (bitmap != null) {
                    _sourceBitmap.value = bitmap
                    val image = InputImage.fromBitmap(bitmap, 0)
                    poseProcessor.processImage(image, isStatic = true)
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
        val source = _sourceBitmap.value ?: return
        
        // 1. Tạo File path cho ảnh preview
        val poseId = "custom_${UUID.randomUUID()}"
        val fileName = "$poseId.png"
        val file = File(getApplication<Application>().filesDir, fileName)
        
        // 2. Crop và lưu ảnh người mẫu thực tế
        try {
            val out = FileOutputStream(file)
            source.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Chuẩn hóa landmarks
        val normalizedLandmarks = normalizeLandmarks(pose.landmarks, pose.imageWidth, pose.imageHeight)

        val template = PoseTemplate(
            id = poseId,
            name = name,
            category = category,
            difficulty = "Easy",
            previewImage = file.absolutePath, // Lưu đường dẫn ảnh thật
            landmarks = normalizedLandmarks,
            isCustom = true
        )
        repository.saveCustomTemplate(template)
        
        _extractedPose.value = null
        _sourceBitmap.value = null
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
