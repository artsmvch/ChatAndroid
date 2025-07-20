package com.artsmvch.chat.core

import kotlinx.coroutines.flow.Flow

interface Chat {
    val chatInfo: ChatInfo
    fun getMessages(): Flow<List<Message>>

    suspend fun getSuggestions(): List<String>
    suspend fun generateImage(text: String)
    suspend fun sendMessage(text: String)
    suspend fun deleteMessages(messages: Collection<Message>)

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    interface Listener {
        fun onMessageSent(message: Message)
        fun onMessageReceived(message: Message)
    }
}