package com.voidcom.ffmpeglib

import android.util.Log

class FFmpegCmd private constructor() {

    init {
        System.loadLibrary("ffmpegSDK")
    }

    external fun executeFF(cmdStr: String): Int

    external fun executeFFmpeg(cmdStr: Array<String>): Int

    fun executeFFmpeg(cmdStr: String): Int {
        Log.d("-------",cmdStr)
        return executeFFmpeg(cmdStr.split(" ").toTypedArray())
    }

    companion object {
        val getInstance: FFmpegCmd by lazy { FFmpegCmd() }
    }
}