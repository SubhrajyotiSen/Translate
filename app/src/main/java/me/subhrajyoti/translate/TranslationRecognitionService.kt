package me.subhrajyoti.translate

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

class TranslationRecognitionService : RecognitionService() {
    override fun onStartListening(intent: Intent?, listener: Callback?) {
        // No-op
    }

    override fun onStopListening(listener: Callback?) {
        // No-op
    }

    override fun onCancel(listener: Callback?) {
        // No-op
    }
}
