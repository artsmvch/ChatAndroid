package com.chat.ui

import androidx.lifecycle.LiveData
import androidx.paging.PagedList

interface Chat {
    val descriptor: Descriptor
    fun getMessageListLiveData(): LiveData<PagedList<Message>>
    suspend fun deleteMessages(messages: Collection<Message>)
    suspend fun sendMessage(text: String)
    suspend fun getSuggestions(): List<String>

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    interface Descriptor {
        val name: String
    }

    interface Listener {
        fun onMessageSent(message: Message)
        fun onMessageReceived(message: Message)
    }
}