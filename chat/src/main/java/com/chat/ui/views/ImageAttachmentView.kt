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
        val imageInfo = attachment?.images?.firstOrNull()
        Glide.with(this)
            .load(imageInfo?.imageUrl)
            .error(
                Glide.with(imageView).load(imageInfo?.filepath)
            )
            .into(imageView)
    }
}