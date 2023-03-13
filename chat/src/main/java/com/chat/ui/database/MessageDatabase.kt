package com.chat.ui.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chat.ui.Message
import kotlinx.coroutines.flow.Flow

internal fun obtainMessageDatabase(context: Context, key: String): MessageDatabase {
    return MessageDatabaseImpl(context, key)
}

internal interface MessageDatabase {
    fun queryMessages(): Flow<List<Message>>
    fun getMessageListLiveData(): LiveData<PagedList<Message>>
    suspend fun insertMessage(message: Message): Long
}

private class MessageDatabaseImpl(
    private val context: Context,
    private val key: String
): MessageDatabase {
    private val database: MessageRoomDatabase by lazy {
        Room.databaseBuilder(context, MessageRoomDatabase::class.java, "chat.$key.messages")
            .setJournalMode(RoomDatabase.JournalMode.AUTOMATIC)
            .build()
    }

    override fun queryMessages(): Flow<List<Message>> {
        return database.getMessageDao().queryMessages()
    }

    override fun getMessageListLiveData(): LiveData<PagedList<Message>> {
        val dataSourceFactory =
            database.getMessageDao().getMessageDataSource()
                .map { value -> value as Message }
        val builder = LivePagedListBuilder(
            dataSourceFactory,
            PagedList.Config.Builder()
                .setPageSize(20)
                .setEnablePlaceholders(true)
                .build()
        )
        return builder
            .build()
    }

    override suspend fun insertMessage(message: Message): Long {
        val entity = MessageEntity(
            id = 0L,
            isFromUser = message.isFromUser,
            text = message.text
        )
        return database.getMessageDao().insertMessage(entity)
    }
}

