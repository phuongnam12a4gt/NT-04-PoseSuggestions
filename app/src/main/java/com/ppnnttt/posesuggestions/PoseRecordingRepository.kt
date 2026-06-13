package com.ppnnttt.posesuggestions

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

class PoseRecordingRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val recordingDir = File(context.filesDir, "recordings").apply { mkdirs() }

    fun saveRecording(recording: PoseRecording) {
        val file = File(recordingDir, "${recording.id}.json")
        file.writeText(json.encodeToString(recording))
    }

    fun loadRecordings(): List<PoseRecording> {
        return recordingDir.listFiles()?.mapNotNull { file ->
            try {
                json.decodeFromString<PoseRecording>(file.readText())
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }

    fun deleteRecording(id: String) {
        File(recordingDir, "$id.json").delete()
    }
}
