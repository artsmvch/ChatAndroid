package com.chat.firebase

import androidx.annotation.GuardedBy
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirebaseRemoteConfigCache {
    private const val STRICT_ACTIVATION = false

    private val valueFlowsLock = Any()
    @GuardedBy("valueFlowsLock")
    private val valueFlows = HashMap<String, MutableStateFlow<String?>>()

    private val valueScope: CoroutineScope get() = GlobalScope

    private val taskExecutor: Executor by lazy { Executors.newFixedThreadPool(2) }

    private fun obtainValueFlow(key: String): Flow<String?> {
        return synchronized(valueFlowsLock) {
            var flow = valueFlows[key]
            if (flow == null) {
                val newFlow = MutableStateFlow<String?>(null)
                valueFlows[key] = newFlow
                valueScope.launch {
                    val value = getRemoteConfigValue(key)
                    newFlow.emit(value?.asString())
                }
                flow = newFlow
            }
            return@synchronized flow
        }
    }

    private suspend fun getRemoteConfigValue(key: String): FirebaseRemoteConfigValue? {
        return try {
            getActivatedConfig(
                fetch = true,
                minimumFetchIntervalInSeconds = 3000L
            ).getValue(key)
        } catch (e: Throwable) {
            null
        }
    }

    private suspend fun getActivatedConfig(fetch: Boolean, minimumFetchIntervalInSeconds: Long? = null): FirebaseRemoteConfig {
        return suspendCancellableCoroutine { unsafeContinuation ->
            val continuation = SafeContinuation(unsafeContinuation)
            try {
                val configInstance = FirebaseRemoteConfig.getInstance()
                val task: Task<Boolean> = if (fetch) {
                    if (minimumFetchIntervalInSeconds != null && minimumFetchIntervalInSeconds >= 0L) {
                        val taskContinuation =
                            SuccessContinuation<Void, Boolean> { configInstance.activate() }
                        configInstance.fetch(minimumFetchIntervalInSeconds)
                            .onSuccessTask(taskExecutor, taskContinuation)
                    } else {
                        configInstance.fetchAndActivate()
                    }
                } else {
                    configInstance.activate()
                }
                task.addOnFailureListener {
                    continuation.resumeWithException(it)
                }
                task.addOnCompleteListener { _task ->
                    if (_task.isSuccessful) {
                        val isActivated = _task.result
                        if (!STRICT_ACTIVATION || isActivated) {
                            continuation.resume(FirebaseRemoteConfig.getInstance())
                        } else {
                            val err: Exception =
                                IllegalStateException("Failed to activate Firebase config instance")
                            continuation.resumeWithException(err)
                        }
                    } else {
                        val err: Exception = _task.exception
                            ?: IllegalStateException("Task is not successful but the exception is null")
                        continuation.resumeWithException(err)
                    }
                }
            } catch (e: Throwable) {
                continuation.resumeWithException(e)
            }
        }
    }

    fun getString(key: String): Flow<String?> {
        return obtainValueFlow(key)
    }
}