package com.ppnnttt.posesuggestions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun localizedOptionLabel(value: String): String {
    val resource = when (value.lowercase()) {
        "all" -> R.string.all
        "easy" -> R.string.easy
        "medium" -> R.string.medium
        "hard" -> R.string.hard
        "cool" -> R.string.cool
        "selfie" -> R.string.selfie
        "travel" -> R.string.travel
        "gym" -> R.string.gym
        "outdoor" -> R.string.outdoor
        "indoor" -> R.string.indoor
        "nature" -> R.string.nature
        "street" -> R.string.street
        "casual" -> R.string.casual
        "active" -> R.string.active
        "formal" -> R.string.formal
        "streetwear" -> R.string.streetwear
        "male" -> R.string.male
        "female" -> R.string.female
        "non-binary" -> R.string.non_binary
        "confident" -> R.string.confident
        "relaxed" -> R.string.relaxed
        "energetic" -> R.string.energetic
        "serene" -> R.string.serene
        else -> return value
    }
    return stringResource(resource)
}
