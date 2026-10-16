package com.ppnnttt.posesuggestions

/** Offline-ready pose packs. The mannequin is the visual guide; photo artwork can be added later. */
object CuratedPoseCatalog {
    private fun pose(
        id: String, name: String, category: String, pack: String, difficulty: String,
        tip: String, tags: List<String>, goals: List<String>, couple: Boolean = false
    ): PoseTemplate {
        val body = listOf(
            LandmarkTemplate(11, .42f, .26f), LandmarkTemplate(12, .58f, .26f),
            LandmarkTemplate(23, .45f, .53f), LandmarkTemplate(24, .55f, .53f),
            LandmarkTemplate(27, .42f, .9f), LandmarkTemplate(28, .58f, .9f)
        )
        val partner = if (couple) body.map { it.copy(x = it.x + .22f) } else null
        return PoseTemplate(
            id = id,
            name = name,
            category = category,
            previewImage = "",
            difficulty = difficulty,
            landmarks = body,
            landmarksPartner = partner,
            recommendationMetadata = RecommendationMetadata(
                cameraAngle = "Eye level",
                bodyOrientation = tip,
                handPlacement = "Keep hands relaxed",
                tags = (tags + goals + pack).distinct()
            ),
            isCustom = false
        )
    }

    val templates = listOf(
        pose("beach_stride", "Beach Stride", "travel", "Beach escapes", "Easy", "Walk slowly across the frame and look past the camera.", listOf("outdoor", "nature", "casual", "travel"), listOf("Taller", "Relaxed")),
        pose("beach_turn", "Sunset Turn", "travel", "Beach escapes", "Easy", "Turn shoulders toward the light; keep one foot forward.", listOf("outdoor", "nature", "serene", "travel"), listOf("Confident", "Taller")),
        pose("mirror_clean", "Clean Mirror Selfie", "selfie", "Mirror confidence", "Easy", "Hold the phone at chest height and leave space around your face.", listOf("indoor", "casual", "streetwear", "selfie"), listOf("Confident", "Hide body")),
        pose("mirror_lean", "Mirror Wall Lean", "selfie", "Mirror confidence", "Medium", "Lean one shoulder lightly into the wall; soften the opposite knee.", listOf("indoor", "streetwear", "cool", "selfie"), listOf("Taller", "Cool")),
        pose("couple_walk", "Couple Walk", "couple", "Couple date", "Easy", "Match your pace and look at each other, not the lens.", listOf("outdoor", "casual", "relaxed", "couple"), listOf("Relaxed", "Confident"), true),
        pose("couple_close", "Close Conversation", "couple", "Couple date", "Easy", "Stand close enough for shoulders to touch and lift your chins slightly.", listOf("indoor", "formal", "serene", "couple"), listOf("Confident", "Relaxed"), true),
        pose("group_stack", "Friendship Stack", "group", "Friends & groups", "Easy", "Create different heights: one person sits or bends slightly.", listOf("outdoor", "casual", "energetic", "group"), listOf("Fun", "Confident")),
        pose("group_cheer", "Group Cheer", "group", "Friends & groups", "Medium", "Raise hands at different heights and capture movement.", listOf("outdoor", "casual", "energetic", "group"), listOf("Fun", "Energetic")),
        pose("gym_lunge", "Power Lunge", "gym", "Gym strength", "Medium", "Keep your front knee over the ankle and chest tall.", listOf("gym", "active", "energetic"), listOf("Taller", "Confident")),
        pose("profile_open", "Open Profile", "profile", "Professional profile", "Easy", "Stand tall, turn 15 degrees, and lower both shoulders.", listOf("indoor", "formal", "profile"), listOf("Confident", "Taller")),
        pose("profile_seated", "Seated Leader", "profile", "Professional profile", "Easy", "Sit forward, lengthen your spine, rest an arm on your knee.", listOf("indoor", "formal", "profile"), listOf("Confident", "Hide body")),
        pose("ootd_step", "OOTD Step", "ootd", "Everyday confidence", "Easy", "Take a small step forward and point the front toe at camera.", listOf("street", "outdoor", "streetwear", "ootd"), listOf("Taller", "Cool")),
        pose("shy_soft", "Soft Side Look", "cool", "Camera-shy starters", "Easy", "Look just past the lens and cross one arm softly at your waist.", listOf("indoor", "outdoor", "casual", "relaxed"), listOf("Hide body", "Relaxed")),
        pose("shy_back", "Over-the-Shoulder", "cool", "Camera-shy starters", "Easy", "Turn three-quarters away, then look back over one shoulder.", listOf("outdoor", "street", "casual", "cool"), listOf("Hide body", "Cool"))
    )
}
