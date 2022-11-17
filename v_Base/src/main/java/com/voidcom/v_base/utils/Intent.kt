package com.voidcom.v_base.utils

import android.content.Intent
import android.os.Build
import android.os.Parcelable

/**
 *
 * @Description: java类作用描述
 * @Author: Void
 * @CreateDate: 2022/11/15 13:14
 * @UpdateDate: 2022/11/15 13:14
 */
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}