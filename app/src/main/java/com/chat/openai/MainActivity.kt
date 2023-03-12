package com.chat.openai

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import com.chat.ui.ChatFeature
import com.chat.utils.resolveColor

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.coordinator).setOnApplyWindowInsetsListener { layout, insets ->
            layout.updatePadding(bottom = insets.systemWindowInsetBottom)
            insets.replaceSystemWindowInsets(
                insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.stableInsetRight, 0
            )
            insets
        }
        ensureChatScreen()
    }

    override fun onResume() {
        super.onResume()
        window?.apply {
            SystemBarUtils.setStatusBarColor(this, resolveColor(android.R.attr.statusBarColor))
        }
    }

    private fun ensureChatScreen() {
        val fragment = supportFragmentManager.findFragmentByTag(TAG_CHAT)
        if (fragment != null) {
            return
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ChatFeature.createChatScreen())
            .commitNow()
    }

    companion object {
        private const val TAG_CHAT = "chat"
    }
}