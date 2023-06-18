package com.chat.ui

enum class ChatEvent {
    ONBOARDING_COMPLETED,
    MESSAGE_SENT,
    SPEAKER_ENABLED,
    SPEAKER_DISABLED
}

interface Analytics {
    fun onError(e: Throwable) = Unit
    fun onUiError(e: Throwable) = Unit
    fun onEvent(event: ChatEvent) = Unit

    object Empty : Analytics
}