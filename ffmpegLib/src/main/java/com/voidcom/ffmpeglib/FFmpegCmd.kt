package com.voidcom.ffmpeglib

import android.util.Log
import com.voidcom.ffmpeglib.callback.CommandModeCallback

class FFmpegCmd private constructor() {
    private var callback: CommandModeCallback? = null

    init {
        System.loadLibrary("ffmpegSDK")
    }

    private external fun executeFFCallback(cmdStr: Array<String>)

    fun executeFFmpeg(cmdStr: String, callback: CommandModeCallback? = null) {
        this.callback = callback
        Log.d("-FFmpegCmd-", cmdStr)
        executeFFCallback(cmdStr.split(" ").toTypedArray())
    }

    fun onFinish() {
        callback?.onFinish()
    }

    fun onError(msg: String) {
        callback?.onError(msg)
    }

    companion object {
        val getInstance: FFmpegCmd by lazy { FFmpegCmd() }
    }
}