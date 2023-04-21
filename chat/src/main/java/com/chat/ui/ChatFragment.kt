package com.chat.ui

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.chat.utils.SystemBarUtils
import com.chat.utils.resolveColor
import com.chat.utils.resolveStyleRes
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch


internal class ChatFragment : Fragment() {
    private var toolbar: MaterialToolbar? = null
    private var editText: EditText? = null
    private var sendButton: SendButton? = null
    private var messageListView: RecyclerView? = null
    private var messageAdapter: MessageAdapter? = null
    private var suggestionsChipGroup: ChipGroup? = null
    private var multiSelectionActionMode: ActionMode? = null

    // Colors
    @ColorInt
    private var statusBarColor: Int? = null
    @ColorInt
    private var actionModeBackgroundColor: Int? = null

    private val viewModel: ChatViewModel by lazy {
        val factory = ChatViewModelFactory(requireContext())
        val provider = ViewModelProvider(this, factory)
        provider[ChatViewModel::class.java]
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        statusBarColor = context.resolveColor(android.R.attr.statusBarColor)
        context.resolveStyleRes(com.google.android.material.R.attr.actionModeStyle)?.let { styleId ->
            val themedContext = androidx.appcompat.view.ContextThemeWrapper(context, styleId)
            actionModeBackgroundColor = themedContext.resolveColor(com.google.android.material.R.attr.background)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageView>(R.id.background).also { imageView ->
            ChatBackgroundLoader.load(imageView)
        }

        toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.speaker -> viewModel.onSpeakerClick()
                }
                false
            }
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

        val adapter = MessageAdapter(
            onItemClickListener = object : OnItemClickListener {
                override fun onItemClick(item: Message, itemView: View) {
                    showContextMenu(item, itemView)
                }
            },
            multiSelectionModeListener = object : MultiSelectionModeListener {
                override fun onStartMultiSelectionMode() {
                    startMultiSelectionActionMode()
                }
                override fun onItemSelectionChanged(item: Message, isSelected: Boolean) {
                    updateMultiSelectionActionMode(item, isSelected)
                }
                override fun onStopMultiSelectionMode() {
                    stopMultiSelectionActionMode()
                }
            }
        )
        messageAdapter = adapter
        messageListView = view.findViewById<RecyclerView>(R.id.messages_list).apply {
            layoutManager = MessageLayoutManager(view.context).apply {
                stackFromEnd = true
            }
            // addItemDecoration(MessageMarginItemDecoration())
            this.adapter = adapter
        }

        suggestionsChipGroup = view.findViewById(R.id.suggestions)

        observeViewModel(viewLifecycleOwner)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toolbar = null
        editText = null
        sendButton = null
        messageListView = null
        messageAdapter = null
        suggestionsChipGroup = null
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

        suggestions.observe(owner) { suggestions ->
            setSuggestions(suggestions)
        }

        error.observe(owner) { error ->
            error?.also(::showError)
        }

        clearInputFieldEvent.observe(owner) {
            editText?.text = null
        }

        copyMessagesEvent.observe(owner) { messages ->
            lifecycleScope.launch {
                context?.copyMessagesToClipboard(messages)
                context?.also {
                    Toast.makeText(it, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                }
            }
        }

        shareMessagesEvent.observe(owner) { messages ->
            lifecycleScope.launch {
                context?.shareMessages(messages)
            }
        }

        deleteMessagesConfirmationEvent.observe(owner) { messages ->
            askConfirmationForMessageDeletion(messages)
        }

        closeContextMenuEvent.observe(owner) {
            multiSelectionActionMode?.finish()
        }

        isSpeakerMuted.observe(owner) { isMuted ->
            toolbar?.menu?.findItem(R.id.speaker)?.also { item ->
                item.setIcon(
                    if (isMuted) R.drawable.ic_speaker_muted_24 else R.drawable.ic_speaker_24
                )
            }
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
            viewModel.onSendMessageClick(text)
        }
    }

    private fun showError(error: Throwable) {
        val context = this.context ?: return
        val message = error.message.orEmpty().ifBlank { error.toString() }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            stopMultiSelectionActionMode()
        }
    }

    private fun startMultiSelectionActionMode() {
        if (multiSelectionActionMode != null) {
            // TODO: check if the current action mode is active
            return
        }
        val activity = this.activity as? AppCompatActivity ?: return
        val callback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.context_menu_messages, menu)
                actionModeBackgroundColor?.also(::setStatusBarColor)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.copy -> {
                        viewModel.onCopyMessagesClick(messageAdapter?.getSelectedItems().orEmpty())
                    }
                    R.id.share -> {
                        viewModel.onShareMessagesClick(messageAdapter?.getSelectedItems().orEmpty())
                    }
                    R.id.delete -> {
                        viewModel.onDeleteMessagesClick(messageAdapter?.getSelectedItems().orEmpty())
                    }
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                messageAdapter?.stopMultiSelectionMode()
                multiSelectionActionMode = null
                statusBarColor?.also(::setStatusBarColor)
            }
        }
        val actionMode = activity.startSupportActionMode(callback)
        multiSelectionActionMode = actionMode
    }

    private fun updateMultiSelectionActionMode(item: Message, isSelected: Boolean) {
        val selectedItemCount = messageAdapter?.getSelectedItemCount() ?: 0
        multiSelectionActionMode?.title = selectedItemCount.toString()
    }

    private fun stopMultiSelectionActionMode() {
        multiSelectionActionMode?.finish()
        multiSelectionActionMode = null
    }

    private fun setStatusBarColor(@ColorInt color: Int) {
        val activity = this.activity ?: return
        SystemBarUtils.setStatusBarColor(activity, color)
    }

    private fun askConfirmationForMessageDeletion(messages: Collection<Message>) {
        val context = this.context ?: return
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.delete_messages)
            .setMessage(R.string.delete_messages_confirmation)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                viewModel.onMessageDeletionConfirmed(messages)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                viewModel.onMessageDeletionDeclined(messages)
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }

    private fun showContextMenu(item: Message, itemView: View) {
        val popup = PopupMenu(itemView.context, itemView)
        popup.inflate(R.menu.menu_message)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.copy -> viewModel.onCopyMessageClick(item)
                R.id.share -> viewModel.onShareMessageClick(item)
                R.id.delete -> viewModel.onDeleteMessageClick(item)
            }
            true
        }
        popup.show()
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

    private fun setSuggestions(suggestions: List<String>?) {
        val chipGroup = suggestionsChipGroup ?: return
        chipGroup.removeAllViews()
        suggestions?.forEach { text ->
            val chip = Chip(chipGroup.context)
            chip.setEnsureMinTouchTargetSize(false)
            chip.text = text
            chip.setOnClickListener {
                viewModel.onSuggestionClick(text)
            }
            chipGroup.addView(chip)
        }
        chipGroup.isVisible = !suggestions.isNullOrEmpty()
    }
}