package com.chat.ui

import kotlinx.coroutines.flow.Flow

interface Chat {
    fun getMessages(): Flow<List<Message>>
    suspend fun sendMessage(text: CharSequence)
}