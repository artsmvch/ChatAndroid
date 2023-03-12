package com.chat.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources


fun Context.dp(dp: Int): Int {
    return dpFloat(resources.displayMetrics, dp.toFloat()).toInt()
}

fun dpFloat(metrics: DisplayMetrics, dp: Float): Float {
    val scale = metrics.densityDpi.toDouble() / DisplayMetrics.DENSITY_DEFAULT
    return (dp * scale).toFloat()
}

fun Context.resolveColorStateList(@AttrRes attrId: Int): ColorStateList? {
    val typedValue = TypedValue()
    if (!theme.resolveAttribute(attrId, typedValue, false)) {
        // Failed to resolve the attribute, returning null
        return null
    }
    return if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
        typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT
    ) {
        ColorStateList.valueOf(typedValue.data)
    } else {
        AppCompatResources.getColorStateList(this, typedValue.data)
    }
}

@ColorInt
fun Context.resolveColor(@AttrRes attrId: Int): Int {
    val list: ColorStateList = resolveColorStateList(attrId)
        ?: throw NullPointerException("Color not found")
    return list.defaultColor
}

fun Context.resolveDrawable(@AttrRes attrId: Int): Drawable? {
    val typedValue = TypedValue()
    if (!theme.resolveAttribute(attrId, typedValue, true)) {
        return null
    }
    return AppCompatResources.getDrawable(this, typedValue.resourceId)
}