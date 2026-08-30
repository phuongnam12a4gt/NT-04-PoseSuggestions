package com.ppnnttt.posesuggestions

import android.content.Context

class FeedbackGenerator(private val context: Context) {
    private val positiveFeedback = listOf(
        R.string.feedback_great,
        R.string.feedback_perfect,
        R.string.feedback_aligned
    )

    private val encouragementFeedback = listOf(
        R.string.feedback_almost,
        R.string.feedback_close,
        R.string.feedback_improving
    )

    fun getPositiveReinforcement(): String {
        return context.getString(positiveFeedback.random())
    }

    fun getEncouragement(): String {
        return context.getString(encouragementFeedback.random())
    }
}
