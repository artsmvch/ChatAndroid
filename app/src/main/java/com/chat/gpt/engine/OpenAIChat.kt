package com.chat.gpt.engine

import android.content.Context
import com.chat.gpt.R
import com.chat.ui.Chat
import com.chat.ui.DatabaseChat
import com.chat.ui.Message
import com.google.firebase.crashlytics.FirebaseCrashlytics
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class OpenAIChat constructor(
    private val context: Context,
    private val listeners: List<Listener>
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

    private fun createMessageRequest(message: String): Request {
        val apiKey = config.getApiKey()
        val bodyJson = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", message)
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

    private fun parseMessageResponse(body: ResponseBody?): Result<Message> {
        return body.runCatching {
            val json = JSONObject(body!!.string())
            val choices = json.getJSONArray("choices")
            val choice = choices.getJSONObject(0)
            val message = choice.getJSONObject("message")
            val content = message.getString("content")
            val text = content.trimIndent().trim()
            return@runCatching createMessage(isFromUser = false, text = text)
        }
    }

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

    private fun createMessage(isFromUser: Boolean, text: String): Message {
        return object : Message {
            override val id: Long = 0L
            override val isFromUser: Boolean = isFromUser
            override val text: String = text
            override val timestamp: Long = System.currentTimeMillis()
        }
    }

    override val descriptor: Chat.Descriptor = object : Chat.Descriptor {
        override val name: String get() = context.getString(R.string.chat_name)
    }

    override suspend fun sendMessage(text: String) {
        appendMessage(
            createMessage(isFromUser = true, text = text).also { msg ->
                listeners.forEach { it.onMessageSent(msg) }
            }
        )
        return suspendCoroutine { continuation ->
            val callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.body.let(::parseMessageResponse)
                            .onSuccess { msg ->
                                appendMessage(msg)
                                listeners.forEach { it.onMessageReceived(msg) }
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
            client.newCall(createMessageRequest(text.toString())).enqueue(callback)
        }
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

    interface Listener {
        fun onMessageSent(message: Message)
        fun onMessageReceived(message: Message)
    }
}