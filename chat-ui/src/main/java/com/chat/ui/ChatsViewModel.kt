package com.chat.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.artsmvch.chat.core.ChatInfo
import com.chat.ui.database.ChatsDatabase
import com.chat.ui.database.getChatsDatabase

@Suppress("UNCHECKED_CAST")
internal fun ChatsViewModelFactory(context: Context): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatsViewModel(getChatsDatabase()) as T
        }
    }
}

internal class ChatsViewModel(
    private val database: ChatsDatabase
) : ViewModel() {
    val chatInfoList: LiveData<List<ChatInfo>> by lazy {
        database.getChatInfoList().asLiveData()
    }
}