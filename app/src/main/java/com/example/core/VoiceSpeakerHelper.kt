package com.example.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceSpeakerHelper(context: Context) {
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun speak(text: String, language: AppLanguage) {
        val engine = tts ?: return
        if (!isReady || text.isBlank()) return

        val locale = when (language) {
            AppLanguage.ENGLISH -> Locale.forLanguageTag("en-IN")
            AppLanguage.KANNADA -> Locale.forLanguageTag("kn-IN")
            AppLanguage.HINDI -> Locale.forLanguageTag("hi-IN")
            AppLanguage.MARATHI -> Locale.forLanguageTag("mr-IN")
        }
        val langResult = engine.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.language = Locale.forLanguageTag("en-IN")
        }
        engine.setSpeechRate(0.95f)
        _isSpeaking.value = true
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "agrimitra_advice")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
