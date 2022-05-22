package com.voidcom.v_base.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 *
 */
object ContextUtils {

    fun getActivityFromContext(mContext: Context): Activity? {
        var context: Context = mContext
        while (mContext !is Activity && mContext is ContextWrapper) {
            context = mContext.baseContext
        }
        return if (context is Activity) context else null
    }
}