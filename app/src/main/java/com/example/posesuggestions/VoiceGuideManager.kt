package com.example.posesuggestions

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceGuideManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false
    
    private var lastSpokenText: String? = null
    private var lastSpokenTime: Long = 0
    private val MIN_GAP_MILLIS = 3500L // Don't speak too often

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isReady = true
        }
    }

    fun speak(text: String, force: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        
        // Check for repetitiveness and frequency
        if (isReady && (force || text != lastSpokenText || currentTime - lastSpokenTime > MIN_GAP_MILLIS)) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            lastSpokenText = text
            lastSpokenTime = currentTime
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
