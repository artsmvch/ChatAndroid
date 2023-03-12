package com.chat.ui

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.atomic.AtomicLong

// ConflatedBroadcastChannel
abstract class DatabaseChat(
    private val context: Context,
    private val key: String
) : Chat {
    protected val chatScope: CoroutineScope get() = GlobalScope
    private val database: MessageDatabase by lazy { getMessageDatabase(context, key) }
    private val messageId by lazy {
        // TODO: make it non-blocking
        val lastId = runBlocking { database.getLastMessageId() }
        AtomicLong(lastId)
    }

    final override fun getMessages(): Flow<List<Message>> = database.queryMessages()

    protected fun generateMessageId(): Long = messageId.incrementAndGet()

    protected fun appendMessage(message: Message) {
        chatScope.launch {
            database.insertMessage(message)
        }
    }
}