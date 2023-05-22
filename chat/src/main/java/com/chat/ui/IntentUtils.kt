package com.chat.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


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
    val imageFileUris = withContext(Dispatchers.Default) {
        messages
            .flatMap { it.imageAttachments?.images.orEmpty() }
            .mapNotNull { info ->
                if (info.filepath.isNullOrBlank()) return@mapNotNull null
                val file = File(info.filepath.orEmpty())
                if (!file.exists()) return@mapNotNull null
                val context = this@shareMessages
                FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", file)
            }
            .let { ArrayList(it) }
    }
    val intent = if (imageFileUris.isNotEmpty()) {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageFileUris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        val text = createText(this, messages, addMetaInfo = true)
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return withContext(Dispatchers.Main) {
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