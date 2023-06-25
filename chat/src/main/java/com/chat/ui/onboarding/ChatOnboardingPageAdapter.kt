package com.chat.ui.onboarding

import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.chat.ui.R
import com.chat.utils.dp

internal data class ChatOnboardingPage(
    @DrawableRes
    val imageId: Int,
    @StringRes
    val titleId: Int,
    @StringRes
    val subtitleId: Int
)

internal class ChatOnboardingPageAdapter(
    private val requestManager: RequestManager,
    private val pages: List<ChatOnboardingPage>
): RecyclerView.Adapter<ChatOnboardingPageAdapter.ViewHolder>() {
    override fun getItemCount(): Int = pages.count()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_onboarding_page, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView.findViewById<ImageView>(R.id.image)
        private val titleTextView = itemView.findViewById<TextView>(R.id.title)
        private val subtitleTextView = itemView.findViewById<TextView>(R.id.subtitle)

        init {
            imageView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val radius = view.context.dp(32)
                    outline.setRoundRect(0, -radius, view.measuredWidth, view.measuredHeight,
                        radius.toFloat())
                }
            }
            imageView.clipToOutline = true
        }

        fun bind(item: ChatOnboardingPage) {
            requestManager.load(item.imageId)
                .into(imageView)
            titleTextView.setText(item.titleId)
            subtitleTextView.setText(item.subtitleId)
        }
    }
}