package com.chat.ui

import com.artsmvch.chat.core.Chat
import com.artsmvch.chat.core.ChatInfo
import com.artsmvch.chat.core.Message
import com.chat.ui.database.ChatsDatabase
import com.chat.ui.database.getChatsDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// ConflatedBroadcastChannel
abstract class ChatWithLocalDb(
    private val chatId: String
) : Chat {
    protected val chatScope: CoroutineScope get() = GlobalScope
    private val database: ChatsDatabase by lazy { getChatsDatabase() }

    override val chatInfo = ChatInfo(
        chatId = chatId,
        timeMillis = System.currentTimeMillis(),
        messagePreview = ""
    )

    override fun getMessages(): Flow<List<Message>> {
        return database.getMessages(chatId)
    }

    final override suspend fun deleteMessages(messages: Collection<Message>) {
        database.deleteMessages(messages)
    }

    protected fun appendMessage(message: Message) {
        chatScope.launch {
            database.insertMessage(message)
        }
    }
}