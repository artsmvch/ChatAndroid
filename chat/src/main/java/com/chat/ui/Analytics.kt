package com.chat.ui

interface Analytics {
    fun onError(e: Throwable) = Unit

    object Empty : Analytics
}