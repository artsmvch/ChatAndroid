package com.chat.ui

import androidx.recyclerview.widget.DiffUtil
import com.artsmvch.chat.core.Message

internal object MessageDiffItemCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.text.contentEquals(newItem.text)
    }
}