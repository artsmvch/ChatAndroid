package com.chat.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent

@SuppressLint("QueryPermissionsNeeded")
fun Context.shareMessage(message: Message): Boolean {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    // intent.putExtra(Intent.EXTRA_SUBJECT, key)
    intent.putExtra(Intent.EXTRA_TEXT, message.text)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val chooserIntent = Intent.createChooser(intent, getString(R.string.share_message)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return if (chooserIntent.resolveActivity(packageManager) != null) {
        startActivity(chooserIntent)
        true
    } else {
        false
    }
}