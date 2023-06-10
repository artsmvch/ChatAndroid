package com.chat.gpt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.chat.ui.BackPressHandler
import com.chat.ui.ChatFeature
import com.chat.utils.resolveColor

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Chat)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
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
        if (fragment != null && ChatFeature.isChatScreen(fragment)) {
            return
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ChatFeature.createChatScreen(), TAG_CHAT)
            .commitNow()
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

    companion object {
        private const val TAG_CHAT = "chat"
    }
}