package com.chat.ui

import androidx.lifecycle.LiveData
import androidx.paging.PagedList

internal class PagedListSizeLiveData<T: Any>(
    private val pagedList: PagedList<T>
): LiveData<Int>() {
    private val pagedListCallback = object : PagedList.Callback() {
        override fun onChanged(position: Int, count: Int) {
            checkSize()
        }
        override fun onInserted(position: Int, count: Int) {
            checkSize()
        }
        override fun onRemoved(position: Int, count: Int) {
            checkSize()
        }
    }

    init {
        pagedList.addWeakCallback(pagedListCallback)
        checkSize()
    }

    private fun checkSize() {
        postValue(pagedList.size)
    }
}