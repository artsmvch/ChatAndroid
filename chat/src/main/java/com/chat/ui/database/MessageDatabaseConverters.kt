package com.chat.ui.database

import androidx.room.TypeConverter
import com.chat.ui.ImageAttachments
import com.chat.ui.ImageInfo
import org.json.JSONArray
import org.json.JSONObject

internal class MessageDatabaseConverters {
    @TypeConverter
    fun toImageAttachment(value: String?): ImageAttachments? {
        value ?: return null
        val images = try {
            val jsonArray = JSONArray(value)
            val images = ArrayList<ImageInfo>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val imageInfo = ImageInfo(
                    imageUrl = jsonObject.getStringOrNull(KEY_IMAGE_URL),
                    filepath = jsonObject.getStringOrNull(KEY_FILEPATH)
                )
                images.add(imageInfo)
            }
            images
        } catch (ignored: Throwable) {
            return null
        }
        return object : ImageAttachments {
            override val images: List<ImageInfo> = images
        }
    }

    @TypeConverter
    fun fromImageAttachment(imageAttachment: ImageAttachments?): String? {
        imageAttachment ?: return null
        return try {
            val jsonArray = JSONArray()
            imageAttachment.images.forEach { info ->
                val jsonObject = JSONObject()
                jsonObject.put(KEY_IMAGE_URL, info.imageUrl)
                jsonObject.put(KEY_FILEPATH, info.filepath)
                jsonArray.put(jsonObject)
            }
            jsonArray.toString()
        } catch (ignored: Throwable) {
            null
        }
    }

    private fun JSONObject.getStringOrNull(key: String): String? {
        return if (has(key)) optString(key) else null
    }

    companion object {
        private const val KEY_IMAGE_URL = "image_url"
        private const val KEY_FILEPATH = "filepath"
    }
}