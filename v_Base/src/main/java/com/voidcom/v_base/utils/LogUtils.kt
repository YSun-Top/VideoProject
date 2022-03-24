package com.voidcom.v_base.utils

import android.text.TextUtils
import android.util.Log
import com.voidcom.v_base.BuildConfig

/**
 * Created by Void on 2018/3/23.
 * 开发：Void
 * 日志打印工具类
 */

object LogUtils {
    private val is_Debug = BuildConfig.DEBUG//全局是否打印日志
    private val TAG = "Debuger"

    fun v(tag: String = TAG, msg: String) {
        if (TextUtils.isEmpty(tag) || !is_Debug) return
        Log.v(tag, msg)
    }

    fun i(tag: String = TAG, msg: String) {
        if (TextUtils.isEmpty(tag) || !is_Debug) return
        Log.i(tag, msg)
    }

    fun d(tag: String = TAG, msg: String) {
        if (TextUtils.isEmpty(tag) || !is_Debug) return
        Log.d(tag, msg)
    }

    fun w(tag: String = TAG, msg: String) {
        if (TextUtils.isEmpty(tag) || !is_Debug) return
        Log.w(tag, msg)
    }

    fun e(tag: String = TAG, msg: String) {
        if (TextUtils.isEmpty(tag) || !is_Debug) return
        Log.e(tag, msg)
    }

    /**
     * 使用日志打印调试信息，因为log在实现上有每条信息4k字符长度限制
     * 所以这里用分节方式打印
     *
     * @param tag
     * @param msg
     */
    fun showLog(tag: String = TAG, msg: String) {
        if (TextUtils.isEmpty(tag) || !is_Debug) return
        val msgLength = msg.length
        var start = 0
        var end = 2000
        for (i in 0 until msgLength) {
            if (msgLength > end) {
                w(tag + "_____" + i + ":", msg.substring(start, end))
                start = end
                end += 2000
            } else {
                w(tag + "_____" + i + ":", msg.substring(start, msgLength))
            }
        }
    }
}