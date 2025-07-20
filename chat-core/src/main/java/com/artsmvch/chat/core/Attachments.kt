package com.artsmvch.chat.core


sealed interface Attachments

interface ImageAttachments: Attachments {
    val images: List<ImageInfo>
}