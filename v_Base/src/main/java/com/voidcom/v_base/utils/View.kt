package com.voidcom.v_base.utils

import android.view.View

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