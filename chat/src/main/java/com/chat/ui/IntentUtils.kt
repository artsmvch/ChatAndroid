package com.chat.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("QueryPermissionsNeeded")
suspend fun Context.shareMessages(messages: List<Message>): Boolean {
    val userCaption = getString(R.string.user)
    val botCaption = getString(R.string.bot)
    val text = withContext(Dispatchers.Default) {
        messages.joinToString(separator = "\n\n") { message ->
            (if (message.isFromUser) userCaption else botCaption) +
                    '\n' + MessageDateUtils.getDateText(message) +
                    '\n' + message.text
        }
    }
    return withContext(Dispatchers.Main) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        // intent.putExtra(Intent.EXTRA_SUBJECT, key)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val chooserIntent = Intent.createChooser(intent, getString(R.string.share_messages)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (chooserIntent.resolveActivity(packageManager) != null) {
            startActivity(chooserIntent)
            true
        } else {
            false
        }
    }
}