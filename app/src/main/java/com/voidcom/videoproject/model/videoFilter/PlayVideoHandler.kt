package com.voidcom.videoproject.model.videoFilter

import android.view.SurfaceHolder
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.log.LogUtils
import com.voidcom.videoproject.utils.FileAttributes
import com.voidcom.videoproject.videoProcess.FFmpegDecoder

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 */
class PlayVideoHandler : SurfaceHolder.Callback, PlayStateCallback {

    private val fFmpegDecoder: FFmpegDecoder by lazy { FFmpegDecoder(this) }
    private val fileInfo: FileAttributes by lazy { FileAttributes() }

    override fun surfaceCreated(holder: SurfaceHolder) {
        fFmpegDecoder.setDisPlay(holder,fileInfo)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        fFmpegDecoder.setDisPlay(null,fileInfo)
    }

    override fun onPrepared() {
        plStart()
    }

    override fun onCompletion() {
    }

    override fun onPlayCancel() {
    }

    fun setDataPath(path: String) {
        if (!fileInfo.initData(path)) {
            LogUtils.e(AppCode.log_videoProcess, "文件信息初始化失败,请检查文件是否有效！path:$path")
            return
        }
        fFmpegDecoder.setDataSource(path)
    }

    fun release(){
        fFmpegDecoder.release()
    }

    @Synchronized
    fun plStart(){
        fFmpegDecoder.start()
    }

    @Synchronized
    fun plPause(){
        fFmpegDecoder.pause()
    }
}