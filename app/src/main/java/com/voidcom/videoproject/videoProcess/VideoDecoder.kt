package com.voidcom.videoproject.videoProcess

import android.view.SurfaceHolder
import com.voidcom.videoproject.utils.FileAttributes

/**
 * Created by Void on 2020/9/10 14:34
 *
 */
abstract class VideoDecoder {

    protected var mHolder: SurfaceHolder? = null
    var isFilterFinishChange = false //滤镜切换状态

    //是否已经被释放
    protected var isRelease = false

    abstract fun setDisPlay(holder: SurfaceHolder?, fileInfo: FileAttributes)

    abstract fun setDataSource(path: String)
    abstract fun start()
    abstract fun pause()

    /**
     * 跳转到目标时间
     * @param time 单位：ms
     */
    abstract fun seekTo(time: Int)
    abstract fun release()

    /**
     * 播放状态
     * @return true播放中
     */
    abstract fun isPlaying(): Boolean

    /**
     * 获取播放时间数据
     * @param type 1当前播放进度 2总播放时长
     */
    abstract fun getPlayTimeIndex(type: Int): Long
}