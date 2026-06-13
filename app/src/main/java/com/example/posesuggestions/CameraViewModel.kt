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
    private val guidanceEngine = PoseGuidanceEngine()
    val errorAnalysisEngine = ErrorAnalysisEngine()
    private val scoreManager = ScoreManager(application)
    
    private val recordingRepository = PoseRecordingRepository(application)
    private val poseRecorder = PoseRecorder()
    private val replayEngine = ReplayEngine()

    private val feedbackGenerator = FeedbackGenerator()
    private val voiceGuideManager = VoiceGuideManager(application)
    private val poseCoach = PoseCoachEngine(voiceGuideManager, feedbackGenerator)
    private val couplePoseEngine = CouplePoseEngine(similarityEngine)
    private val smoothingFilter = PoseSmoothingFilter(alpha = 0.3f) // Lọc mạnh để cực mượt
    private val smoothingFilterPartner = PoseSmoothingFilter(alpha = 0.3f)

    private val recommendationEngine = RecommendationEngine(repository)
    private val promptBuilder = PromptBuilder()

    private val challengeEngine = ChallengeEngine { finalScore ->
        _selectedTemplate.value?.let { template ->
            scoreManager.saveHighScore(template.id, finalScore.toInt())
        }
    }
    
    private val shutterSound = MediaActionSound()

    private val _detectedPose = MutableStateFlow<DetectedPose?>(null)
    val detectedPose = _detectedPose.asStateFlow()

    private val _detectedPosePartner = MutableStateFlow<DetectedPose?>(null)
    val detectedPosePartner = _detectedPosePartner.asStateFlow()

    private val _currentScore = MutableStateFlow(0f)
    val currentScore = _currentScore.asStateFlow()

    private val _guidanceMessage = MutableStateFlow<String?>(null)
    val guidanceMessage = _guidanceMessage.asStateFlow()

    private val _templates = MutableStateFlow<List<PoseTemplate>>(emptyList())
    val templates = _templates.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedTemplate = MutableStateFlow<PoseTemplate?>(null)
    val selectedTemplate = _selectedTemplate.asStateFlow()

    val challengeState = challengeEngine.state
    val challengeTimeLeft = challengeEngine.timeLeft

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow("All")
    val selectedDifficulty = _selectedDifficulty.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites = _favorites.asStateFlow()

    private val _recentTemplates = MutableStateFlow<List<PoseTemplate>>(emptyList())
    val recentTemplates = _recentTemplates.asStateFlow()

    private val _recommendedPose = MutableStateFlow<PoseTemplate?>(null)
    val recommendedPose = _recommendedPose.asStateFlow()

    private val _recommendationText = MutableStateFlow<String?>(null)
    val recommendationText = _recommendationText.asStateFlow()

    private val _captureThreshold = MutableStateFlow(85f)
    val captureThreshold = _captureThreshold.asStateFlow()

    private val _countdownValue = MutableStateFlow<Int?>(null)
    val countdownValue = _countdownValue.asStateFlow()

    private val _isRecordingPose = MutableStateFlow(false)
    val isRecordingPose = _isRecordingPose.asStateFlow()

    private val _recordedPoses = MutableStateFlow<List<PoseRecording>>(emptyList())
    val recordedPoses = _recordedPoses.asStateFlow()

    val replayFrame = replayEngine.currentFrame
    val isReplaying = replayEngine.isPlaying

    private val _ghostOpacity = MutableStateFlow(0.5f)
    val ghostOpacity = _ghostOpacity.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera = _isFrontCamera.asStateFlow()

    private val _lastCapturedPhoto = MutableStateFlow<File?>(null)
    val lastCapturedPhoto = _lastCapturedPhoto.asStateFlow()

    private var isCapturing = false
    private var countdownJob: Job? = null
    private var imageCapture: ImageCapture? = null

    private val poseProcessor = PoseProcessor()
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        loadTemplates()
        refreshUserData()
        loadRecordings()
        shutterSound.load(MediaActionSound.SHUTTER_CLICK)
    }

    private fun loadRecordings() {
        viewModelScope.launch {
            _recordedPoses.value = recordingRepository.loadRecordings()
        }
    }

    fun startRecording() {
        _isRecordingPose.value = true
        poseRecorder.start()
    }

    fun stopRecording(name: String) {
        _isRecordingPose.value = false
        val recording = poseRecorder.stop(name, _selectedTemplate.value?.id)
        if (recording != null) {
            recordingRepository.saveRecording(recording)
            loadRecordings()
        }
    }

    fun playRecording(recording: PoseRecording, width: Int, height: Int) {
        replayEngine.play(recording, width, height)
    }

    fun stopReplay() {
        replayEngine.stop()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            _templates.value = repository.getTemplates(
                query = _searchQuery.value,
                category = _selectedCategory.value,
                difficulty = _selectedDifficulty.value
            )
        }
    }

    private fun refreshUserData() {
        _favorites.value = repository.getFavorites()
        val recentIds = repository.getRecent()
        val allTemplates = repository.loadTemplates()
        _recentTemplates.value = recentIds.mapNotNull { id -> allTemplates.find { it.id == id } }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        loadTemplates()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadTemplates()
    }

    fun selectDifficulty(difficulty: String) {
        _selectedDifficulty.value = difficulty
        loadTemplates()
    }

    fun toggleFavorite(templateId: String) {
        repository.toggleFavorite(templateId)
        refreshUserData()
    }

    fun selectTemplate(template: PoseTemplate) {
        _selectedTemplate.value = template
        _recommendedPose.value = null // Clear recommendation when manually selecting
        _recommendationText.value = null
        repository.addToRecent(template.id)
        refreshUserData()
        cancelCountdown()
        challengeEngine.stop()
    }

    fun getRecommendations(input: RecommendationInput) {
        viewModelScope.launch {
            val recommendations = recommendationEngine.recommendPoses(input)
            if (recommendations.isNotEmpty()) {
                val best = recommendations.first()
                _recommendedPose.value = best
                _recommendationText.value = promptBuilder.buildRecommendationDescription(best, input)
                selectTemplate(best)
            }
        }
    }

    fun startRandomChallenge() {
        val all = repository.loadTemplates()
        if (all.isNotEmpty()) {
            val randomPose = all.random()
            selectTemplate(randomPose)
            challengeEngine.startChallenge()
        }
    }

    fun getHighScore(poseId: String): Int {
        return scoreManager.getHighScore(poseId)
    }

    fun setGhostOpacity(opacity: Float) {
        _ghostOpacity.value = opacity
    }

    fun toggleCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
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
                    it.setAnalyzer(cameraExecutor, PoseAnalyzer(poseProcessor) { poses, width, height ->
                        val detectedList = poses.map { it.toDetectedPose(width, height) }
                        
                        // Lọc mượt cho từng người
                        val p1Raw = detectedList.getOrNull(0)
                        val p2Raw = detectedList.getOrNull(1)
                        
                        val p1 = p1Raw?.let { smoothingFilter.filter(it) }
                        val p2 = p2Raw?.let { smoothingFilterPartner.filter(it) }
                        
                        _detectedPose.value = p1
                        _detectedPosePartner.value = p2
                        
                        // Record frame if active
                        p1?.let { poseRecorder.recordFrame(it) }
                        
                        _selectedTemplate.value?.let { template ->
                            val score = if (template.isCouple && p1 != null && p2 != null) {
                                val coupleScore = couplePoseEngine.calculateCoupleScore(p1, p2, template)
                                // Combine into a display score
                                coupleScore.totalScore
                            } else if (p1 != null) {
                                similarityEngine.calculateSimilarity(p1, template)
                            } else 0f

                            _currentScore.value = score
                            
                            // Coaching & Capture logic
                            val guidance = p1?.let { guidanceEngine.getGuidance(it, template) }
                            _guidanceMessage.value = guidance?.message
                            
                            if (score < 80f) {
                                poseCoach.provideCoaching(score, guidance)
                            } else {
                                _guidanceMessage.value = "Hold still!"
                                voiceGuideManager.speak("Hold still!")
                            }
                            
                            challengeEngine.updateCurrentScore(score)
                            checkAutoCapture(score)
                        } ?: run {
                            _currentScore.value = 0f
                            _guidanceMessage.value = null
                            cancelCountdown()
                        }
                    })
                }

            val cameraSelector = if (_isFrontCamera.value) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

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
                    _lastCapturedPhoto.value = photoFile
                    Toast.makeText(getApplication(), "Pose Captured!", Toast.LENGTH_SHORT).show()
                    viewModelScope.launch {
                        delay(2000)
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
        voiceGuideManager.shutdown()
        cameraExecutor.shutdown()
    }
}
