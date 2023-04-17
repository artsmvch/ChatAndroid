package com.chat.ui

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import kotlinx.coroutines.flow.Flow

interface Chat {
    val descriptor: Descriptor
    fun getMessages(): Flow<List<Message>>
    fun getMessageListLiveData(): LiveData<PagedList<Message>>
    suspend fun deleteMessages(messages: Collection<Message>)
    suspend fun sendMessage(text: String)

    interface Descriptor {
        val name: String
    }
}