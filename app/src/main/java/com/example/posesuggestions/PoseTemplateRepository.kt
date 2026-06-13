package com.example.posesuggestions

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

class PoseTemplateRepository(private val context: Context) {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
    }

    private val prefs = context.getSharedPreferences("pose_prefs", Context.MODE_PRIVATE)

    fun loadTemplates(): List<PoseTemplate> {
        val assetTemplates = try {
            val inputStream = context.assets.open("pose_templates.json")
            val reader = InputStreamReader(inputStream)
            val config = json.decodeFromString<PoseTemplatesConfig>(reader.readText())
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
            val matchesQuery = template.name.contains(query, ignoreCase = true)
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
