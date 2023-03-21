package com.chat.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chat.utils.dp
import com.chat.utils.resolveColor
import com.chat.utils.resolveDrawable
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel

internal typealias OnItemLongClickListener = (item: Message, itemView: View) -> Unit

internal class MessageAdapter constructor(
    private val onItemLongClickListener: OnItemLongClickListener
): PagedListAdapter<Message, MessageAdapter.ViewHolder>(MessageDiffItemCallback) {

    public override fun getItem(position: Int): Message? {
        return super.getItem(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ViewHolder(itemView, onItemLongClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(itemView: View,
                     onItemLongClickListener: OnItemLongClickListener): RecyclerView.ViewHolder(itemView) {
//        private val frameLayout = itemView as FrameLayout
        private val linearLayout = itemView as LinearLayout
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card)
        private val textView: TextView
        private val dateView: TextView = itemView.findViewById(R.id.date)
        private val horizontalPadding = itemView.context.dp(32)
        private val messageBackgroundColor = itemView.context.resolveColor(R.attr.messageBackgroundColor)
        private val userMessageBackgroundColor = itemView.context.resolveColor(R.attr.userMessageBackgroundColor)

        private val cardCornerRadius: Float = itemView.context.dp(16).toFloat()
        private val defaultShapeAppearanceModel = ShapeAppearanceModel.builder()
            .setBottomLeftCorner(CornerFamily.ROUNDED, cardCornerRadius)
            .setTopLeftCorner(CornerFamily.ROUNDED, cardCornerRadius)
            .setTopRightCorner(CornerFamily.ROUNDED, cardCornerRadius)
            .setBottomRightCorner(CornerFamily.ROUNDED, cardCornerRadius)
            .build()
        private val leftShapeAppearanceModel = defaultShapeAppearanceModel.toBuilder()
            .setBottomLeftCorner(CornerFamily.ROUNDED, 0f)
            .setBottomRightCorner(CornerFamily.ROUNDED, cardCornerRadius)
            .build()
        private val rightShapeAppearanceModel = defaultShapeAppearanceModel.toBuilder()
            .setBottomLeftCorner(CornerFamily.ROUNDED, cardCornerRadius)
            .setBottomRightCorner(CornerFamily.ROUNDED, 0f)
            .build()

        init {
            textView = itemView.findViewById(R.id.text)
            cardView.setOnLongClickListener { clickedView ->
                (itemView.tag as? Message)?.also { item ->
                    onItemLongClickListener.invoke(item, clickedView)
                }
                true
            }
        }

        fun bind(item: Message?) {
            itemView.tag = item
            if (item != null) {
                val gravity = (if (item.isFromUser) Gravity.RIGHT else Gravity.LEFT) or Gravity.CENTER_VERTICAL
                cardView.shapeAppearanceModel =
                    if (item.isFromUser) rightShapeAppearanceModel else leftShapeAppearanceModel
                cardView.setCardBackgroundColor(
                    if (item.isFromUser) userMessageBackgroundColor else messageBackgroundColor
                )
                cardView.updateLayoutParams<LinearLayout.LayoutParams> {
                    this.gravity = gravity
                }
                textView.text = item.text
                dateView.text = MessageDateUtils.getDateText(item)
                dateView.updateLayoutParams<LinearLayout.LayoutParams> {
                    this.gravity = gravity
                }
                linearLayout.updatePadding(
                    left = if (item.isFromUser) horizontalPadding else 0,
                    right = if (item.isFromUser) 0 else horizontalPadding
                )
                linearLayout.gravity = gravity
            } else {
                cardView.shapeAppearanceModel = defaultShapeAppearanceModel
                cardView.setCardBackgroundColor(userMessageBackgroundColor)
                cardView.updateLayoutParams<LinearLayout.LayoutParams> {
                    gravity = Gravity.CENTER
                }
                textView.text = null
                dateView.text = null
                linearLayout.updatePadding(
                    left = horizontalPadding,
                    right = horizontalPadding
                )
                linearLayout.gravity = Gravity.CENTER
            }
        }
    }
}