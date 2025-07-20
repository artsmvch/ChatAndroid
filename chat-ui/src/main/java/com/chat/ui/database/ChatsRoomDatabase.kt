package com.chat.ui.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    version = 1,
    exportSchema = true,
    entities = [MessageEntity::class]
)
@TypeConverters(Converters::class)
internal abstract class ChatsRoomDatabase : RoomDatabase() {
    abstract fun getMessageDao(): MessageDao
}