package com.chat.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


private suspend fun createText(context: Context, messages: List<Message>, addMetaInfo: Boolean): String {
    val separator = "\n\n"
    return if (addMetaInfo) {
        val userCaption = context.getString(R.string.user)
        val botCaption = context.getString(R.string.bot)
        withContext(Dispatchers.Default) {
            messages.joinToString(separator = separator) { message ->
                (if (message.isFromUser) userCaption else botCaption) +
                        '\n' + MessageDateUtils.getDateText(message) +
                        '\n' + message.text
            }
        }
    } else {
        withContext(Dispatchers.Default) {
            messages.joinToString(separator = separator) { it.text }
        }
    }
}

@SuppressLint("QueryPermissionsNeeded")
suspend fun Context.shareMessages(messages: List<Message>): Boolean {
    val text = createText(this, messages, addMetaInfo = true)
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

suspend fun Context.copyMessagesToClipboard(messages: List<Message>): Boolean {
    val clipboard: ClipboardManager =
        ContextCompat.getSystemService(this, ClipboardManager::class.java) ?: return false
    val label = getString(R.string.messages)
    val text = createText(this, messages, addMetaInfo = messages.count() > 1)
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    return true
}