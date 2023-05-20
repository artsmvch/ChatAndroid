package com.chat.ui.database

import androidx.room.TypeConverter
import com.chat.ui.ImageAttachments

internal class MessageDatabaseConverters {
    @TypeConverter
    fun toImageAttachment(value: String?): ImageAttachments? {
        value ?: return null
        return value.split(',').let { imageUrls ->
            object : ImageAttachments {
                override val imageUrls: List<String> = imageUrls
            }
        }
    }

    @TypeConverter
    fun fromImageAttachment(imageAttachment: ImageAttachments?): String? {
        imageAttachment ?: return null
        return imageAttachment.imageUrls.joinToString(",")
    }
}