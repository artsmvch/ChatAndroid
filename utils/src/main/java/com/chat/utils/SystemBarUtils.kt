package com.chat.utils

import android.app.Activity
import android.view.Window
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object SystemBarUtils {
    private fun Window.insetsControllerCompat(): WindowInsetsControllerCompat? {
        val decorView = this.peekDecorView() ?: return null
        return WindowCompat.getInsetsController(this, decorView)
    }

    fun setStatusBarColor(window: Window, @ColorInt color: Int) {
        window.statusBarColor = color
    }

    fun setStatusBarColor(activity: Activity, @ColorInt color: Int) {
        activity.window?.also { setStatusBarColor(it, color) }
    }

    fun isLight(@ColorInt color: Int): Boolean {
        val luminance = ColorUtils.calculateLuminance(color)
        return luminance > 0.75
    }

    fun isDark(@ColorInt color: Int): Boolean {
        return !isLight(color)
    }

    fun setStatusBarVisible(window: Window, isVisible: Boolean) {
        val controller = window.insetsControllerCompat() ?: return
        if (isVisible) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    fun setStatusBarAppearanceLight(window: Window, isLight: Boolean) {
        window.insetsControllerCompat()?.also { controller ->
            controller.isAppearanceLightStatusBars = isLight
        }
    }
}