package com.chat.gpt

import android.app.Application
import com.chat.gpt.engine.OpenAIChat
import com.chat.ui.ChatFeature
import com.google.android.gms.ads.MobileAds

class ApplicationImpl : Application() {
    private val activityLifecycleCallbacksImpl = ActivityLifecycleCallbacksImpl()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityLifecycleCallbacksImpl)
        setupAdMob()
        setupChat()
    }

    private fun setupAdMob() {
        MobileAds.initialize(this)
    }

    private fun setupChat() {
        val listeners = listOf(
            AdvertisementChatListener {
                activityLifecycleCallbacksImpl.lastCreatedActivity
            }
        )
        ChatFeature.init(OpenAIChat(this, listeners))
    }
}