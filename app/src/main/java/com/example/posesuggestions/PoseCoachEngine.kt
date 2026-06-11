package com.example.posesuggestions

class PoseCoachEngine(
    private val voiceGuideManager: VoiceGuideManager,
    private val feedbackGenerator: FeedbackGenerator
) {
    private var lastScore = 0f
    private var lastPositiveReinforcementTime = 0L
    private val POSITIVE_REINFORCEMENT_COOLDOWN = 8000L

    fun provideCoaching(currentScore: Float, guidance: PoseGuidanceEngine.Guidance?) {
        val currentTime = System.currentTimeMillis()

        when {
            // High score - provide positive reinforcement occasionally
            currentScore > 90f -> {
                if (currentTime - lastPositiveReinforcementTime > POSITIVE_REINFORCEMENT_COOLDOWN) {
                    voiceGuideManager.speak(feedbackGenerator.getPositiveReinforcement())
                    lastPositiveReinforcementTime = currentTime
                }
            }

            // Improving - encourage
            currentScore > lastScore + 5f && currentScore > 60f -> {
                voiceGuideManager.speak(feedbackGenerator.getEncouragement())
            }

            // Needs correction
            guidance != null && currentScore < 85f -> {
                // Split guidance message or use FeedbackGenerator to make it natural
                // Note: guidance.message is already something like "Bend your left arm"
                voiceGuideManager.speak(guidance.message)
            }
        }

        lastScore = currentScore
    }
}
