package com.voidcom.v_base.utils.log

import android.util.Log

/**
 * Created by zhaokaiqiang on 15/11/18.
 */
object BaseLog {
    private const val MAX_LENGTH = 4000

    fun printDefault(type: LogType, tag: String, msg: String) {
        val length = msg.length
        val countOfSub = length / MAX_LENGTH
        if (countOfSub <= 0) {
            printSub(type, tag, msg)
            return
        }
        var index = 0
        for (i in 0 until countOfSub) {
            val sub = msg.substring(index, index + MAX_LENGTH)
            printSub(type, tag, sub)
            index += MAX_LENGTH
        }
        printSub(type, tag, msg.substring(index, length))
    }

    private fun printSub(type: LogType, tag: String, sub: String) {
        when (type) {
            LogType.D -> Log.d(tag, sub)
            LogType.I -> Log.i(tag, sub)
            LogType.W -> Log.w(tag, sub)
            LogType.E -> Log.e(tag, sub)
            LogType.A -> Log.wtf(tag, sub)
            else -> Log.v(tag, sub)
        }
    }
}