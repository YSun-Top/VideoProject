package com.voidcom.videoproject.model.videoFilter

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 */
interface PlayStateCallback {
    /**
     * 播放准备
     */
    fun onPrepared()

    /**
     * 播放结束
     */
    fun onCompletion()

    /**
     * 播放取消、终止
     */
    fun onPlayCancel()
}