package com.chat.gpt.engine

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.chat.gpt.R
import com.chat.ui.ImageAttachments
import com.chat.ui.Chat
import com.chat.ui.DatabaseChat
import com.chat.ui.Message
import com.google.firebase.crashlytics.FirebaseCrashlytics
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class OpenAIChat constructor(
    private val context: Context,
) : DatabaseChat(context, "openai"), Chat {
    private val config = OpenAIChatConfig(context, chatScope).apply { preload() }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val listeners = CopyOnWriteArraySet<Chat.Listener>()

    private fun parseResponseError(response: Response): Throwable {
        val apiError = when (response.code) {
            401 -> ApiError.UNAUTHORIZED
            else -> ApiError.UNKNOWN
        }
        val message = kotlin.runCatching {
            val body = response.body?.string()!!
            val json = JSONObject(body)
            val error = json.getJSONObject("error")
            error.getString("message")
        }.getOrNull() ?: ""
        return OpenAIApiException(apiError, message.ifBlank { response.message })
    }

    override val descriptor: Chat.Descriptor = object : Chat.Descriptor {
        override val name: String get() = ""//context.getString(R.string.chat_name)
    }

    override suspend fun sendMessage(text: String) {
        return TextMessageSender().sendMessage(text)
    }

    override suspend fun generateImage(text: String) {
        return ImageGenerator().sendMessage(text)
    }

    private fun handleError(error: Throwable) {
        if (error is OpenAIApiException) {
            if (error.apiError == ApiError.UNAUTHORIZED) {
                FirebaseCrashlytics.getInstance().recordException(error)
                config.reload()
            }
        }
    }

    override suspend fun getSuggestions(): List<String> {
        return try {
            context.resources.getStringArray(R.array.chat_suggestions).toList()
        } catch (e: Throwable) {
            emptyList<String>()
        }
    }

    override fun addListener(listener: Chat.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: Chat.Listener) {
        listeners.remove(listener)
    }

    private fun onMessageSent(message: Message) {
        listeners.forEach { it.onMessageSent(message) }
    }

    private fun onMessageReceived(message: Message) {
        message.imageAttachments?.imageUrls?.also { urls -> downloadImages(urls) }
        listeners.forEach { it.onMessageReceived(message) }
    }

    private fun downloadImages(urls: List<String>) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return
        urls.forEach { url ->
            val folderName = "chatty_ai_bot"
            val fileName = "image-${System.currentTimeMillis()}.png"
            val subPath = File.separator + folderName + File.separator + fileName
            val request = DownloadManager.Request(Uri.parse(url))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, subPath
                )
                .apply { allowScanningByMediaScanner() }
                .setTitle(fileName)
                .setDescription(context.getString(com.chat.ui.R.string.image_download_description))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            downloadManager.enqueue(request)
        }
    }

    private abstract inner class MessageSender {
        suspend fun sendMessage(text: String) {
            appendMessage(
                createMessage(isFromUser = true, text = text).also { msg ->
                    onMessageSent(msg)
                }
            )
            return suspendCoroutine { continuation ->
                val callback = object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            kotlin.runCatching { parseMessageResponse(response.body!!) }
                                .onSuccess { msg ->
                                    appendMessage(msg)
                                    onMessageReceived(msg)
                                    continuation.resume(Unit)
                                }
                                .onFailure {
                                    continuation.resumeWithException(it)
                                }
                        } else {
                            val error = parseResponseError(response)
                            handleError(error)
                            continuation.resumeWithException(error)
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }
                }
                client.newCall(createMessageRequest(text)).enqueue(callback)
            }
        }

        protected fun createMessage(
            isFromUser: Boolean,
            text: String,
            imageAttachments: ImageAttachments? = null
        ): Message {
            return object : Message {
                override val id: Long = 0L
                override val timestamp: Long = System.currentTimeMillis()
                override val isFromUser: Boolean = isFromUser
                override val text: String = text
                override val imageAttachments: ImageAttachments? = imageAttachments
            }
        }

        abstract fun createMessageRequest(text: String): Request
        @Throws(Exception::class)
        abstract fun parseMessageResponse(body: ResponseBody): Message
    }

    private inner class TextMessageSender : MessageSender() {
        override fun createMessageRequest(text: String): Request {
            val apiKey = config.getApiKey()
            val bodyJson = JSONObject().apply {
                put("model", "gpt-3.5-turbo")
                put("messages",
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("role", "user")
                                put("content", text)
                            }
                        )
                    }
                )
            }
            val body = bodyJson.toString().toRequestBody()
            return Request.Builder()
                .post(body)
                .url("https://api.openai.com/v1/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .build()
        }

        @Throws(Exception::class)
        override fun parseMessageResponse(body: ResponseBody): Message {
            val json = JSONObject(body.string())
            val choices = json.getJSONArray("choices")
            val choice = choices.getJSONObject(0)
            val message = choice.getJSONObject("message")
            val content = message.getString("content")
            val text = content.trimIndent().trim()
            return createMessage(isFromUser = false, text = text)
        }

    }

    private inner class ImageGenerator : MessageSender() {
        override fun createMessageRequest(text: String): Request {
            val apiKey = config.getApiKey()
            val bodyJson = JSONObject().apply {
                put("prompt", text)
                put("n", 1)
                put("size", "1024x1024")
            }
            val body = bodyJson.toString().toRequestBody()
            return Request.Builder()
                .post(body)
                .url("https://api.openai.com/v1/images/generations")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .build()
        }

        @Throws(Exception::class)
        override fun parseMessageResponse(body: ResponseBody): Message {
            val json = JSONObject(body.string())
            val data = json.getJSONArray("data")
            val imageUrls = ArrayList<String>(1)
            for (i in 0 until data.length()) {
                val url = data.optJSONObject(i)?.optString("url")
                if (!url.isNullOrBlank()) {
                    imageUrls.add(url)
                }
            }
            val imageAttachments = object : ImageAttachments {
                override val imageUrls: List<String> = imageUrls
            }
            return createMessage(isFromUser = false, text = "", imageAttachments = imageAttachments)
        }
    }
}