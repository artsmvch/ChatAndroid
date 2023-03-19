package com.chat.gpt

import android.app.Activity
import com.chat.gpt.engine.OpenAIChat
import com.chat.ui.Message
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicInteger

internal class AdvertisementChatListener(
    private val foregroundActivityProvider: () -> Activity?
) : OpenAIChat.Listener {
    private val sentMessageCount = AtomicInteger(0)

    override fun onMessageSent(message: Message) {
        if (sentMessageCount.incrementAndGet() % 10 == 0) {
            showInterstitialAd()
        }
    }

    override fun onMessageReceived(message: Message) {
    }

    private fun showInterstitialAd() {
        val activity = foregroundActivityProvider.invoke() ?: return
        val testUnitId = TEST_UNIT_ID
        val adRequest = AdRequest.Builder()
            .build()
        val callback = object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                ad.show(activity)
            }
        }
        InterstitialAd.load(activity, testUnitId, adRequest, callback)
    }

    private companion object {
        private const val TEST_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    }
}