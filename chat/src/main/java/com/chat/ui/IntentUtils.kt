package com.chat.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent

@SuppressLint("QueryPermissionsNeeded")
fun Context.shareMessages(messages: Set<Message>): Boolean {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    // intent.putExtra(Intent.EXTRA_SUBJECT, key)
    val text = messages.joinToString(separator = "\n")
    intent.putExtra(Intent.EXTRA_TEXT, text)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val chooserIntent = Intent.createChooser(intent, getString(R.string.share_messages)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return if (chooserIntent.resolveActivity(packageManager) != null) {
        startActivity(chooserIntent)
        true
    } else {
        false
    }
}