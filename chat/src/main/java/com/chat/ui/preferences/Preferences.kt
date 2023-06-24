package com.chat.ui.preferences

import android.content.Context
import android.content.coroutines.edit
import android.content.coroutines.getBooleanValueFlow
import android.content.coroutines.getStringValueFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull

internal fun getPreferencesInstance(context: Context): Preferences {
    return PreferencesImpl(context)
}

internal interface Preferences {
    fun isOnboardingNeededFlow(): Flow<Boolean>
    suspend fun setOnboardingCompleted()

    fun isSpeakerMutedFlow(): Flow<Boolean>
    suspend fun isSpeakerMuted(): Boolean = isSpeakerMutedFlow().first()
    suspend fun setSpeakerMuted(muted: Boolean)

    fun getLanguageFlow(): Flow<String?>
    suspend fun getLanguage(): String? = getLanguageFlow().first()
    suspend fun setLanguage(lang: String)
}

private class PreferencesImpl(
    private val context: Context
) : Preferences {
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isOnboardingNeededFlow(): Flow<Boolean> {
        return flowOf(false)
//        return sharedPreferences.getBooleanValueFlow(key = KEY_ONBOARDING_NEEDED, defValue = true)
//            .mapNotNull { it ?: true }
    }

    override suspend fun setOnboardingCompleted() {
        sharedPreferences.edit { putBoolean(KEY_ONBOARDING_NEEDED, false) }
    }

    override fun isSpeakerMutedFlow(): Flow<Boolean> {
        return sharedPreferences
            .getBooleanValueFlow(KEY_SPEAKER_MUTED, true)
            .mapNotNull { it ?: true }
    }

    override suspend fun setSpeakerMuted(muted: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_SPEAKER_MUTED, muted)
        }
    }

    override fun getLanguageFlow(): Flow<String?> {
        return sharedPreferences.getStringValueFlow(KEY_LANGUAGE)
    }

    override suspend fun setLanguage(lang: String) {
        sharedPreferences.edit {
            putString(KEY_LANGUAGE, lang)
        }
    }

    companion object {
        private const val PREFS_NAME = "com.chat.ui.preferences"

        private const val KEY_ONBOARDING_NEEDED = "onboarding_needed"
        private const val KEY_SPEAKER_MUTED = "speaker_muted"
        private const val KEY_LANGUAGE = "language"
    }
}