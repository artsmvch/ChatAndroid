package com.chat.gpt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.chat.ui.ChatFeature
import com.chat.utils.resolveColor

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Chat_GPT)
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