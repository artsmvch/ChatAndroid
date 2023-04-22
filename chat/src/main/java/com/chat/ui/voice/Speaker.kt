package com.chat.ui.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import com.chat.ui.Message

internal fun getSpeakerInstance(context: Context): Speaker {
    return TTSSpeakerImpl(context)
}

internal interface Speaker {
    fun speak(message: Message)
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
                pendingMessage?.also(::speak)
            }
            TextToSpeech.ERROR -> {
                Log.e(LOG_TAG, "TextToSpeech failed to initialize")
                initializationStatus = InitializationStatus.FAILED_TO_INITIALIZE
            }
        }
    }
    private val textToSpeech: TextToSpeech = TextToSpeech(context, ttsInitListener)

    private var pendingMessage: Message? = null

    override fun speak(message: Message) {
        pendingMessage = null
        if (initializationStatus == InitializationStatus.INITIALIZED) {
            textToSpeech.speak(
                message.text,
                TextToSpeech.QUEUE_FLUSH,
                Bundle(),
                message.id.toString()
            )
        } else if (initializationStatus == InitializationStatus.INITIALIZING) {
            pendingMessage = message
        }
    }

    override fun stop() {
        pendingMessage = null
        textToSpeech.stop()
    }

    private enum class InitializationStatus {
        NOT_INITIALIZED,
        INITIALIZING,
        INITIALIZED,
        FAILED_TO_INITIALIZE
    }

    companion object {
        private const val LOG_TAG = "TTSSpeakerImpl"
    }
}