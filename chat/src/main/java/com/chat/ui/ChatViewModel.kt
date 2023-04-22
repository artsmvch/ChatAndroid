package com.chat.ui

import android.content.Context
import androidx.lifecycle.*
import androidx.paging.PagedList
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
internal fun ChatViewModelFactory(context: Context): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(
                chat = ChatFeature.getChat(),
                preferences = getPreferencesInstance(context),
                speaker = getSpeakerInstance(context),
                speechToText = getSpeechToTextInstance(context)
            ) as T
        }
    }
}

internal class ChatViewModel(
    private val chat: Chat,
    private val preferences: Preferences,
    private val speaker: Speaker,
    private val speechToText: SpeechToText
) : ViewModel() {

//    val messages: LiveData<List<Message>> by lazy {
//        liveData { chat.getMessages().collect { emit(it) } }
//    }

    private val chatListener = object : Chat.Listener {
        override fun onMessageSent(message: Message) {
        }
        override fun onMessageReceived(message: Message) {
            viewModelScope.launch {
                if (!preferences.isSpeakerMuted()) {
                    speaker.speak(message)
                }
            }
        }
    }

    val messagePagedList: LiveData<PagedList<Message>> by lazy {
        chat.getMessageListLiveData()
    }

    private val messageCount: LiveData<Int> by lazy {
        Transformations.switchMap(messagePagedList) { list ->
            PagedListSizeLiveData(list)
        }
    }

    val suggestions: LiveData<List<String>> by lazy {
        Transformations.switchMap(messageCount) { count ->
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

    init {
        chat.addListener(chatListener)
    }

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
            chat.runCatching { sendMessage(text) }
                .onFailure { _error.setValue(it) }
            _isLoading.value = false
        }
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
        }
    }

    override fun onCleared() {
        super.onCleared()
        chat.removeListener(chatListener)
        speechToText.clear()
    }

    companion object {
        private const val JOB_KEY = "listening_to_speech"
    }
}