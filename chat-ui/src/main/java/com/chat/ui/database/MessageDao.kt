package com.chat.ui.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class MessageDao {
    @Query("""
        SELECT m.*
        FROM Messages AS m
        WHERE timestamp = (
            SELECT MIN(timestamp)
            FROM Messages
            WHERE chat_id = m.chat_id
        )
    """)
    abstract fun getFirstMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM Messages WHERE chat_id = :chatId")
    abstract fun getMessages(chatId: String): Flow<List<MessageEntity>>

    @Insert
    abstract suspend fun insertMessage(entity: MessageEntity): Long

    @Query("DELETE FROM messages WHERE id in (:entityIds)")
    abstract suspend fun deleteMessages(entityIds: Collection<Long>)
}