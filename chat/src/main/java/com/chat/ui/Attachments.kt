package com.chat.ui


sealed interface Attachments

interface ImageAttachments: Attachments {
    val images: List<ImageInfo>
}