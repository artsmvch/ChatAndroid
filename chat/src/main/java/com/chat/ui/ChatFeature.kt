package com.chat.ui

import androidx.fragment.app.Fragment
import java.util.concurrent.atomic.AtomicReference

object ChatFeature {
    private val chatRef = AtomicReference<Chat>()
    private val analyticsRef = AtomicReference<Analytics>()

    fun init(chat: Chat, analytics: Analytics = Analytics.Empty) {
        chatRef.set(chat)
        analyticsRef.set(analytics)
    }

    fun isChatScreen(fragment: Fragment): Boolean {
        return fragment is ChatFragment
    }

    fun createChatScreen(): Fragment {
        return ChatFragment()
    }

    internal fun requireChat(): Chat {
        return chatRef.get() ?: throw NullPointerException("Feature has not been initialized")
    }

    internal fun getAnalytics(): Analytics? {
        return analyticsRef.get()
    }
}