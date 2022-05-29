package com.voidcom.videoproject.model.videoFilter

import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.KLog
import com.voidcom.videoproject.utils.FileAttributes
import com.voidcom.videoproject.videoProcess.FFmpegDecoder
import com.voidcom.videoproject.videoProcess.PlayStateListener

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 */
class PlayVideoHandler : SurfaceHolder.Callback,
    PlayStateCallback {
    private val mHandler = Handler(Looper.getMainLooper())
    private val fFmpegDecoder: FFmpegDecoder by lazy { FFmpegDecoder(this) }
    private val fileInfo: FileAttributes by lazy { FileAttributes() }
    private var listenerThread: ListenerPlayTimeThread? = null
    private var isReadyPlay = false
    lateinit var listener: PlayStateListener

    override fun surfaceCreated(holder: SurfaceHolder) {
        fFmpegDecoder.setDisPlay(holder, fileInfo)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        fFmpegDecoder.setDisPlay(null, fileInfo)
    }

    override fun onPrepared() {
        plStart()
        isReadyPlay = true
        if (listenerThread == null) {
            listenerThread = ListenerPlayTimeThread()
            listenerThread?.start()
        }
        listenerThread?.isStop = false
        listener.onPlayStart()
    }

    override fun onCompletion() {
        listener.onPlayEnd()
        stopTimeUpdateThread()
    }

    override fun onPlayCancel() {
    }

    fun setDataPath(path: String) {
        if (!fileInfo.initData(path)) {
            KLog.e(AppCode.log_videoProcess, "文件信息初始化失败,请检查文件是否有效！path:$path")
            return
        }
        fFmpegDecoder.setDataSource(path)
    }

    fun release() {
        fFmpegDecoder.release()
    }

    /**
     * 是否准备好播放
     */
    fun isReadyPlay() = isReadyPlay

    //region 播放控制或播放信息获取
    @Synchronized
    fun plStart() {
        fFmpegDecoder.start()
    }

    @Synchronized
    fun plPause() {
        fFmpegDecoder.pause()
        listener.onPlayPaused()
    }

    @Synchronized
    fun isPlaying(): Boolean = fFmpegDecoder.isPlaying()

    @Synchronized
    fun getCurrentTime(): Long = fFmpegDecoder.getPlayTimeIndex(0)

    @Synchronized
    fun getMaxTime(): Long = fFmpegDecoder.getPlayTimeIndex(1)
    //endregion

    //region -----播放时间更新线程控制-----
    fun stopTimeUpdateThread() {
        isReadyPlay = false
        listenerThread?.isStop = true
        listenerThread = null
    }

    fun setFilterValue(str: String) {
        if (!fFmpegDecoder.isFilterFinishChange) return
        fFmpegDecoder.isFilterFinishChange = false
        fFmpegDecoder.setFilter(str)
    }

    /**
     * 播放时间更新线程
     */
    inner class ListenerPlayTimeThread : Thread() {
        var isStop = false //是否停止播放，不包括暂停
        override fun run() {
            super.run()
            while (isReadyPlay) {
                if (isStop) break
                sleep(300)
                if (!isPlaying()) continue
                mHandler.post { listener.onPlayTime(getCurrentTime()) }
            }
        }
    }
    //endregion
}