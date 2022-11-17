package com.voidcom.v_base.utils

import android.content.res.Resources
import android.content.res.Resources.Theme
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

fun Resources.getColorValue(@ColorRes id: Int, theme: Theme? = null): Int = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> getColor(id, theme)
    else -> @Suppress("DEPRECATION") getColor(id)
}

fun Resources.getDrawableObj(@DrawableRes id: Int, theme: Theme? = null): Drawable = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> getDrawable(id, theme)
    else -> @Suppress("DEPRECATION") getDrawable(id)
}