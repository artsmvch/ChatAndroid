package com.chat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView


internal class ChatFragment : Fragment() {
    private var editText: EditText? = null
    private var sendButton: SendButton? = null
    private var messageListView: RecyclerView? = null
    private var messageAdapter: MessageAdapter? = null

    private val viewModel: ChatViewModel by lazy {
        val provider = ViewModelProvider(this)
        provider[ChatViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        skipWindowInsets(view)
        view.findViewById<View>(R.id.chat).setOnApplyWindowInsetsListener { layout, insets ->
            layout.updatePadding(bottom = insets.systemWindowInsetBottom)
            insets.replaceSystemWindowInsets(
                insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.stableInsetRight, 0
            )
            insets
        }

        view.findViewById<ImageView>(R.id.background).also { imageView ->
            ChatBackgroundLoader.load(imageView)
        }

        editText = view.findViewById<EditText>(R.id.edit_text).apply {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    triggerSendMessage()
                    true
                } else {
                    false
                }
            }
        }

        sendButton = view.findViewById<SendButton>(R.id.send_button).apply {
            setOnClickListener {
                triggerSendMessage()
            }
        }

        val adapter = MessageAdapter()
        messageAdapter = adapter
        messageListView = view.findViewById<RecyclerView>(R.id.messages_list).apply {
            setOnApplyWindowInsetsListener { listView, insets ->
                listView.updatePadding(top = insets.systemWindowInsetTop)
                insets.consumeSystemWindowInsets()
            }
            layoutManager = MessageLayoutManager(view.context).apply {
                stackFromEnd = true
            }
            // addItemDecoration(MessageMarginItemDecoration())
            this.adapter = adapter
        }

        observeViewModel(viewLifecycleOwner)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        editText = null
        sendButton = null
        messageListView = null
        messageAdapter = null
    }

    private fun observeViewModel(owner: LifecycleOwner) = with(viewModel) {
        isLoading.observe(owner) { isLoading ->
            sendButton?.state = if (isLoading) SendButton.State.LOADING else SendButton.State.IDLE
        }

        messagePagedList.observe(owner) { pagedList ->
            messageAdapter?.submitList(pagedList) {
                smoothScrollToLastMessage()
            }
        }

        error.observe(owner) { error ->
            error?.also(::showError)
        }

        clearInputFieldEvent.observe(owner) {
            editText?.text = null
        }
    }

    private fun smoothScrollToLastMessage() {
        val messageCount = messageAdapter?.currentList?.size ?: return
        messageListView?.smoothScrollToPosition(messageCount)
    }

    private fun skipWindowInsets(view: View) {
        view.fitsSystemWindows = true
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets -> insets }
        view.requestApplyInsets()
    }

    private fun triggerSendMessage() {
        val editText = editText ?: return
        val text = editText.text ?: return
        if (text.isNotEmpty()) {
            viewModel.onSendMessage(text)
        }
    }

    private fun showError(error: Throwable) {
        val context = this.context ?: return
        val message = error.message.orEmpty().ifBlank { error.toString() }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

//    private fun smoothScrollChatToBottom() {
//        val scrollView = this.chatScrollView ?: return
//        val childView = if (scrollView.childCount > 0) scrollView.getChildAt(0) else null
//        childView ?: return
//        childView.doOnLayout {
//            val childBottom = childView.bottom + scrollView.paddingBottom
//            val deltaScrollY = childBottom - scrollView.measuredHeight - scrollView.scrollY
//            scrollView.smoothScrollBy(0, deltaScrollY, 400)
//        }
//    }
}