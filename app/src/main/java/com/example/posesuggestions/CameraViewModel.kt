package com.example.posesuggestions

import android.media.MediaActionSound
import android.os.Environment
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val repository = PoseTemplateRepository(application)
    private val similarityEngine = PoseSimilarityEngine()
    private val shutterSound = MediaActionSound()

    private val _detectedPose = MutableStateFlow<DetectedPose?>(null)
    val detectedPose = _detectedPose.asStateFlow()

    private val _currentScore = MutableStateFlow(0f)
    val currentScore = _currentScore.asStateFlow()

    private val _templates = MutableStateFlow<List<PoseTemplate>>(emptyList())
    val templates = _templates.asStateFlow()

    private val _selectedCategory = MutableStateFlow("cool")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedTemplate = MutableStateFlow<PoseTemplate?>(null)
    val selectedTemplate = _selectedTemplate.asStateFlow()

    private val _captureThreshold = MutableStateFlow(85f)
    val captureThreshold = _captureThreshold.asStateFlow()

    private val _countdownValue = MutableStateFlow<Int?>(null)
    val countdownValue = _countdownValue.asStateFlow()

    private val _ghostOpacity = MutableStateFlow(0.5f)
    val ghostOpacity = _ghostOpacity.asStateFlow()

    fun setGhostOpacity(opacity: Float) {
        _ghostOpacity.value = opacity
    }

    private var isCapturing = false
    private var countdownJob: Job? = null
    private var imageCapture: ImageCapture? = null

    private val poseProcessor = PoseProcessor()
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        loadTemplates()
        shutterSound.load(MediaActionSound.SHUTTER_CLICK)
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            _templates.value = repository.getTemplatesByCategory(_selectedCategory.value)
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _selectedTemplate.value = null
        cancelCountdown()
        loadTemplates()
    }

    fun selectTemplate(template: PoseTemplate) {
        _selectedTemplate.value = template
        cancelCountdown()
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

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, PoseAnalyzer(poseProcessor) { pose, width, height ->
                        val detected = pose.toDetectedPose(width, height)
                        _detectedPose.value = detected
                        
                        _selectedTemplate.value?.let { template ->
                            val score = similarityEngine.calculateSimilarity(detected, template)
                            _currentScore.value = score
                            checkAutoCapture(score)
                        } ?: run {
                            _currentScore.value = 0f
                            cancelCountdown()
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                    imageCapture
                )
            } catch (exc: Exception) {
                // Handle binding failure
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun checkAutoCapture(score: Float) {
        if (score >= _captureThreshold.value && !isCapturing && countdownJob == null) {
            startCountdown()
        } else if (score < _captureThreshold.value - 5f && countdownJob != null) {
            // Add a small buffer to prevent flickering
            cancelCountdown()
        }
    }

    private fun startCountdown() {
        countdownJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _countdownValue.value = i
                delay(1000)
            }
            _countdownValue.value = null
            capturePhoto()
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _countdownValue.value = null
    }

    private fun capturePhoto() {
        val capture = imageCapture ?: return
        if (isCapturing) return
        isCapturing = true

        val photoFile = File(
            getApplication<android.app.Application>().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        shutterSound.play(MediaActionSound.SHUTTER_CLICK)

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(getApplication()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Toast.makeText(getApplication(), "Pose Captured!", Toast.LENGTH_SHORT).show()
                    viewModelScope.launch {
                        delay(2000) // Cooldown before next capture
                        isCapturing = false
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                }
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        poseProcessor.stop()
        shutterSound.release()
        cameraExecutor.shutdown()
    }
}
