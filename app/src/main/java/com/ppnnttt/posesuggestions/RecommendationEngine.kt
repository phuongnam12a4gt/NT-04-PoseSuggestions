package com.ppnnttt.posesuggestions

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

class PromptBuilder {
    fun buildRecommendationDescription(template: PoseTemplate, input: RecommendationInput): String {
        val metadata = template.recommendationMetadata ?: return "Try this pose!"
        
        return "Based on your ${input.mood} mood at a ${input.locationType}, I recommend the \"${template.name}\". " +
                "Try a ${metadata.cameraAngle} camera angle. " +
                "Orient your body by ${metadata.bodyOrientation} and place your hands ${metadata.handPlacement}."
    }
}
