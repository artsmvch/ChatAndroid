package com.chat.ui.database

import com.artsmvch.chat.core.ChatInfo
import com.artsmvch.chat.core.Message
import com.chat.ui.ContextHolder
import kotlinx.coroutines.flow.Flow

private val chatsDatabaseInstance by lazy {
    ChatsDatabaseImpl(ContextHolder.appContext)
}

internal fun getChatsDatabase(): ChatsDatabase = chatsDatabaseInstance

internal interface ChatsDatabase {
    fun getChatInfoList(): Flow<List<ChatInfo>>
    fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun insertMessage(message: Message): Long
    suspend fun deleteMessages(messages: Collection<Message>)
}