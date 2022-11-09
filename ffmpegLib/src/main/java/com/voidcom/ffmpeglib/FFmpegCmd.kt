package com.voidcom.ffmpeglib

class FFmpegCmd private constructor() {

    init {
        System.loadLibrary("ffmpegSDK")
    }

    external fun executeFF(cmdStr: String): Int

    external fun executeFFmpeg(cmdStr: Array<String>): Int

    fun executeFFmpeg(cmdStr: String): Int {
        return executeFFmpeg(cmdStr.split(" ").toTypedArray())
    }

    companion object {
        val getInstance: FFmpegCmd by lazy { FFmpegCmd() }
    }
}