package com.chat.ui.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import com.chat.ui.Message
import java.util.Locale

internal fun getSpeakerInstance(context: Context): Speaker {
    return TTSSpeakerImpl(context)
}

internal interface Speaker {
    fun speak(message: Message, locale: Locale? = null)
    fun stop()
}

private class TTSSpeakerImpl(
    private val context: Context
) : Speaker {
    private var initializationStatus: InitializationStatus = InitializationStatus.NOT_INITIALIZED
    private val ttsInitListener = TextToSpeech.OnInitListener { status ->
        when (status) {
            TextToSpeech.SUCCESS -> {
                Log.d(LOG_TAG, "TextToSpeech initialized successfully")
                initializationStatus = InitializationStatus.INITIALIZED
                pendingMessageWithLocale?.also { speak(it.message, it.locale) }
            }
            TextToSpeech.ERROR -> {
                Log.e(LOG_TAG, "TextToSpeech failed to initialize")
                initializationStatus = InitializationStatus.FAILED_TO_INITIALIZE
            }
        }
    }
    private val textToSpeech: TextToSpeech = TextToSpeech(context, ttsInitListener)

    private var pendingMessageWithLocale: MessageWithLocale? = null

    override fun speak(message: Message, locale: Locale?) {
        pendingMessageWithLocale = null
        if (initializationStatus == InitializationStatus.INITIALIZED) {
            if (locale != null && textToSpeech.supports(locale)) {
                textToSpeech.language = locale
            }
            textToSpeech.speak(
                message.text,
                TextToSpeech.QUEUE_FLUSH,
                Bundle(),
                message.id.toString()
            )
        } else if (initializationStatus == InitializationStatus.INITIALIZING) {
            pendingMessageWithLocale = MessageWithLocale(message, locale)
        }
    }

    private fun TextToSpeech.supports(locale: Locale): Boolean {
        return this.isLanguageAvailable(locale).let { code ->
            code == TextToSpeech.LANG_AVAILABLE ||
                    code == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    code == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        }
    }

    override fun stop() {
        pendingMessageWithLocale = null
        textToSpeech.stop()
    }

    private enum class InitializationStatus {
        NOT_INITIALIZED,
        INITIALIZING,
        INITIALIZED,
        FAILED_TO_INITIALIZE
    }

    private class MessageWithLocale(
        val message: Message,
        val locale: Locale?
    )

    companion object {
        private const val LOG_TAG = "TTSSpeakerImpl"
    }
}