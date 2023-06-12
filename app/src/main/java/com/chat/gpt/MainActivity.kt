package com.chat.gpt

import android.os.Bundle
import com.chat.ui.ChatActivity

class MainActivity : ChatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Chat)
        super.onCreate(savedInstanceState)
    }
}