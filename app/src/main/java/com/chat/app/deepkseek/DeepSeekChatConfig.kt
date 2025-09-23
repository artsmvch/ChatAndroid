package com.chat.app.deepkseek

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.chat.app.BuildConfig
import com.chat.firebase.FirebaseRemoteConfigCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal class DeepSeekChatConfig(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

//    private val _apiKeyStateFlow = MutableStateFlow<String>(DEFAULT_API_KEY)
//    val apiKeyStateFlow: StateFlow<String> by lazy {
//        _apiKeyStateFlow.apply { reload() }
//    }

    private val apiKeyRef: AtomicReference<String> by lazy {
        val defaultApiKey = BuildConfig.DEEPSEEK_API_KEY
        val storedApiKey = prefs.getString(KEY_API_KEY, defaultApiKey)
        AtomicReference<String>(storedApiKey)
    }
    private val reloadAttemptsLeft = AtomicLong(2)

    fun getApiKey(): String {
        return apiKeyRef.get()
    }

    fun preload() {
        reload()
    }

    fun reload() {
        if (reloadAttemptsLeft.getAndDecrement() <= 0) {
            return
        }
        coroutineScope.launch {
            FirebaseRemoteConfigCache.getString("deepseek_config").collectLatest { raw ->
                kotlin.runCatching {
                    val openAIConfig = JSONObject(raw!!)
                    val apiKey = openAIConfig.getString("api_key")
                    if (!apiKey.isNullOrBlank()) {
                        apiKeyRef.set(apiKey)
                        prefs.edit { putString(KEY_API_KEY, apiKey) }
                    }
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "com.chat.deepseek.config"

        private const val KEY_API_KEY = "api_key"
    }
}