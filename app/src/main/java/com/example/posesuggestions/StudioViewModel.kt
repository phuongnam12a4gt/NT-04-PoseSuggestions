package com.example.posesuggestions

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PoseTemplateRepository(application)
    private val poseProcessor = PoseProcessor()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _extractedPose = MutableStateFlow<DetectedPose?>(null)
    val extractedPose = _extractedPose.asStateFlow()

    private val _sourceBitmap = MutableStateFlow<Bitmap?>(null)
    val sourceBitmap = _sourceBitmap.asStateFlow()

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive = _isCameraActive.asStateFlow()

    private val _countdownValue = MutableStateFlow<Int?>(null)
    val countdownValue = _countdownValue.asStateFlow()

    private var imageCapture: ImageCapture? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun setCameraActive(active: Boolean) {
        _isCameraActive.value = active
    }

    fun startPoseCapture() {
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _countdownValue.value = i
                delay(1000)
            }
            _countdownValue.value = null
            captureAndProcess()
        }
    }

    private fun captureAndProcess() {
        val capture = imageCapture ?: return
        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    
                    if (bitmap != null) {
                        _sourceBitmap.value = bitmap
                        _isProcessing.value = true
                        val inputImage = InputImage.fromBitmap(bitmap, 0)
                        poseProcessor.processImage(inputImage, isStatic = true)
                            .addOnSuccessListener { pose ->
                                val detected = pose.toDetectedPose(bitmap.width, bitmap.height)
                                _extractedPose.value = detected
                                _isProcessing.value = false
                                _isCameraActive.value = false
                            }
                            .addOnFailureListener {
                                _isProcessing.value = false
                            }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    // Handle error
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            
            // Fix rotation
            val matrix = Matrix()
            matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e("StudioViewModel", "Error converting image proxy to bitmap: ${e.message}")
            null
        }
    }

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

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                // Handle binding failure
            }
        }, ContextCompat.getMainExecutor(context))
    }

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
        val pose = _extractedPose.value ?: run {
            Log.e("StudioViewModel", "Save failed: extractedPose is null")
            return
        }
        val source = _sourceBitmap.value ?: run {
            Log.e("StudioViewModel", "Save failed: sourceBitmap is null")
            return
        }
        
        Log.d("StudioViewModel", "Saving template: $name, category: $category")

        // 1. Tạo File path cho ảnh preview
        val poseId = "custom_${UUID.randomUUID()}"
        val fileName = "$poseId.png"
        val file = File(getApplication<Application>().filesDir, fileName)
        
        // 2. Crop và lưu ảnh người mẫu thực tế
        try {
            FileOutputStream(file).use { out ->
                source.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            Log.d("StudioViewModel", "Image saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("StudioViewModel", "Error saving image: ${e.message}")
            e.printStackTrace()
        }

        // 3. Chuẩn hóa landmarks
        try {
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
            Log.d("StudioViewModel", "Template saved successfully to repository")
        } catch (e: Exception) {
            Log.e("StudioViewModel", "Error saving template: ${e.message}")
            e.printStackTrace()
        }
        
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
        cameraExecutor.shutdown()
    }
}
