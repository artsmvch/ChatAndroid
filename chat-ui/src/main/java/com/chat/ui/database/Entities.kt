package com.chat.ui.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.artsmvch.chat.core.ImageAttachments

@Entity(
    tableName = "Messages"
)
internal data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id")
    val id: Long,
    @ColumnInfo("chat_id")
    val chatId: String,
    @ColumnInfo("timestamp")
    val timestamp: Long,
    @ColumnInfo("role")
    val role: String,
    @ColumnInfo("text")
    val text: String,
    @ColumnInfo("image_attachments")
    val imageAttachments: ImageAttachments?
)
