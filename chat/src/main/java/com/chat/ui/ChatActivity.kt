package com.chat.ui

import android.content.Context
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.chat.ui.onboarding.ChatOnboardingCallback
import com.chat.ui.onboarding.ChatOnboardingFragment
import com.chat.ui.preferences.Preferences
import com.chat.ui.preferences.getPreferencesInstance
import com.chat.utils.SystemBarUtils
import com.chat.utils.resolveColor
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

abstract class ChatActivity : AppCompatActivity(), ChatOnboardingCallback {

    private val viewModel: ChatHostViewModel by lazy {
        val factory = ChatHostViewModelFactory(this)
        val provider = ViewModelProvider(this, factory)
        provider[ChatHostViewModel::class.java]
    }

    private val fragmentLifecycleCallbacks = object : FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fragmentManager: FragmentManager, fragment: Fragment) {
            window?.also { safeWindow ->
                var statusBarColor = (fragment as? WithCustomStatusBar)?.getStatusBarColor()
                if (statusBarColor == null) {
                    statusBarColor = resolveColor(android.R.attr.statusBarColor)
                }
                SystemBarUtils.setStatusBarColor(safeWindow, statusBarColor)
                SystemBarUtils.setStatusBarAppearanceLight(safeWindow, SystemBarUtils.isLight(statusBarColor))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_chat)
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)
        viewModel.screen.observe(this) { screen: Screen? ->
            val fragmentClazz = when(screen) {
                Screen.ONBOARDING -> ChatOnboardingFragment::class
                Screen.CHAT -> ChatFragment::class
                null -> null
            }
            if (fragmentClazz != null) {
                ensureScreen(fragmentClazz)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
    }

    @UiThread
    private fun ensureScreen(clazz: KClass<out Fragment>) {
        if (supportFragmentManager.isStateSaved) {
            return
        }
        val fragment = supportFragmentManager.findFragmentByTag(TAG_SCREEN)
        if (fragment == null || !clazz.isInstance(fragment)) {
            val newFragment = clazz.java.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, newFragment, TAG_SCREEN)
                .commitNow()
        }
    }

    override fun onBackPressed() {
        if (!handleBackPress()) {
            super.onBackPressed()
        }
    }

    private fun handleBackPress(): Boolean {
        for (fragment in supportFragmentManager.fragments) {
            if (fragment is BackPressHandler && fragment.handleBackPress()) {
                return true
            }
        }
        return false
    }

    @CallSuper
    override fun onOnboardingComplete() {
        viewModel.onOnboardingCompleted()
    }

    companion object {
        private const val TAG_SCREEN = "screen"
    }
}

internal enum class Screen {
    ONBOARDING,
    CHAT
}

internal class ChatHostViewModelFactory(
    private val context: Context
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChatHostViewModel(
            preferences = getPreferencesInstance(context),
            analytics = ChatFeature.getAnalytics() ?: Analytics.Empty
        ) as T
    }
}

internal class ChatHostViewModel(
    private val preferences: Preferences,
    private val analytics: Analytics
): ViewModel() {
    private val _screen by lazy {
        liveData<Screen?> {
            preferences.isOnboardingNeededFlow().collect { isNeeded: Boolean? ->
                if (isNeeded == true) {
                    emit(Screen.ONBOARDING)
                } else if (isNeeded == false) {
                    emit(Screen.CHAT)
                }
            }
        }.distinctUntilChanged()
    }
    val screen: LiveData<Screen?> get() = _screen

    fun onOnboardingCompleted() {
        viewModelScope.launch {
            preferences.setOnboardingCompleted()
            analytics.onEvent(ChatEvent.ONBOARDING_COMPLETED)
        }
    }

}