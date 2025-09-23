package com.chat.app

import android.app.Application
import android.os.Bundle
import com.chat.app.deepkseek.DeepSeekChatImpl
import com.chat.ui.Analytics
import com.artsmvch.chat.core.Chat
import com.chat.ui.ChatEvent
import com.chat.ui.ChatFeature
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.UUID

class ApplicationImpl : Application() {
    private val activityLifecycleCallbacksImpl = ActivityLifecycleCallbacksImpl()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityLifecycleCallbacksImpl)
        setupAdMob()
        setupChat()
    }

    private fun setupAdMob() {
        try {
            MobileAds.initialize(this)
        } catch (e: Throwable) {
            if (BuildConfig.GOOGLE_SERVICES_ENABLED) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    private fun setupChat() {
        val analytics = if (BuildConfig.GOOGLE_SERVICES_ENABLED) {
            object : Analytics {
                override fun onError(e: Throwable) {
                    reportError(e)
                }

                override fun onUiError(e: Throwable) {
                    reportError(e)
                }

                private fun reportError(error: Throwable) {
                    FirebaseCrashlytics.getInstance().recordException(error)
                }

                override fun onEvent(event: ChatEvent) {
                    FirebaseAnalytics.getInstance(this@ApplicationImpl)
                        .logEvent(event.name.toLowerCase(), Bundle.EMPTY)
                }
            }
        } else {
            Analytics.None
        }
        ChatFeature.init(
            delegate = object : ChatFeature.Delegate {
                override fun getChat(chatId: String?): Chat {
                    val chat = DeepSeekChatImpl(
                        context = this@ApplicationImpl,
                        chatId = chatId ?: UUID.randomUUID().toString()
                    )
                    chat.addListener(
                        AdvertisementChatListener { activityLifecycleCallbacksImpl.lastCreatedActivity }
                    )
                    return chat
                }
            },
            analytics = analytics
        )
    }
}