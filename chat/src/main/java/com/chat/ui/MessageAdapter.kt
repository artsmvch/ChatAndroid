package com.chat.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.GravityInt
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chat.utils.dp
import com.chat.utils.resolveColor
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel

internal typealias OnItemLongClickListener = (item: Message, itemView: View) -> Unit
internal interface MultiSelectionModeListener {
    fun onStartMultiSelectionMode() = Unit
    fun onItemSelectionChanged(item: Message, isSelected: Boolean) = Unit
    fun onStopMultiSelectionMode() = Unit
}

internal class MessageAdapter constructor(
    private val onItemLongClickListener: OnItemLongClickListener,
    private val multiSelectionModeListener: MultiSelectionModeListener
): PagedListAdapter<Message, MessageAdapter.ViewHolder>(MessageDiffItemCallback) {

    var isMultiSelectionModeEnabled: Boolean = false
        private set
    private val selectedItemIds = HashMap<Long, Message>()

    private fun handleItemClick(position: Int, item: Message?, itemView: View, isLongClick: Boolean) {
        if (item == null) {
            return
        }
        if (isLongClick || isMultiSelectionModeEnabled) {
            handleMultiSelectionMode(position, item, itemView)
        }
    }

    private fun handleMultiSelectionMode(position: Int, item: Message, itemView: View) {
        val oldItem = selectedItemIds.put(item.id, item)
        val wasSelected = oldItem != null
        if (wasSelected) {
            selectedItemIds.remove(item.id)
        }
        // Check if the multi selection mode is being started
        if (selectedItemIds.isNotEmpty() && !isMultiSelectionModeEnabled) {
            isMultiSelectionModeEnabled = true
            multiSelectionModeListener.onStartMultiSelectionMode()
        }
        // Check if the multi selection mode is being stopped
        if (selectedItemIds.isEmpty() && isMultiSelectionModeEnabled) {
            isMultiSelectionModeEnabled = false
            multiSelectionModeListener.onStopMultiSelectionMode()
        }
        multiSelectionModeListener.onItemSelectionChanged(item, !wasSelected)
        notifyItemChanged(position)
    }

    fun stopMultiSelectionMode() {
        if (isMultiSelectionModeEnabled) {
            selectedItemIds.clear()
            isMultiSelectionModeEnabled = false
            multiSelectionModeListener.onStopMultiSelectionMode()
            notifyDataSetChanged()
        }
    }

    fun getSelectedItemCount(): Int {
        return selectedItemIds.count()
    }

    fun getSelectedItems(): Set<Message> {
        return selectedItemIds.values.toSet()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val isSelected = if (item != null) {
            selectedItemIds.contains(item.id)
        } else {
            false
        }
        holder.bind(item, isSelected)
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
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
                handleItemClick(bindingAdapterPosition, getItem(), clickedView, isLongClick = true)
                true
            }
            cardView.setOnClickListener { clickedView ->
                handleItemClick(bindingAdapterPosition, getItem(), clickedView, isLongClick = false)
            }
        }

        private fun getItem(): Message? {
            return itemView.tag as? Message
        }

        fun bind(item: Message?, isSelected: Boolean) {
            itemView.tag = item
            if (item != null) {
                @GravityInt
                val contentGravity: Int
                @MaterialCardView.CheckedIconGravity
                val checkedIconGravity: Int
                if (item.isFromUser) {
                    contentGravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                    checkedIconGravity = MaterialCardView.CHECKED_ICON_GRAVITY_TOP_START
                } else {
                    contentGravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                    checkedIconGravity = MaterialCardView.CHECKED_ICON_GRAVITY_TOP_END
                }
                cardView.shapeAppearanceModel =
                    if (item.isFromUser) rightShapeAppearanceModel else leftShapeAppearanceModel
                cardView.setCardBackgroundColor(
                    if (item.isFromUser) userMessageBackgroundColor else messageBackgroundColor
                )
                cardView.updateLayoutParams<LinearLayout.LayoutParams> {
                    this.gravity = contentGravity
                }
                cardView.checkedIconGravity = checkedIconGravity
                textView.text = item.text
                dateView.text = MessageDateUtils.getDateText(item)
                dateView.updateLayoutParams<LinearLayout.LayoutParams> {
                    this.gravity = contentGravity
                }
                linearLayout.updatePadding(
                    left = if (item.isFromUser) horizontalPadding else 0,
                    right = if (item.isFromUser) 0 else horizontalPadding
                )
                linearLayout.gravity = contentGravity
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
            cardView.isChecked = isSelected
        }
    }
}