package com.chat.ui.preferences

import android.content.Context
import android.content.coroutines.edit
import android.content.coroutines.getBooleanValueFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

internal fun obtainPreferences(context: Context): Preferences {
    return PreferencesImpl(context)
}

internal interface Preferences {
    fun isSpeakerMutedFlow(): Flow<Boolean>
    suspend fun isSpeakerMuted(): Boolean = isSpeakerMutedFlow().first()
    suspend fun setSpeakerMuted(muted: Boolean)
}

private class PreferencesImpl(
    private val context: Context
) : Preferences {
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    companion object {
        private const val PREFS_NAME = "com.chat.ui.preferences"

        private const val KEY_SPEAKER_MUTED = "speaker_muted"
    }
}