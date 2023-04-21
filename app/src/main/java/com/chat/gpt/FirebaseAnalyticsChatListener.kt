package com.chat.gpt

import android.app.Application
import android.os.Bundle
import com.chat.ui.Chat
import com.chat.ui.Message
import com.google.firebase.analytics.FirebaseAnalytics

internal class FirebaseAnalyticsChatListener(
    private val application: Application
) : Chat.Listener {
    override fun onMessageSent(message: Message) {
        FirebaseAnalytics.getInstance(application).logEvent("message_sent", EMPTY_BUNDLE)
    }

    override fun onMessageReceived(message: Message) {
    }

    private companion object {
        private val EMPTY_BUNDLE = Bundle(0)
    }
}