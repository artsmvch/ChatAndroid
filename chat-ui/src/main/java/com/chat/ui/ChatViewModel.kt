package com.chat.ui

import android.content.Context
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.artsmvch.chat.core.Chat
import com.artsmvch.chat.core.Message
import com.chat.ui.download.MessageAttachmentsDownloader
import com.chat.ui.download.getMessageAttachmentsDownloader
import com.chat.ui.preferences.Preferences
import com.chat.ui.preferences.getPreferencesInstance
import com.chat.ui.voice.Speaker
import com.chat.ui.voice.SpeechToText
import com.chat.ui.voice.getSpeakerInstance
import com.chat.ui.voice.getSpeechToTextInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Suppress("FunctionName")
internal fun ChatViewModelFactory(
    owner: SavedStateRegistryOwner,
    context: Context,
    chatId: String?
): AbstractSavedStateViewModelFactory {
    return object : AbstractSavedStateViewModelFactory(owner, null) {
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(
                savedStateHandle = handle,
                analytics = ChatFeature.getAnalytics() ?: Analytics.None,
                preferences = getPreferencesInstance(context),
                messageAttachmentsDownloader = getMessageAttachmentsDownloader(context),
                speaker = getSpeakerInstance(context),
                speechToText = getSpeechToTextInstance(context),
                chatId = chatId
            ) as T
        }
    }
}

internal class ChatViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val analytics: Analytics,
    private val preferences: Preferences,
    private val messageAttachmentsDownloader: MessageAttachmentsDownloader,
    private val speaker: Speaker,
    private val speechToText: SpeechToText,
    private val chatId: String?
) : ViewModel(), BackPressHandler {

    private val chat by lazy {
        val chat = ChatFeature.getChat(
            chatId = savedStateHandle[SAVED_STATE_CHAT_ID] ?: chatId,
        )
        chat.addListener(chatListener)
        return@lazy chat
    }

    private val chatListener = object : Chat.Listener {
        override fun onMessageSent(message: Message) {
        }
        override fun onMessageReceived(message: Message) {
            viewModelScope.launch {
                if (!preferences.isSpeakerMuted()) {
                    val locale = preferences.getLanguage()?.let { lang -> Locale(lang) }
                    speaker.speak(message, locale)
                }
            }
        }
    }

    val messages: LiveData<List<Message>> by lazy { chat.getMessages().asLiveData() }

    private val messageCount: LiveData<Int> by lazy { messages.map { it.count() } }

    val suggestions: LiveData<List<String>> by lazy {
        messageCount.switchMap { count ->
            MutableLiveData<List<String>>(emptyList()).apply {
                viewModelScope.launch {
                    if (count == 0) {
                        delay(300L)
                        value = chat.getSuggestions()
                    }
                }
            }
        }
    }

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = OneShotLiveData<Throwable>()
    val error: LiveData<Throwable> get() = _error

    private val _clearInputFieldEvent = OneShotLiveData<Unit>()
    val clearInputFieldEvent: LiveData<Unit> get() = _clearInputFieldEvent

    private val _copyMessagesEvent = OneShotLiveData<List<Message>>()
    val copyMessagesEvent: LiveData<List<Message>> get() = _copyMessagesEvent

    private val _shareMessagesEvent = OneShotLiveData<List<Message>>()
    val shareMessagesEvent: LiveData<List<Message>> get() = _shareMessagesEvent

    private val _deleteMessagesConfirmationEvent = OneShotLiveData<Collection<Message>>()
    val deleteMessagesConfirmationEvent: LiveData<Collection<Message>>
        get() = _deleteMessagesConfirmationEvent

    private val _closeContextMenuEvent = OneShotLiveData<Unit>()
    val closeContextMenuEvent: LiveData<Unit> = _closeContextMenuEvent

    val isSpeakerMuted: LiveData<Boolean> by lazy {
        preferences.isSpeakerMutedFlow().asLiveData(Dispatchers.Main)
    }

    val isListeningToSpeech by lazy { speechToText.isListening.asLiveData(Dispatchers.Main) }

    fun onSuggestionClick(text: String) {
        proceedMessageText(text)
    }

    fun onMicrophoneButtonClick() {
        viewModelScope.launch {
            val isActuallyListening = speechToText.isListening.first()
            if (isActuallyListening) {
                speechToText.stopListening()
            } else {
                val locale = preferences.getLanguage()?.let { lang -> Locale(lang) }
                val flow = speechToText.startListening(locale)
                val tokens = flow.firstOrNull()
                val text = withContext(Dispatchers.Default) {
                    tokens?.joinToString(separator = " ")
                }
                proceedMessageText(text)
            }
        }
    }

    fun onSendMessageClick(rawText: CharSequence?) {
        proceedMessageText(rawText)
    }

    private fun proceedMessageText(rawText: CharSequence?) {
        if (rawText.isNullOrBlank()) {
            return
        }
        _clearInputFieldEvent.setValue(Unit)
        viewModelScope.launch {
            _isLoading.value = true
            val text: String = withContext(Dispatchers.Default) {
                rawText.toString().trimIndent().trim()
            }
            chat.runCatching { sendMessage(text) }.onFailure {
                analytics.onError(it)
                _error.setValue(it)
            }
            analytics.onEvent(ChatEvent.MESSAGE_SENT)
            _isLoading.value = false
        }
    }

    fun onDownloadMessageImagesClick(message: Message) {
        messageAttachmentsDownloader.downloadImages(message)
    }

    fun onCopyMessageClick(message: Message) {
        onCopyMessagesClick(setOf(message))
    }

    fun onCopyMessagesClick(messages: Collection<Message>) {
        _closeContextMenuEvent.setValue(Unit)
        viewModelScope.launch {
            val sortedMessages = withContext(Dispatchers.Default) {
                messages.sortedBy { it.timestamp }
            }
            _copyMessagesEvent.setValue(sortedMessages)
        }
    }

    fun onShareMessageClick(message: Message) {
        onShareMessagesClick(setOf(message))
    }

    fun onShareMessagesClick(messages: Collection<Message>) {
        _closeContextMenuEvent.setValue(Unit)
        viewModelScope.launch {
            val sortedMessages = withContext(Dispatchers.Default) {
                messages.sortedBy { it.timestamp }
            }
            _shareMessagesEvent.setValue(sortedMessages)
        }
    }

    fun onDeleteMessageClick(messages: Message) {
        onDeleteMessagesClick(setOf(messages))
    }

    fun onDeleteMessagesClick(messages: Collection<Message>) {
        _deleteMessagesConfirmationEvent.setValue(messages)
    }

    fun onMessageDeletionConfirmed(messages: Collection<Message>) {
        _closeContextMenuEvent.setValue(Unit)
        viewModelScope.launch {
            chat.deleteMessages(messages)
        }
    }

    fun onMessageDeletionDeclined(messages: Collection<Message>) {
        _closeContextMenuEvent.setValue(Unit)
    }

    fun onSpeakerClick() {
        viewModelScope.launch {
            val muted = preferences.isSpeakerMuted()
            val newMuted = !muted
            preferences.setSpeakerMuted(newMuted)
            if (newMuted) {
                speaker.stop()
            }
            if (newMuted) {
                analytics.onEvent(ChatEvent.SPEAKER_ENABLED)
            } else {
                analytics.onEvent(ChatEvent.SPEAKER_DISABLED)
            }
        }
    }

    fun onUiErrorOccurred(error: Throwable) {
        analytics.onUiError(error)
    }

    override fun onCleared() {
        super.onCleared()
        savedStateHandle[SAVED_STATE_CHAT_ID] = chat.chatInfo.chatId
        chat.removeListener(chatListener)
        speechToText.clear()
    }

    override fun handleBackPress(): Boolean {
        return false
    }

    companion object {
        private const val JOB_KEY = "listening_to_speech"

        private const val SAVED_STATE_CHAT_ID = "chat_id"
    }
}