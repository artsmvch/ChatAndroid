package com.chat.ui

import androidx.annotation.ColorInt

internal interface WithCustomStatusBar {
    @ColorInt
    fun getStatusBarColor(): Int? = null
}