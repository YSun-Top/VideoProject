package com.voidcom.videoproject.videoProcess

/**
 * Created by Void on 2020/8/25 17:00
 * 视频播放状态监听
 */
interface PlayStateListener {
    fun onPlayStart()
    fun onPlayPaused()
    fun onPlayStop()
    fun onPlayEnd()
    fun onPlayRelease()

    /**
     * 更新播放时间
     * @param time 时间,单位ms
     */
    fun onPlayTime(time:Long)
}