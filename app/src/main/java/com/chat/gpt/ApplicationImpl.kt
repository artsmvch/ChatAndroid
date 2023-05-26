package com.chat.gpt

import android.app.Application
import com.chat.gpt.engine.OpenAIChat
import com.chat.ui.Analytics
import com.chat.ui.ChatFeature
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

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
        val chat = OpenAIChat(this)
        chat.addListener(
            AdvertisementChatListener { activityLifecycleCallbacksImpl.lastCreatedActivity }
        )
        chat.addListener(FirebaseAnalyticsChatListener(this))
        val analytics = object : Analytics {
            override fun onError(e: Throwable) {
                reportError(e)
            }

            override fun onUiError(e: Throwable) {
                reportError(e)
            }

            private fun reportError(error: Throwable) {
                FirebaseCrashlytics.getInstance().recordException(error)
            }
        }
        ChatFeature.init(chat, analytics)
    }
}