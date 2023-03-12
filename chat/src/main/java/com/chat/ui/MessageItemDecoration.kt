package com.chat.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.chat.utils.dp

internal class MessageItemDecoration : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val verticalMargin = view.context.dp(8)
        outRect.set(0, verticalMargin, 0, verticalMargin)
    }
}