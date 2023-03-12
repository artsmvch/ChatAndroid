package com.chat.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class InMemoryChat : Chat {
    protected val chatScope: CoroutineScope get() = GlobalScope
    // ConflatedBroadcastChannel
    private val messages = MutableStateFlow<List<Message>>(emptyList())

    final override fun getMessages(): Flow<List<Message>> = messages

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