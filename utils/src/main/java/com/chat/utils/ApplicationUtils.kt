package com.chat.utils

import android.content.Context

fun Context.getFirstInstallTime(): Long? {
    return runCatching { packageManager.getPackageInfo(packageName, 0).firstInstallTime }
        .getOrNull()
}