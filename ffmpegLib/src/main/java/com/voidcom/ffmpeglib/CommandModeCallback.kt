package com.voidcom.ffmpeglib

/**
 *
 * @Description: 命令模式回调
 * @Author: Void
 * @CreateDate: 2022/11/16 15:11
 * @UpdateDate: 2022/11/16 15:11
 */
interface CommandModeCallback {

    fun onFinish()

    fun onError(msg: String)
}