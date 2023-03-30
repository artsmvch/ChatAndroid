package com.chat.ui

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import kotlinx.coroutines.flow.Flow

interface Chat {
    fun getMessages(): Flow<List<Message>>
    fun getMessageListLiveData(): LiveData<PagedList<Message>>
    suspend fun deleteMessage(message: Message)
    suspend fun sendMessage(text: String)
}