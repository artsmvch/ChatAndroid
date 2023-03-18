package com.chat.ui

import android.content.Context
import android.content.Intent
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

    final override suspend fun deleteMessage(message: Message) {
        database.deleteMessage(message)
    }

    override suspend fun shareMessage(message: Message) = with(Dispatchers.Main) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        // intent.putExtra(Intent.EXTRA_SUBJECT, key)
        intent.putExtra(Intent.EXTRA_TEXT, message.text)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val chooserIntent = Intent.createChooser(intent, context.getString(R.string.share_message)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    protected fun appendMessage(message: Message) {
        chatScope.launch {
            database.insertMessage(message)
        }
    }
}