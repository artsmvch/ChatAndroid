package com.chat.openai

import android.app.Application
import com.chat.openai.engine.OpenAIChat
import com.chat.ui.ChatFeature

class ApplicationImpl : Application() {
    override fun onCreate() {
        super.onCreate()
        setup()
    }

    private fun setup() {
        ChatFeature.init(OpenAIChat())
    }
}