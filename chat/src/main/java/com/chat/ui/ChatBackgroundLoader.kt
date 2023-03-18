package com.chat.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.WorkerThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.doOnLayout
import com.chat.utils.resolveDrawableId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.atomic.AtomicBoolean

internal object ChatBackgroundLoader {
    private const val MIN_WIDTH = 800
    private const val MIN_HEIGHT = 1440

    private val backgroundFlow = MutableStateFlow<Drawable?>(null)
    private val isOrWillBeLoaded = AtomicBoolean(false)

    fun load(imageView: ImageView) {
        loadBackgroundIfNeeded(imageView)
        observeBackground(imageView)
    }

    private fun loadBackgroundIfNeeded(imageView: ImageView) {
        if (isOrWillBeLoaded.getAndSet(true)) {
            return
        }
        // Gotta know view size before getting background
        imageView.doOnLayout {
            val context = imageView.context
            val targetWidth = imageView.measuredWidth.coerceAtLeast(MIN_WIDTH)
            val targetHeight = imageView.measuredHeight.coerceAtLeast(MIN_HEIGHT)
            GlobalScope.launch {
                val drawableId = context.resolveDrawableId(R.attr.chatBackground)
                    ?: return@launch
                val result: Result<Drawable?> = withContext(Dispatchers.IO) {
                    kotlin.runCatching {
                        blockingGetBackground1(context, drawableId, targetWidth, targetHeight)
                            ?: blockingGetBackground2(context, drawableId, targetWidth, targetHeight)
                    }
                }
                val drawable = result.getOrNull()
                if (drawable == null) {
                    isOrWillBeLoaded.set(false)
                }
                backgroundFlow.emit(drawable)
            }
        }
    }

    @WorkerThread
    @kotlin.jvm.Throws(Exception::class)
    private fun blockingGetBackground1(
        context: Context,
        @DrawableRes drawableId: Int,
        width: Int,
        height: Int
    ): Drawable? {
        val originalOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeResource(context.resources, drawableId, this)
        }
        val optimalOptions = getOptimalOptions(originalOptions, width, height)
        val bitmap = BitmapFactory.decodeResource(context.resources, drawableId, optimalOptions)
            ?: return null
        return BitmapDrawable(context.resources, bitmap)
    }

    @WorkerThread
    @kotlin.jvm.Throws(Exception::class)
    private fun blockingGetBackground2(
        context: Context,
        @DrawableRes drawableId: Int,
        width: Int,
        height: Int
    ): Drawable? {
        val drawable = AppCompatResources.getDrawable(context, drawableId)
            ?: return null
        if (drawable !is BitmapDrawable) {
            return drawable
        }
        var bitmap: Bitmap = drawable.bitmap
        // Crop if necessary
        if (bitmap.width.toFloat() / width != bitmap.height.toFloat() / height) {
            val dstDimensionRatio = width.toFloat() / height
            val dstWidth: Int
            val dstHeight: Int
            if (bitmap.width.toFloat() / bitmap.height > dstDimensionRatio) {
                dstWidth = (bitmap.height * dstDimensionRatio).toInt()
                dstHeight = bitmap.height
            } else {
                dstWidth = bitmap.width
                dstHeight = (bitmap.height / dstDimensionRatio).toInt()
            }
            val cropped = Bitmap.createBitmap(bitmap,
                (bitmap.width - dstWidth) / 2, (bitmap.height - dstHeight) / 2,
                dstWidth, dstHeight)
            if (cropped != bitmap) {
                bitmap.recycle()
                bitmap = cropped
            }
        }
        // Scale if necessary
        if (bitmap.width > width && bitmap.height > height) {
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
            if (scaledBitmap != bitmap) {
                bitmap.recycle()
                bitmap = scaledBitmap
            }
        }
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun getOptimalOptions(
        original: BitmapFactory.Options,
        targetWidth: Int,
        targetHeight: Int
    ): BitmapFactory.Options {
        var widthTmp = original.outWidth
        var heightTmp = original.outHeight
        var scale = 1
        while (widthTmp > targetWidth || heightTmp > targetHeight) {
            widthTmp /= 2
            heightTmp /= 2
            scale *= 2
        }
        return BitmapFactory.Options().apply { inSampleSize = scale }
    }

    private fun observeBackground(imageView: ImageView) {
        val listener = OnAttachStateChangeListenerImpl(backgroundFlow)
        imageView.addOnAttachStateChangeListener(listener)
        if (imageView.isAttachedToWindow) {
            listener.noteAttached(imageView)
        }
    }
}

private class OnAttachStateChangeListenerImpl(
    val flow: Flow<Drawable?>
) : OnAttachStateChangeListener {
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    var job: Job? = null

    override fun onViewAttachedToWindow(view: View) {
        noteAttached(view)
    }

    override fun onViewDetachedFromWindow(view: View) {
        job?.cancel()
        job = null
    }

    fun noteAttached(view: View) {
        job?.cancel()
        job = scope.launch {
            flow.collectLatest { drawable ->
                (view as? ImageView)?.setImageDrawable(drawable)
            }
        }
    }
}