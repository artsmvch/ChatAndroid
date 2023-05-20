package com.chat.ui


interface Message {
    val id: Long
    val timestamp: Long
    val isFromUser: Boolean
    val text: String
    val imageAttachments: ImageAttachments?
}