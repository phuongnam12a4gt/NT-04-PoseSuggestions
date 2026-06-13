package com.ppnnttt.posesuggestions

import android.content.Context

class ScoreManager(context: Context) {
    private val prefs = context.getSharedPreferences("score_prefs", Context.MODE_PRIVATE)

    fun saveHighScore(poseId: String, score: Int) {
        val currentHighScore = getHighScore(poseId)
        if (score > currentHighScore) {
            prefs.edit().putInt("high_score_$poseId", score).apply()
        }
    }

    fun getHighScore(poseId: String): Int {
        return prefs.getInt("high_score_$poseId", 0)
    }

    fun getTotalHighScore(): Int {
        return prefs.all.filter { it.key.startsWith("high_score_") }
            .values.filterIsInstance<Int>().sum()
    }
}
