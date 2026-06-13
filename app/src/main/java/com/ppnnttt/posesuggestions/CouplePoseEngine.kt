package com.ppnnttt.posesuggestions

import kotlin.math.abs

class CouplePoseEngine(
    private val similarityEngine: PoseSimilarityEngine
) {

    data class CoupleScore(
        val person1Score: Float,
        val person2Score: Float,
        val syncScore: Float,
        val totalScore: Float
    )

    fun calculateCoupleScore(
        person1: DetectedPose,
        person2: DetectedPose,
        template: PoseTemplate
    ): CoupleScore {
        val score1 = similarityEngine.calculateSimilarity(person1, template)
        
        // Use a dummy PoseTemplate for the partner to use similarityEngine
        val partnerTemplate = template.copy(landmarks = template.landmarksPartner ?: emptyList())
        val score2 = similarityEngine.calculateSimilarity(person2, partnerTemplate)

        // Synchronization score: how similar are their individual scores?
        // High sync means they are both doing well at the same time.
        val sync = (100f - abs(score1 - score2)).coerceIn(0f, 100f)

        val total = (score1 * 0.4f + score2 * 0.4f + sync * 0.2f)

        return CoupleScore(score1, score2, sync, total)
    }
}
