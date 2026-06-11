package com.example.posesuggestions

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ChallengeState {
    IDLE,
    COUNTDOWN,
    ACTIVE,
    FINISHED
}

class ChallengeEngine(
    private val onChallengeFinished: (Float) -> Unit
) {
    private var challengeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(ChallengeState.IDLE)
    val state = _state.asStateFlow()

    private val _timeLeft = MutableStateFlow(0)
    val timeLeft = _timeLeft.asStateFlow()

    private var maxScoreAchieved = 0f

    fun startChallenge(durationSeconds: Int = 10) {
        challengeJob?.cancel()
        maxScoreAchieved = 0f
        challengeJob = scope.launch {
            // Pre-start countdown
            _state.value = ChallengeState.COUNTDOWN
            for (i in 3 downTo 1) {
                _timeLeft.value = i
                delay(1000)
            }

            // Challenge active
            _state.value = ChallengeState.ACTIVE
            for (i in durationSeconds downTo 1) {
                _timeLeft.value = i
                delay(1000)
            }

            _state.value = ChallengeState.FINISHED
            onChallengeFinished(maxScoreAchieved)
            
            delay(3000)
            _state.value = ChallengeState.IDLE
        }
    }

    fun updateCurrentScore(score: Float) {
        if (_state.value == ChallengeState.ACTIVE) {
            if (score > maxScoreAchieved) {
                maxScoreAchieved = score
            }
        }
    }

    fun stop() {
        challengeJob?.cancel()
        _state.value = ChallengeState.IDLE
    }
}
