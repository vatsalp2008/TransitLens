package com.vatsalp.transitlens.guidance

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Text-to-speech guidance. Every haptic cue has a spoken equivalent (ADR-003). */
@Singleton
class AudioEngine @Inject constructor(
    @ApplicationContext context: Context,
) : TextToSpeech.OnInitListener {

    @Volatile
    private var ready = false
    private val tts = TextToSpeech(context, this)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            ready = true
        }
    }

    fun speak(text: String) {
        if (ready) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
