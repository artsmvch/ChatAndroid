package com.chat.ui

import androidx.fragment.app.Fragment
import java.util.concurrent.atomic.AtomicReference

object ChatFeature {
    private val chatRef = AtomicReference<Chat>()

    fun init(chat: Chat) {
        chatRef.set(chat)
    }

    fun createChatScreen(): Fragment {
        return ChatFragment()
    }

    internal fun getChat(): Chat {
        return chatRef.get() ?: throw NullPointerException("Feature has not been initialized")
    }
}