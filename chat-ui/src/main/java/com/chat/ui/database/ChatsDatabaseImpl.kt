package com.chat.ui.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.artsmvch.chat.core.ChatInfo
import com.artsmvch.chat.core.ImageAttachments
import com.artsmvch.chat.core.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext


internal class ChatsDatabaseImpl(
    private val context: Context,
): ChatsDatabase {
    private val database: ChatsRoomDatabase by lazy {
        Room.databaseBuilder(context, ChatsRoomDatabase::class.java, "Chat.db")
            .setJournalMode(RoomDatabase.JournalMode.AUTOMATIC)
            .fallbackToDestructiveMigration()
//            .enableMultiInstanceInvalidation()
            .build()
    }

    override fun getChatInfoList(): Flow<List<ChatInfo>> {
        return database.getMessageDao()
            .getFirstMessages()
            .map { messages ->
                withContext(Dispatchers.Default) {
                    messages.map { msg ->
                        ChatInfo(
                            chatId = msg.chatId,
                            timeMillis = msg.timestamp,
                            messagePreview = msg.text
                        )
                    }
                }
            }
    }

    override fun getMessages(chatId: String): Flow<List<Message>> {
        return database.getMessageDao()
            .getMessages(chatId)
            .map { entities ->
                withContext(Dispatchers.Default) {
                    entities.map { MessageImpl(it) }
                }
            }
    }

    override suspend fun insertMessage(message: Message): Long {
        val entity = MessageEntity(
            id = 0L,
            chatId = message.chatId,
            role = message.role,
            text = message.text,
            timestamp = message.timestamp,
            imageAttachments = message.imageAttachments
        )
        return database.getMessageDao().insertMessage(entity)
    }

    override suspend fun deleteMessages(messages: Collection<Message>) {
        val entityIds = withContext(Dispatchers.Default) {
            messages.map { it.id }
        }
        database.getMessageDao().deleteMessages(entityIds)
    }

    private class MessageImpl(entity: MessageEntity): Message {
        override val id: Long = entity.id
        override val chatId: String = entity.chatId
        override val timestamp: Long = entity.timestamp
        override val role: String = entity.role
        override val isFromUser: Boolean = entity.role == "user"
        override val text: String = entity.text
        override val imageAttachments: ImageAttachments? = entity.imageAttachments
    }
}
