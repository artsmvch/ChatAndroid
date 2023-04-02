package com.chat.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.chat.ui.database.MessageDatabase
import com.chat.ui.database.obtainMessageDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

// ConflatedBroadcastChannel
abstract class DatabaseChat(
    private val context: Context,
    private val key: String
) : Chat {
    protected val chatScope: CoroutineScope get() = GlobalScope
    private val database: MessageDatabase by lazy { obtainMessageDatabase(context, key) }

    final override fun getMessages(): Flow<List<Message>> {
        return database.queryMessages()
    }

    final override fun getMessageListLiveData(): LiveData<PagedList<Message>> {
        return database.getMessageListLiveData()
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