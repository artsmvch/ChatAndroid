package com.chat.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class CustomLiveData<T> : LiveData<T>() {
    private val observerWrappers = CopyOnWriteArraySet<ObserverWrapper<in T>>()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        val wrapper = wrapObserver(observer)
        observerWrappers.add(wrapper)
        super.observe(owner, wrapper)
    }

    override fun observeForever(observer: Observer<in T>) {
        val wrapper = wrapObserver(observer)
        observerWrappers.add(wrapper)
        super.observeForever(wrapper)
    }

    override fun removeObserver(observer: Observer<in T>) {
        val wrapper = wrapObserver(observer)
        observerWrappers.remove(wrapper)
        super.removeObserver(wrapper)
    }

    override fun removeObservers(owner: LifecycleOwner) {
        // TODO: the owner is not considered here
        observerWrappers.clear()
        super.removeObservers(owner)
    }

    public override fun setValue(value: T) {
        observerWrappers.forEach {
            wrapper -> wrapper.onSetNewValue()
        }
        super.setValue(value)
    }

    abstract fun wrapObserver(observer: Observer<in T>): ObserverWrapper<T>

    interface ObserverWrapper<T>: Observer<T> {
        fun onSetNewValue()
        override fun hashCode(): Int
        override fun equals(other: Any?): Boolean
    }
}

internal class OneShotLiveData<T> : CustomLiveData<T>() {
    override fun wrapObserver(observer: Observer<in T>): ObserverWrapper<T> {
        return ObserverWrapperImpl(observer)
    }

    class ObserverWrapperImpl<T>(
        private val observer: Observer<in T>
    ): ObserverWrapper<T> {
        private val isPendingNewValue = AtomicBoolean(false)

        override fun onSetNewValue() {
            isPendingNewValue.set(true)
        }

        override fun onChanged(t: T) {
            if (isPendingNewValue.getAndSet(false)) {
                observer.onChanged(t)
            }
        }

        override fun hashCode(): Int {
            return observer.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is ObserverWrapperImpl<*> && observer == other.observer
        }
    }
}