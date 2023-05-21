package com.chat.ui.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.os.Handler
import com.chat.ui.Message
import com.chat.ui.R
import java.io.File
import java.util.concurrent.ConcurrentHashMap


internal fun getMessageAttachmentsDownloader(context: Context): MessageAttachmentsDownloader{
    return DefaultMessageAttachmentDownloader(context)
}

internal interface MessageAttachmentsDownloader {
    fun downloadImages(message: Message): Boolean
}

@Deprecated("Not tested")
private class DefaultMessageAttachmentDownloader(
    private val context: Context,
): MessageAttachmentsDownloader {
    override fun downloadImages(message: Message): Boolean {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return false
        val images = message.imageAttachments?.images
        if (images.isNullOrEmpty()) {
            return false
        }

        val downloadIds = ConcurrentHashMap<Long, String>()
        val downloadListener: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                downloadIds.remove(downloadId)
                if (downloadIds.isEmpty()) {
                    Handler(context.mainLooper).post {
                        this@DefaultMessageAttachmentDownloader.context.unregisterReceiver(this)
                    }
                }
            }
        }
        context.registerReceiver(downloadListener, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        images.mapNotNull { it.imageUrl }.forEach { imageUrl ->
            val folderName = "chatty_ai_bot"
            val fileName = "image-${System.currentTimeMillis()}.png"
            val subPath = File.separator + folderName + File.separator + fileName
            val request = DownloadManager.Request(Uri.parse(imageUrl))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, subPath
                )
                .apply { allowScanningByMediaScanner() }
                .setTitle(fileName)
                .setDescription(context.getString(R.string.image_download_description))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            val downloadId = downloadManager.enqueue(request)
            downloadIds[downloadId] = subPath
        }
        return true
    }
}