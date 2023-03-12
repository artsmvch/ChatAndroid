package com.chat.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

abstract class AbstractChat : Chat {
    // ConflatedBroadcastChannel
    private val messages = MutableStateFlow<List<Message>>(emptyList())

    final override fun getMessages(): Flow<List<Message>> = messages

    protected fun appendMessage(message: Message) {
        val list = messages.value.toMutableList()
        list.add(message)
        messages.tryEmit(list)
    }
}