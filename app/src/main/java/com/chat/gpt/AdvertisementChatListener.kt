package com.chat.gpt

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chat.firebase.FirebaseRemoteConfigCache
import com.chat.gpt.engine.OpenAIChat
import com.chat.ui.Message
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

internal class AdvertisementChatListener(
    private val foregroundActivityProvider: () -> Activity?
) : OpenAIChat.Listener {
    private val sentMessageCount = AtomicInteger(0)

    override fun onMessageSent(message: Message) {
        if (sentMessageCount.incrementAndGet() % 10 == 0) {
            checkAd()
        }
    }

    override fun onMessageReceived(message: Message) {
    }

    private fun checkAd() {
        if (BuildConfig.DEBUG) {
            foregroundActivityProvider.invoke()?.also { activity ->
                showInterstitialAd(activity, TEST_UNIT_ID)
            }
            return
        }
        val coroutineScope = foregroundActivityProvider.invoke().let { activity ->
            if (activity is AppCompatActivity) {
                activity.lifecycleScope
            } else {
                GlobalScope
            }
        }
        coroutineScope.launch {
            FirebaseRemoteConfigCache.getString(KEY_ADMOB_INTERSTITIAL_AD_CONFIG)
                .collectLatest { config ->
                    if (config != null) {
                        val json = JSONObject(config)
                        val isEnabled = json.getBoolean("is_enabled")
                        val minVersionCode = json.optInt("min_version_code")
                        val unitId = json.getString("unit_id")
                        if (isEnabled && BuildConfig.VERSION_CODE >= minVersionCode) {
                            foregroundActivityProvider.invoke()?.also { activity ->
                                showInterstitialAd(activity, unitId)
                            }
                        }
                    }
                }
        }
    }


    private fun showInterstitialAd(activity: Activity, unitId: String) {
        val adRequest = AdRequest.Builder()
            .build()
        val callback = object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                ad.show(activity)
            }
        }
        InterstitialAd.load(activity, unitId, adRequest, callback)
    }

    private companion object {
        private const val TEST_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

        private const val KEY_ADMOB_INTERSTITIAL_AD_CONFIG = "admob_interstitial_ad_config"
    }
}