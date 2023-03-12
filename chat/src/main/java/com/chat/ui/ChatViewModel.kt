package com.chat.ui

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ChatViewModel : ViewModel() {
    private val chat: Chat = ChatFeature.getChat()

    val messages: LiveData<List<Message>> by lazy {
        liveData { chat.getMessages().collect { emit(it) } }
    }

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = OneShotLiveData<Throwable>()
    val error: LiveData<Throwable> get() = _error

    private val _clearInputFieldEvent = OneShotLiveData<Unit>()
    val clearInputFieldEvent: LiveData<Unit> get() = _clearInputFieldEvent

    fun onSendMessage(rawText: CharSequence?) {
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
}