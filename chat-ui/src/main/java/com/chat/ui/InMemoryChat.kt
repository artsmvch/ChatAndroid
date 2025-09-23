package com.chat.ui

import com.artsmvch.chat.core.Chat
import com.artsmvch.chat.core.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class InMemoryChat : Chat {
    protected val chatScope: CoroutineScope get() = GlobalScope
    // ConflatedBroadcastChannel
    private val messages = MutableStateFlow<List<Message>>(emptyList())

    protected fun appendMessage(message: Message) {
        chatScope.launch(Dispatchers.Default) {
            synchronized(this@InMemoryChat) {
                val list = messages.value.toMutableList()
                list.add(message)
                messages.tryEmit(list)
            }
        }
    }
}