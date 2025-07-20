package com.chat.app.deepkseek

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.artsmvch.chat.core.Chat
import com.artsmvch.chat.core.ImageAttachments
import com.artsmvch.chat.core.ImageInfo
import com.artsmvch.chat.core.Message
import com.chat.app.BuildConfig
import com.chat.app.R
import com.chat.ui.ChatWithLocalDb
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


internal class DeepSeekChatImpl(
    private val context: Context,
    private val chatId: String
) : ChatWithLocalDb(chatId), Chat {
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val config = DeepSeekChatConfig(context, chatScope).apply { preload() }

    private val client: OkHttpClient by lazy {
        val timeoutInSeconds = 120L
        OkHttpClient.Builder()
            .callTimeout(timeoutInSeconds, TimeUnit.SECONDS)
            .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
            .build()
    }

    private val listeners = CopyOnWriteArraySet<Chat.Listener>()

    private fun ensureDeepSeekDir(): File {
        val dir = File(context.filesDir, "deepseek")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getImageDir(): File {
        return File(ensureDeepSeekDir(), "generated_images").apply { mkdirs() }
    }

    private fun parseResponseError(response: Response): Throwable {
        val message = kotlin.runCatching {
            val body = response.body?.string()!!
            val json = JSONObject(body)
            val error = json.getJSONObject("error")
            error.getString("message")
        }.getOrNull() ?: ""
        return DeepSeekApiException(response.code, message.ifBlank { response.message })
    }

    override suspend fun sendMessage(text: String) {
        return TextMessageSender().sendMessage(text)
    }

    override suspend fun generateImage(text: String) {
        throw UnsupportedOperationException("Not supported yet!")
    }

    private fun handleError(error: Throwable) {
        if (error is DeepSeekApiException) {
            if (error.isUnauthorized()) {
                if (BuildConfig.GOOGLE_SERVICES_ENABLED) {
                    FirebaseCrashlytics.getInstance().recordException(error)
                }
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
        message.imageAttachments?.images?.also { images ->
            downloadImagesToInternalDir(images)
            // TODO: fix permission issues
            // downloadImagesToExternalDir(images)
        }
        listeners.forEach { it.onMessageReceived(message) }
    }

    private fun downloadImagesToInternalDir(images: List<ImageInfo>) {
        images.forEach { info ->
            coroutineScope.launch(Dispatchers.IO) {
                val imageUrl = info.imageUrl ?: return@launch
                val filepath = info.filepath ?: return@launch
                try {
                    val connection: HttpURLConnection = URL(imageUrl).openConnection() as HttpURLConnection
                    connection.connect()
                    val inputStream = connection.inputStream
                    val bufferedInputStream = BufferedInputStream(inputStream)
                    val bitmap = BitmapFactory.decodeStream(bufferedInputStream)
                    FileOutputStream(filepath).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                } catch (ignored: Exception) {
                }
            }
        }
    }

    private abstract inner class MessageSender {
        suspend fun sendMessage(text: String) {
            val userMsg = createMessage(role = ROLE_USER, text = text)
            val prevMessages = getMessages().first()
            return suspendCoroutine { continuation ->
                val callback = object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            kotlin.runCatching { parseMessageResponse(response.body!!) }
                                .onSuccess { receivedMsg ->
                                    // User message
                                    appendMessage(userMsg)
                                    onMessageSent(userMsg)

                                    // Received message
                                    appendMessage(receivedMsg)
                                    onMessageReceived(receivedMsg)

                                    continuation.resume(Unit)
                                }
                                .onFailure { continuation.resumeWithException(it) }
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
                val request = createMessageRequest(
                    prevMessages = prevMessages,
                    text = text
                )
                client.newCall(request).enqueue(callback)
            }
        }

        protected fun createMessage(
            role: String,
            text: String,
            imageAttachments: ImageAttachments? = null
        ): Message {
            return object : Message {
                override val id: Long = 0L
                override val chatId: String = this@DeepSeekChatImpl.chatId
                override val timestamp: Long = System.currentTimeMillis()
                override val role: String = role
                override val isFromUser: Boolean = role == ROLE_USER
                override val text: String = text
                override val imageAttachments: ImageAttachments? = imageAttachments
            }
        }

        abstract fun createMessageRequest(prevMessages: List<Message>, text: String): Request
        @Throws(Exception::class)
        abstract fun parseMessageResponse(body: ResponseBody): Message
    }

    private inner class TextMessageSender : MessageSender() {
        override fun createMessageRequest(prevMessages: List<Message>, text: String): Request {
            val messagesJsonArr = JSONArray()
            prevMessages.forEach { msg ->
                messagesJsonArr.put(
                    JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.text)
                    }
                )
            }
            messagesJsonArr.put(
                JSONObject().apply {
                    put("role", ROLE_USER)
                    put("content", text)
                }
            )

            val bodyJson = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", messagesJsonArr)
            }

            val body = bodyJson.toString().toRequestBody()
            val apiKey = config.getApiKey()
            return Request.Builder()
                .post(body)
                .url("https://api.deepseek.com/chat/completions")
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
            val role = message.getString("role")
            val content = message.getString("content")
            val text = content.trimIndent().trim()
            return createMessage(role = role, text = text)
        }
    }

    companion object {
        private const val ROLE_USER = "user"
    }
}