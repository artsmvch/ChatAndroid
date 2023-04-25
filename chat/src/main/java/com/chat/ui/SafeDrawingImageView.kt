package com.chat.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatImageView

internal class SafeDrawingImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): AppCompatImageView(context, attrs, defStyleAttr) {

    var onError: ((Throwable) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        try {
            super.onDraw(canvas)
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to draw wallpaper", e)
            onError?.invoke(e)
            if (BuildConfig.DEBUG) {
                throw e
            }
        }
    }

    companion object {
        private const val LOG_TAG = "WallpaperImageView"
    }
}