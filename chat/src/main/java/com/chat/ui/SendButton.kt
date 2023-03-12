package com.chat.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout

internal class SendButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr) {

    var state: State = State.IDLE
        set(value) {
            if (field != value) {
                field = value
                setStateInternal(value)
            }
        }

    private val sendIconView: View
    private val loadingView: View

    init {
        View.inflate(context, R.layout.merge_send_button, this)
        sendIconView = findViewById(R.id.send_icon)
        loadingView = findViewById(R.id.progress)
        setStateInternal(state)
    }

    private fun setStateInternal(state: State) {
        animateFade(sendIconView, visible = state == State.IDLE)
        animateFade(loadingView, visible = state == State.LOADING)
    }

    private fun animateFade(view: View, visible: Boolean) {
        if (visible) {
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.scaleX = 0.6f
            view.scaleY = 0.6f
            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        } else {
            view.animate()
                .alpha(0f)
                .scaleX(0.6f)
                .scaleY(0.6f)
                .setDuration(150L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    view.visibility = View.INVISIBLE
                }
                .start()
        }
    }

    override fun performClick(): Boolean {
        if (state == State.LOADING) {
            return true
        }
        return super.performClick()
    }

    enum class State {
        IDLE,
        LOADING
    }
}