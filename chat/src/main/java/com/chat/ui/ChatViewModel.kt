package com.chat.ui

import androidx.lifecycle.*
import androidx.paging.PagedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ChatViewModel : ViewModel() {
    private val chat: Chat = ChatFeature.getChat()

//    val messages: LiveData<List<Message>> by lazy {
//        liveData { chat.getMessages().collect { emit(it) } }
//    }

    val messagePagedList: LiveData<PagedList<Message>> by lazy {
        chat.getMessageListLiveData()
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

    fun onSendMessageClick(rawText: CharSequence?) {
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
}