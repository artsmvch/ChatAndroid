package com.chat.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.chat.utils.setWindowSize
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

internal class MessageImageViewerDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AppCompatDialog(requireContext(), theme).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_message_image_viewer)
            val imageUrls = arguments?.getStringArrayList(ARG_IMAGE_URLS).orEmpty().let { it + it + it }
            findViewById<ViewPager2>(R.id.view_pager)?.also { viewPager ->
                viewPager.adapter = MessageImageAdapter(imageUrls)
                findViewById<TabLayout>(R.id.tabs)?.also { tabs ->
                    TabLayoutMediator(tabs, viewPager) { _, _ -> }.attach()
                    tabs.isVisible = imageUrls.count() > 1
                }
            }
            setWindowSize(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    companion object {
        private const val FRAGMENT_TAG = "message_image_viewer"
        private const val ARG_IMAGE_URLS = "image_urls"

        fun show(fragmentManager: FragmentManager, message: Message) {
            val args = Bundle(1)
            val imageUrls = ArrayList(message.imageAttachments?.imageUrls.orEmpty())
            args.putStringArrayList(ARG_IMAGE_URLS, imageUrls)
            val dialog = MessageImageViewerDialog().apply { arguments = args }
            dialog.show(fragmentManager, FRAGMENT_TAG)
        }
    }
}

private class MessageImageAdapter(
    private val imageUrls: List<String>
): Adapter<MessageImageAdapter.ViewHolder>() {

    override fun getItemCount(): Int = imageUrls.count()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = ImageView(parent.context)
        itemView.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.MATCH_PARENT
        )
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(imageUrls[position])
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView as ImageView

        fun bind(imageUrl: String) {
            Glide.with(imageView)
                .load(imageUrl)
                .into(imageView)
        }
    }
}