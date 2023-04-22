package com.chat.ui.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.chat.ui.BuildConfig
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

internal fun obtainSpeechToText(
    context: Context,
    language: (() -> String?) = { null }
): SpeechToText {
    return SpeechToTextImpl(context, language)
}

internal interface SpeechToText {
    val isListening: Flow<Boolean>
    fun startListening(): Flow<List<String>>
    fun stopListening()
    fun clear()
}

private class SpeechToTextImpl(
    private val context: Context,
    private val language: () -> String?
): SpeechToText {
    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)

    private val _isListeningStateFlow = MutableStateFlow(false)
    override val isListening: Flow<Boolean> = _isListeningStateFlow

    private var _resultsSharedFlow = MutableSharedFlow<List<String>>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle) {
                log("onReadyForSpeech: params=$params")
            }
            override fun onBeginningOfSpeech() {
                log("onBeginningOfSpeech")
            }
            override fun onRmsChanged(rmsdB: Float) {
//                log("onRmsChanged: rmsdB=$rmsdB")
            }
            override fun onBufferReceived(buffer: ByteArray) {
                log("onBufferReceived: buffer=$buffer")
            }
            override fun onEndOfSpeech() {
                log("onEndOfSpeech")
                _isListeningStateFlow.tryEmit(false)
            }
            override fun onError(error: Int) {
                log("onError: error=$error")
                _isListeningStateFlow.tryEmit(false)
            }
            override fun onResults(results: Bundle) {
                log("onResults: results=$results")
                val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val values = data.orEmpty().filterNotNull()
                if (!_resultsSharedFlow.tryEmit(values)) {
                    if (BuildConfig.DEBUG) {
                        throw IllegalStateException("Failed to emit speech recognition results")
                    }
                }
                _isListeningStateFlow.tryEmit(false)
            }
            override fun onPartialResults(partialResults: Bundle) {
                log("onPartialResults: results=$partialResults")
            }
            override fun onEvent(eventType: Int, params: Bundle) {
                log("onEvent: type=$eventType, params=$params")
            }

            private fun log(msg: String) {
                if (BuildConfig.DEBUG) Log.d("SpeechToTextImpl", msg)
            }
        })
    }

    override fun startListening(): Flow<List<String>> {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        val locale: Locale = language.invoke()?.let { lang -> Locale(lang) } ?: Locale.getDefault()
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
        speechRecognizer.startListening(intent)
        _isListeningStateFlow.tryEmit(true)
        return _resultsSharedFlow
    }

    override fun stopListening() {
        speechRecognizer.stopListening()
    }

    override fun clear() {
        speechRecognizer.destroy()
    }
}