package com.chat.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chat.utils.dp
import com.chat.utils.resolveDrawable

internal class MessageAdapter: PagedListAdapter<Message, MessageAdapter.ViewHolder>(MessageDiffItemCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val frameLayout = itemView as FrameLayout
        private val textView: TextView
        private val horizontalPadding = itemView.context.dp(32)
        private val messageBackground = itemView.context.resolveDrawable(R.attr.messageBackground)
        private val userMessageBackground = itemView.context.resolveDrawable(R.attr.userMessageBackground)

        init {
            textView = itemView.findViewById(R.id.text)
        }

        fun bind(item: Message?) {
            if (item != null) {
                textView.text = item.text
                textView.background =
                    if (item.isFromUser) userMessageBackground else messageBackground
                textView.updateLayoutParams<FrameLayout.LayoutParams> {
                    gravity =
                        (if (item.isFromUser) Gravity.RIGHT else Gravity.LEFT) or Gravity.CENTER_VERTICAL
                }
                frameLayout.updatePadding(
                    left = if (item.isFromUser) horizontalPadding else 0,
                    right = if (item.isFromUser) 0 else horizontalPadding
                )
            } else {
                textView.text
                textView.background = messageBackground
                textView.updateLayoutParams<FrameLayout.LayoutParams> {
                    gravity = Gravity.CENTER
                }
                frameLayout.updatePadding(
                    left = horizontalPadding,
                    right = horizontalPadding
                )
            }
        }
    }
}