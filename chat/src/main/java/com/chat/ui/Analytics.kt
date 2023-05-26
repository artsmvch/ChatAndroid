package com.chat.ui

interface Analytics {
    fun onError(e: Throwable) = Unit
    fun onUiError(e: Throwable) = Unit

    object Empty : Analytics
}