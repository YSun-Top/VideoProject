package com.voidcom.ffmpeglib

class FFmpegCmd private constructor() {

    external fun executeFF(cmdStr: String): String

    external fun executeFFmpeg(cmdStr: Array<String>)

    companion object {
        private var cmd: FFmpegCmd? = null

        init {
            System.loadLibrary("ffmpegSDK")
        }

        fun getInstance(): FFmpegCmd {
            if (cmd == null) {
                cmd = FFmpegCmd()
            }
            return cmd as FFmpegCmd
        }
    }
}