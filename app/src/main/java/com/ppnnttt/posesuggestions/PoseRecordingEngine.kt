package com.ppnnttt.posesuggestions

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class PoseRecorder {
    private var isRecording = false
    private var startTime = 0L
    private val frames = mutableListOf<PoseFrame>()

    fun start() {
        isRecording = true
        startTime = System.currentTimeMillis()
        frames.clear()
    }

    fun recordFrame(pose: DetectedPose) {
        if (!isRecording) return
        
        val timestamp = System.currentTimeMillis() - startTime
        val landmarkTemplates = pose.landmarks.map {
            LandmarkTemplate(it.type, it.x / pose.imageWidth, it.y / pose.imageHeight)
        }
        frames.add(PoseFrame(timestamp, landmarkTemplates))
    }

    fun stop(name: String, templateId: String?): PoseRecording? {
        if (!isRecording) return null
        isRecording = false
        val duration = System.currentTimeMillis() - startTime
        return PoseRecording(
            id = UUID.randomUUID().toString(),
            name = name,
            templateId = templateId,
            frames = frames.toList(),
            durationMillis = duration
        )
    }
}

class ReplayEngine {
    private val _currentFrame = MutableStateFlow<DetectedPose?>(null)
    val currentFrame = _currentFrame.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private var replayJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun play(recording: PoseRecording, width: Int, height: Int) {
        stop()
        _isPlaying.value = true
        replayJob = scope.launch {
            val startTime = System.currentTimeMillis()
            var frameIndex = 0
            
            while (frameIndex < recording.frames.size) {
                val elapsed = System.currentTimeMillis() - startTime
                val frame = recording.frames[frameIndex]
                
                if (elapsed >= frame.timestamp) {
                    val landmarksData = frame.landmarks.map {
                        PoseLandmarkData(it.type, it.x * width, it.y * height, 1.0f)
                    }
                    _currentFrame.value = DetectedPose(landmarksData, width, height)
                    frameIndex++
                } else {
                    delay(10) // Small delay to avoid tight loop
                }
            }
            _isPlaying.value = false
        }
    }

    fun stop() {
        replayJob?.cancel()
        _isPlaying.value = false
        _currentFrame.value = null
    }
}
