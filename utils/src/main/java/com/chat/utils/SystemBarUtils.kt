package com.chat.utils

import android.app.Activity
import android.view.Window
import androidx.annotation.ColorInt

object SystemBarUtils {
    fun setStatusBarColor(window: Window, @ColorInt color: Int) {
        window.statusBarColor = color
    }

    fun setStatusBarColor(activity: Activity, @ColorInt color: Int) {
        activity.window?.also { setStatusBarColor(it, color) }
    }
}