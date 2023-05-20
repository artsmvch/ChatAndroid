package com.chat.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.chat.ui.ImageAttachments

internal class ImageAttachmentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr) {
    private val imageView: ImageView

    init {
        imageView = AppCompatImageView(context)
        addView(imageView, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun bind(attachment: ImageAttachments?) {
        Glide.with(this)
            .load(attachment?.imageUrls?.firstOrNull())
            .into(imageView)
    }
}