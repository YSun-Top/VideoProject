package com.voidcom.ffmpeglib

/**
 *
 * @Description: java类作用描述
 * @Author: Void
 * @CreateDate: 2022/11/11 9:43
 * @UpdateDate: 2022/11/11 9:43
 */
class FFprobeCmd private constructor() {

    init {
        System.loadLibrary("ffmpegSDK")
    }

    external fun executeFFprobeArray(cmdStr: Array<String>): String?

    fun executeFFprobe(cmdStr: String): String? {
        return executeFFprobeArray(cmdStr.split(" ").toTypedArray())
    }

    companion object {
        val getInstance: FFprobeCmd by lazy { FFprobeCmd() }
    }
}