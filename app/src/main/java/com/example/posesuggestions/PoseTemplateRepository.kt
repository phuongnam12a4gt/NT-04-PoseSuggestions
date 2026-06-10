package com.example.posesuggestions

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

class PoseTemplateRepository(private val context: Context) {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
    }

    fun loadTemplates(): List<PoseTemplate> {
        return try {
            val inputStream = context.assets.open("pose_templates.json")
            val reader = InputStreamReader(inputStream)
            val config = json.decodeFromString<PoseTemplatesConfig>(reader.readText())
            config.templates
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getTemplatesByCategory(category: String): List<PoseTemplate> {
        return loadTemplates().filter { it.category.equals(category, ignoreCase = true) }
    }
}
