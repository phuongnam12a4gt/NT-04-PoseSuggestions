package com.example.posesuggestions

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TTSHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false
    private var lastSpokenText: String? = null
    private var lastSpokenTime: Long = 0

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isReady = true
        }
    }

    fun speak(text: String) {
        val currentTime = System.currentTimeMillis()
        // Prevent repetitive feedback within 3 seconds for the same text
        if (isReady && (text != lastSpokenText || currentTime - lastSpokenTime > 3000)) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            lastSpokenText = text
            lastSpokenTime = currentTime
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
