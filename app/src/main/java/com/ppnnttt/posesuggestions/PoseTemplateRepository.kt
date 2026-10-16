package com.ppnnttt.posesuggestions

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

class PoseTemplateRepository(private val context: Context) {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
    }

    private val prefs = context.getSharedPreferences("pose_prefs", Context.MODE_PRIVATE)

    fun loadTemplates(): List<PoseTemplate> {
        val assetTemplates = try {
            val config = context.assets.open("pose_templates.json").bufferedReader().use {
                json.decodeFromString<PoseTemplatesConfig>(it.readText())
            }
            config.templates
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }

        val customTemplates = loadCustomTemplates()
        return assetTemplates + customTemplates
    }

    private fun loadCustomTemplates(): List<PoseTemplate> {
        val customStr = prefs.getString("custom_templates", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<PoseTemplate>>(customStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCustomTemplate(template: PoseTemplate) {
        val customTemplates = loadCustomTemplates().toMutableList()
        customTemplates.add(template)
        prefs.edit().putString("custom_templates", json.encodeToString(customTemplates)).apply()
    }

    fun getTemplates(
        query: String = "",
        category: String = "All",
        difficulty: String = "All"
    ): List<PoseTemplate> {
        return loadTemplates().filter { template ->
            fun normalized(text: String) = java.text.Normalizer.normalize(text.lowercase(java.util.Locale.ROOT).replace('đ', 'd'), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
            val categoryName = mapOf("cool" to R.string.cool, "selfie" to R.string.selfie, "travel" to R.string.travel, "gym" to R.string.gym)[template.category]?.let { context.getString(it) }.orEmpty()
            val matchesQuery = normalized("${template.name} ${template.category} $categoryName").contains(normalized(query.trim()))
            val matchesCategory = category == "All" || template.category.equals(category, ignoreCase = true)
            val matchesDifficulty = difficulty == "All" || template.difficulty.equals(difficulty, ignoreCase = true)
            matchesQuery && matchesCategory && matchesDifficulty
        }
    }

    // Favorites
    fun toggleFavorite(templateId: String) {
        val favorites = getFavorites().toMutableSet()
        if (favorites.contains(templateId)) {
            favorites.remove(templateId)
        } else {
            favorites.add(templateId)
        }
        prefs.edit().putStringSet("favorites", favorites).apply()
    }

    fun getFavorites(): Set<String> {
        return prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    }

    // Recently Used
    fun addToRecent(templateId: String) {
        val recent = getRecent().toMutableList()
        recent.remove(templateId)
        recent.add(0, templateId)
        if (recent.size > 10) {
            recent.removeAt(recent.size - 1)
        }
        prefs.edit().putString("recent", recent.joinToString(",")).apply()
    }

    fun getRecent(): List<String> {
        val recentStr = prefs.getString("recent", "") ?: ""
        return if (recentStr.isEmpty()) emptyList() else recentStr.split(",")
    }
}
