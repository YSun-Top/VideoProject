package com.voidcom.v_base.utils

/**
 * Created by Void on 2020/9/3 17:40
 *
 */
object TimeUtils {
    /**
     * 格式化时间戳
     * 时间戳->12:10:06
     * @param t 时间戳(ms)
     */
    @JvmStatic
    fun formatTimeS(t: Long): String {
        var seconds: Long = if (t < 0) 0 else t / 1000
        if (t % 1000 > 0) seconds++
        val sb = StringBuffer()
        if (seconds > 3600) {
            val temp = (seconds / 3600).toInt()
            sb.append(if (seconds / 3600 < 10) "0$temp:" else "$temp:")
            formatSeconds(seconds, seconds % 3600 / 60, sb)
        } else {
            formatSeconds(seconds, seconds % 3600 / 60, sb)
        }
        return sb.toString()
    }

    private fun formatSeconds(seconds: Long, temp: Long, sb: StringBuffer) {
        sb.append(if (temp < 10) "0$temp:" else "$temp:")
        val tmp: Int = (seconds % 3600 % 60).toInt()
        sb.append(if (tmp < 10) "0$tmp" else "" + tmp)
    }
}