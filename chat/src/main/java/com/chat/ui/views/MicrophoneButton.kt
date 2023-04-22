package com.chat.ui.views

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import com.chat.ui.R

internal class MicrophoneButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr) {
    private val imageView: ImageView
    private var animator: Animator? = null

    var state: State = State.IDLE
        set(value) {
            if (field != value) {
                field = value
                setStateInternal(state)
            }
        }

    init {
        imageView = ImageView(context)
        addView(imageView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        imageView.setImageResource(R.drawable.ic_microphone_button)
    }

    private fun setStateInternal(state: State) {
        animator?.end()
        animator = null
        if (state == State.LISTENING) {
            val targetView = this
            animator = ValueAnimator.ofFloat(1.0f, 1.2f)
                .apply {
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener {
                        val value = it.animatedValue as Float
                        val scaleFactor = value
                        targetView.scaleX = scaleFactor
                        targetView.scaleY = scaleFactor
                    }
                    interpolator = AccelerateDecelerateInterpolator()
                    duration = 300L
                    doOnEnd {
                        targetView.scaleX = 1f
                        targetView.scaleY = 1f
                    }
                }
                .apply { start() }
        }
    }

    enum class State {
        IDLE,
        LISTENING
    }
}