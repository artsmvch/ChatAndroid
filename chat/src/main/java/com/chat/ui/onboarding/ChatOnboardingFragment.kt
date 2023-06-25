package com.chat.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.chat.ui.R
import com.chat.ui.WithCustomStatusBar
import com.chat.utils.resolveColor
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

internal class ChatOnboardingFragment : Fragment(), WithCustomStatusBar {
    private val callback: ChatOnboardingCallback? get() {
        return (parentFragment as? ChatOnboardingCallback) ?: (activity as? ChatOnboardingCallback)
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            checkPage()
        }
    }

    private var viewPager: ViewPager2? = null
    private var button: MaterialButton? = null

    override fun getStatusBarColor(): Int? {
        return context?.resolveColor(com.google.android.material.R.attr.colorSurface)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_chat_onboarding, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.apply {
            fitsSystemWindows = true
            requestApplyInsets()
        }
        val tabs = view.findViewById<TabLayout>(R.id.tabs)
        viewPager = view.findViewById<ViewPager2>(R.id.view_pager).apply {
            adapter = ChatOnboardingPageAdapter(
                requestManager = Glide.with(this),
                pages = listOf(
                    ChatOnboardingPage(
                        imageId = R.drawable.png_onboarding_page1,
                        titleId = R.string.onboarding_page1_title,
                        subtitleId = R.string.onboarding_page1_subtitle
                    ),
                    ChatOnboardingPage(
                        imageId = R.drawable.png_onboarding_page2,
                        titleId = R.string.onboarding_page2_title,
                        subtitleId = R.string.onboarding_page2_subtitle
                    )
                )
            )
            registerOnPageChangeCallback(pageChangeCallback)
            TabLayoutMediator(tabs, this) { _, _ -> }.attach()
        }
        button = view.findViewById<MaterialButton>(R.id.button).apply {
            setOnClickListener {
                goNext()
            }
        }
        checkPage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        viewPager = null
        button = null
    }

    private fun checkPage() {
        val button = this.button ?: return
        val viewPager = this.viewPager ?: return
        val adapter = viewPager.adapter ?: return
        val position = viewPager.currentItem
        val isLastItem = position >= adapter.itemCount - 1
        button.setText(if (isLastItem) R.string.start else R.string.next)
    }

    private fun goNext() {
        val viewPager = this.viewPager ?: return
        val adapter = viewPager.adapter ?: return
        val position = viewPager.currentItem
        val isLastItem = position >= adapter.itemCount - 1
        if (isLastItem) {
            callback?.onOnboardingComplete()
        } else {
            viewPager.setCurrentItem(position + 1, true)
        }
    }
}