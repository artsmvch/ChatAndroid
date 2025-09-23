package com.artsmvch.chat.core


interface Message {
    val id: Long
    val chatId: String
    val timestamp: Long
    val role: String
    val isFromUser: Boolean
    val text: String
    val imageAttachments: ImageAttachments?
}