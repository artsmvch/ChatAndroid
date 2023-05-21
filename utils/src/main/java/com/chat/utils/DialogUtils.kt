package com.chat.utils

import android.app.Dialog
import android.view.WindowManager

fun Dialog.updateWindowLayoutParams(update: WindowManager.LayoutParams.() -> Unit) {
    val window = this.window ?: return
    val layoutParams = WindowManager.LayoutParams()
    layoutParams.copyFrom(window.attributes)
    layoutParams.update()
    window.attributes = layoutParams
}

fun Dialog.setWindowSize(width: Int, height: Int) {
    val window = this.window ?: return
    window.setLayout(width, height)
}