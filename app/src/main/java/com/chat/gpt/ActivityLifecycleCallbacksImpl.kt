package com.chat.gpt

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import java.util.concurrent.CopyOnWriteArrayList

internal class ActivityLifecycleCallbacksImpl: ActivityLifecycleCallbacks {
    private val createdActivities = CopyOnWriteArrayList<Activity>()

    val lastCreatedActivity: Activity? get() = createdActivities.lastOrNull()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        createdActivities.add(activity)
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity.isFinishing) {
            createdActivities.remove(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        createdActivities.remove(activity)
    }
}