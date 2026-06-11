package com.example.posesuggestions

import kotlin.random.Random

class FeedbackGenerator {
    private val positiveFeedback = listOf(
        "Great job!",
        "Looking good!",
        "Perfect!",
        "Spot on!",
        "You've got it!",
        "Excellent alignment!"
    )

    private val encouragementFeedback = listOf(
        "Almost there, keep going!",
        "So close!",
        "Just a little more adjustment.",
        "You're improving!"
    )

    fun getPositiveReinforcement(): String {
        return positiveFeedback[Random.nextInt(positiveFeedback.size)]
    }

    fun getEncouragement(): String {
        return encouragementFeedback[Random.nextInt(encouragementFeedback.size)]
    }

    fun getCorrection(jointName: String, action: String): String {
        return "Try to $action your $jointName slightly."
    }
}
