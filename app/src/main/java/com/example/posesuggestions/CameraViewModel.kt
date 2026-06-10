package com.example.posesuggestions

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val repository = PoseTemplateRepository(application)
    private val similarityEngine = PoseSimilarityEngine()

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

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            _templates.value = repository.getTemplatesByCategory(_selectedCategory.value)
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _selectedTemplate.value = null
        loadTemplates()
    }

    fun selectTemplate(template: PoseTemplate) {
        _selectedTemplate.value = template
    }

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
                        val detected = pose.toDetectedPose(width, height)
                        _detectedPose.value = detected
                        
                        _selectedTemplate.value?.let { template ->
                            _currentScore.value = similarityEngine.calculateSimilarity(detected, template)
                        } ?: run {
                            _currentScore.value = 0f
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
