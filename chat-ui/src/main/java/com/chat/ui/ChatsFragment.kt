package com.chat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks

class ChatsFragment : Fragment() {
    private val viewModel: ChatsViewModel by lazy {
        val factory = ChatsViewModelFactory(requireContext())
        val provider = ViewModelProvider(this, factory)
        provider[ChatsViewModel::class.java]
    }

    private var drawerLayout: DrawerLayout? = null
    private var navView: NavigationView? = null
    private var toolbar: Toolbar? = null

    private val fragmentLifecycleCallbacks = object : FragmentLifecycleCallbacks() {
        override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
            if (f is FragmentToolbarCallbacks) {
                f.onToolbarAttached(toolbar!!)
            }
        }

        override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
            if (f is FragmentToolbarCallbacks) {
                f.onToolbarDetached(toolbar!!)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_chats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = view.findViewById(R.id.toolbar)
        navView = view.findViewById(R.id.nav_view)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        drawerLayout = view.findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            requireActivity(), drawerLayout, toolbar,
            R.string.nav_drawer_open, R.string.nav_drawer_close
        )
        drawerLayout?.addDrawerListener(toggle)
        toggle.syncState()

        observeViewModel(viewLifecycleOwner)

        selectChat(chatId = null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toolbar= null
        drawerLayout = null
        navView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        childFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
    }

    private fun observeViewModel(owner: LifecycleOwner) = with(viewModel) {
        chatInfoList.observe(owner) { chats ->
            navView?.menu?.also { menu ->
                menu.clear()
                menu.add(requireContext().getString(R.string.new_chat)).setOnMenuItemClickListener {
                    selectChat(chatId = null)
                    true
                }
                for (chat in chats) {
                    // TODO: optimize
                    menu.add(chat.messagePreview).setOnMenuItemClickListener {
                        selectChat(chat.chatId)
                        true
                    }
                }
            }
        }
    }

    private fun selectChat(chatId: String?) {
        drawerLayout?.closeDrawers()
        openChat(chatId)
    }

    private fun openChat(chatId: String?) {
        val fragment = ChatFragment.newFragment(chatId)
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()
    }
}