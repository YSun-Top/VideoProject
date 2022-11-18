package com.voidcom.v_base.utils

import android.util.TypedValue
import android.view.View
import com.voidcom.v_base.BaseApplication

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

internal fun View.setVisible(visible: Boolean) {
    visibility = if (visible)
        View.VISIBLE
    else
        View.GONE
}

fun dp2px(dpValue: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dpValue,
        BaseApplication.displayMetrics
    )
}