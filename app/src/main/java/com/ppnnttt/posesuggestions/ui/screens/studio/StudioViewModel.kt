package com.ppnnttt.posesuggestions.ui.screens.studio

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.ppnnttt.posesuggestions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PoseTemplateRepository(application)
    private val processor = PoseProcessor()
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
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    private var capture: ImageCapture? = null
    private var provider: ProcessCameraProvider? = null
    private var countdown: Job? = null
    private var generation = 0

    private fun error(resource: Int) { _errorMessage.value = getApplication<Application>().getString(resource) }
    fun clearError() { _errorMessage.value = null }
    fun setCameraActive(active: Boolean) {
        if (!active) { generation++; countdown?.cancel(); countdown = null; _countdownValue.value = null; provider?.unbindAll(); capture = null }
        _isCameraActive.value = active
    }
    fun leave() { setCameraActive(false); generation++; _isProcessing.value = false }
    fun resetPose() { _sourceBitmap.value = null; _extractedPose.value = null }
    fun startPoseCapture() {
        if (_isProcessing.value || countdown?.isActive == true || capture == null) return
        countdown = viewModelScope.launch {
            for (i in 3 downTo 1) { _countdownValue.value = i; delay(1000) }
            _countdownValue.value = null
            val camera = capture ?: return@launch
            _isProcessing.value = true
            val session = generation
            val file = File.createTempFile("studio-", ".jpg", getApplication<Application>().cacheDir)
            camera.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(getApplication()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                        if (session == generation) extractPoseFromImage(Uri.fromFile(file), file) else file.delete()
                    }
                    override fun onError(exception: ImageCaptureException) {
                        file.delete()
                        if (session == generation) { _isProcessing.value = false; error(R.string.photo_save_failed) }
                    }
                })
        }
    }
    fun bindCamera(owner: LifecycleOwner, preview: androidx.camera.view.PreviewView) {
        val session = ++generation
        val future = ProcessCameraProvider.getInstance(preview.context)
        future.addListener({
            if (session != generation || !_isCameraActive.value) return@addListener
            try {
                val cameraProvider = future.get(); provider = cameraProvider
                val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                capture = ImageCapture.Builder().build()
                val cameraPreview = Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(owner, selector, cameraPreview, capture)
            } catch (e: Exception) { error(R.string.camera_start_failed) }
        }, ContextCompat.getMainExecutor(getApplication()))
    }
    fun extractPoseFromImage(uri: Uri, temporaryFile: File? = null) {
        val session = ++generation
        _isProcessing.value = true; _extractedPose.value = null
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) { loadPhoto(getApplication(), uri) }
                if (session != generation) return@launch
                processor.processImage(InputImage.fromBitmap(bitmap, 0), true)
                    .addOnSuccessListener { pose ->
                        if (session == generation) {
                            val detected = pose.toDetectedPose(bitmap.width, bitmap.height)
                            if (detected.hasUsableBody()) { _sourceBitmap.value = bitmap; _extractedPose.value = detected; setCameraActive(false) }
                            else error(R.string.no_pose_in_image)
                            _isProcessing.value = false
                        }
                    }.addOnFailureListener {
                        if (session == generation) { _isProcessing.value = false; error(R.string.no_pose_in_image) }
                    }
            } catch (e: Exception) {
                if (session == generation) { _isProcessing.value = false; error(R.string.photo_load_failed) }
            } finally { temporaryFile?.delete() }
        }
    }
    fun saveAsTemplate(name: String, category: String, onComplete: () -> Unit) {
        if (_isProcessing.value) return
        val pose = _extractedPose.value ?: return
        val bitmap = _sourceBitmap.value ?: return
        if (!pose.hasUsableBody()) { error(R.string.no_pose_in_image); return }
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val id = "custom_${UUID.randomUUID()}"
                    val file = File(getApplication<Application>().filesDir, "$id.jpg")
                    file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
                    repository.saveCustomTemplate(PoseTemplate(id, name.trim().take(80), category, file.absolutePath, "Easy",
                        pose.landmarks.filter { it.inFrameLikelihood >= .5f }.map { LandmarkTemplate(it.type, it.x / pose.imageWidth, it.y / pose.imageHeight) }, sourceAspectRatio = pose.imageWidth.toFloat() / pose.imageHeight, isCustom = true))
                }
                resetPose(); onComplete()
            } catch (e: Exception) { error(R.string.pose_save_failed) }
            finally { _isProcessing.value = false }
        }
    }
    override fun onCleared() { leave(); processor.stop(); super.onCleared() }
}
