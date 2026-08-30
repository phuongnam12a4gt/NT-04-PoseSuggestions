package com.ppnnttt.posesuggestions

import android.content.Context

data class RecommendationInput(
    val locationType: String,
    val outfitStyle: String,
    val gender: String,
    val mood: String
)

class RecommendationEngine(private val repository: PoseTemplateRepository) {

    fun recommendPoses(input: RecommendationInput): List<PoseTemplate> {
        val allTemplates = repository.loadTemplates()
        val queryTags = listOf(
            input.locationType,
            input.outfitStyle,
            input.gender,
            input.mood
        ).map { it.lowercase() }

        return allTemplates.filter { template ->
            val metadata = template.recommendationMetadata
            if (metadata != null) {
                // Check how many tags match
                val matchCount = metadata.tags.count { tag ->
                    queryTags.contains(tag.lowercase())
                }
                matchCount > 0
            } else {
                false
            }
        }.sortedByDescending { template ->
            // Sort by number of matching tags
            template.recommendationMetadata?.tags?.count { tag ->
                queryTags.contains(tag.lowercase())
            } ?: 0
        }
    }
}

class PromptBuilder(private val context: Context) {
    fun buildRecommendationDescription(template: PoseTemplate, input: RecommendationInput): String {
        val metadata = template.recommendationMetadata ?: return context.getString(R.string.try_this_pose)
        
        return context.getString(
            R.string.recommendation_description,
            input.mood,
            input.locationType,
            template.name,
            metadata.cameraAngle,
            metadata.bodyOrientation,
            metadata.handPlacement
        )
    }
}
