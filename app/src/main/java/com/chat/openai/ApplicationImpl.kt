package com.chat.openai

import android.app.Application
import com.chat.openai.engine.OpenAIChat
import com.chat.ui.ChatFeature
import com.chat.ui.Message
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicInteger

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
        val listener = object : OpenAIChat.Listener {
            val sentMessageCount = AtomicInteger(0)

            override fun onMessageSent(message: Message) {
                if (sentMessageCount.incrementAndGet() % 10 == 0) {
                    showInterstitialAd()
                }
            }

            override fun onMessageReceived(message: Message) {
            }
        }
        ChatFeature.init(OpenAIChat(this, listener))
    }

    private fun showInterstitialAd() {
        val testUnitId = "ca-app-pub-3940256099942544/1033173712"
        val adRequest = AdRequest.Builder()
            .build()
        val callback = object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                activityLifecycleCallbacksImpl.lastCreatedActivity?.also { activity ->
                    ad.show(activity)
                }
            }
        }
        InterstitialAd.load(this, testUnitId, adRequest, callback)
    }
}