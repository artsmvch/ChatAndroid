@file:Suppress("FunctionName")

package com.chat.firebase

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

private class SafeContinuationImpl<T>(
    private val delegate: Continuation<T>
): Continuation<T> {
    private val resumedRef = AtomicBoolean(false)

    override val context: CoroutineContext = delegate.context

    override fun resumeWith(result: Result<T>) {
        if (!resumedRef.getAndSet(true)) {
            delegate.resumeWith(result)
        }
    }
}

internal fun <T> SafeContinuation(delegate: Continuation<T>): Continuation<T> {
    return SafeContinuationImpl(delegate)
}