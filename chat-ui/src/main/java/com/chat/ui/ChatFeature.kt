package com.chat.ui

import com.artsmvch.chat.core.Chat
import java.util.concurrent.atomic.AtomicReference

object ChatFeature {
    private val delegateRef = AtomicReference<Delegate>()
    private val analyticsRef = AtomicReference<Analytics>()

    interface Delegate {
        fun getChat(chatId: String? = null): Chat
    }

    fun init(delegate: Delegate, analytics: Analytics = Analytics.None) {
        delegateRef.set(delegate)
        analyticsRef.set(analytics)
    }

    internal fun getAnalytics(): Analytics? {
        return analyticsRef.get()
    }

    fun getChat(chatId: String? = null): Chat {
        val delegate = delegateRef.get() ?: throw NullPointerException("Chat provider not found!")
        return delegate.getChat(chatId)
    }
}