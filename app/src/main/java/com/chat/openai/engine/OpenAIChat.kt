package com.chat.openai.engine

import android.content.Context
import com.chat.firebase.FirebaseRemoteConfigCache
import com.chat.openai.BuildConfig
import com.chat.ui.Chat
import com.chat.ui.DatabaseChat
import com.chat.ui.Message
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class OpenAIChat(context: Context) : DatabaseChat(context, "openai"), Chat {
    private val openAIApiKeyRef = AtomicReference<String>(BuildConfig.OPEN_AI_API_KEY)

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    init {
        initAsync()
    }

    private fun createMessageRequest(message: String): Request {
        val apiKey = openAIApiKeyRef.get()
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
        return try {
            val messageFromBody: String? = response.body?.string()?.let { string ->
                val json = JSONObject(string)
                json.getJSONObject("error").getString("message")
            }
            val message = (messageFromBody ?: "").ifBlank { response.message }
            Exception(message)
        } catch (e: Throwable) {
            IllegalStateException("Failed to parse error", e)
        }
    }

    private fun createMessage(isFromUser: Boolean, text: String): Message {
        return object : Message {
            override val id: Long = 0L
            override val isFromUser: Boolean = isFromUser
            override val text: String = text
            override val timestamp: Long = System.currentTimeMillis()
        }
    }

    override suspend fun sendMessage(text: String) {
        appendMessage(
            createMessage(isFromUser = true, text = text)
        )
        return suspendCoroutine { continuation ->
            val callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.body.let(::parseMessageResponse)
                            .onSuccess {
                                appendMessage(it)
                                continuation.resume(Unit)
                            }
                            .onFailure {
                                continuation.resumeWithException(it)
                            }
                    } else {
                        val error = parseResponseError(response)
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

    private fun initAsync() {
        chatScope.launch {
            FirebaseRemoteConfigCache.getString("openai_config").collectLatest { raw ->
                kotlin.runCatching {
                    val openAIConfig = JSONObject(raw!!)
                    val apiKey = openAIConfig.getString("api_key")
                    if (!apiKey.isNullOrBlank()) {
                        openAIApiKeyRef.set(apiKey)
                    }
                }
            }
        }
    }
}