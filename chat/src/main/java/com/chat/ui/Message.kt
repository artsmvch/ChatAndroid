package com.chat.ui


interface Message {
    val id: Long
    val isFromUser: Boolean
    val text: String
}