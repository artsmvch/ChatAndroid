package com.chat.ui

import androidx.appcompat.widget.Toolbar

interface FragmentToolbarCallbacks {
    fun onToolbarAttached(toolbar: Toolbar)
    fun onToolbarDetached(toolbar: Toolbar)
}