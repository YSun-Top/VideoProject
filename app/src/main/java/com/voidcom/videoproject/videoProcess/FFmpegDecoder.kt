package com.voidcom.videoproject.videoProcess

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.SurfaceHolder
import com.voidcom.ffmpeglib.FFmpegDecoderJni
import com.voidcom.libsdkbase.JniCallback
import com.voidcom.v_base.utils.KLog
import com.voidcom.videoproject.model.videoFilter.PlayStateCallback
import com.voidcom.videoproject.utils.FileAttributes

/**
 * Created by voidcom on 2022/3/28 21:58
 * Description:
 */
class FFmpegDecoder(val callback: PlayStateCallback) : VideoDecoder(), JniCallback {
    private val TAG = FFmpegDecoder::class.java.simpleName
    private val mHandler = Handler(Looper.getMainLooper())

    init {
        FFmpegDecoderJni.newInstant.callback = this
        FFmpegDecoderJni.newInstant.initJni()
        FFmpegDecoderJni.newInstant.isPlayAudio(true)
    }

    override fun setDisPlay(holder: SurfaceHolder?, fileInfo: FileAttributes) {
        mHolder = holder
    }

    override fun setDataSource(path: String) {
        mHolder?.let { FFmpegDecoderJni.newInstant.setDisplay(it.surface) }
        FFmpegDecoderJni.newInstant.setDataSource(path)
    }

    override fun start() {
        FFmpegDecoderJni.newInstant.setPlayState(1)
    }

    override fun pause() {
        FFmpegDecoderJni.newInstant.setPlayState(2)
    }

    override fun seekTo(time: Int) {
        FFmpegDecoderJni.newInstant.goSelectedTime(time)
    }

    override fun release() {
        FFmpegDecoderJni.newInstant.setPlayState(5)
    }

    override fun isPlaying(): Boolean = FFmpegDecoderJni.newInstant.isPlaying()

    /**
     * 获取播放进度
     * 时间单位：ms
     */
    override fun getPlayTimeIndex(type: Int): Long = if (type == 0) {
        FFmpegDecoderJni.newInstant.getCurrentPosition()
    } else {
        FFmpegDecoderJni.newInstant.getDuration()
    }

    fun setFilter(value: String) {
        if (TextUtils.isEmpty(value)) return
        KLog.w(TAG, "设置滤镜：$value")
        mHandler.removeCallbacksAndMessages(null)
        mHandler.postDelayed({
            FFmpegDecoderJni.newInstant.setFilter(value)
        }, 1000)
    }

    override fun onPlayStatusCallback(status: Int) {
        mHandler.post {
            when (status) {
                0 -> callback.onPrepared()
                3 -> callback.onCompletion()
                4 -> callback.onPlayCancel()
                7 -> {
                    isFilterFinishChange = true
                    KLog.d(msg = "滤镜切换成功")
                }
            }
        }
    }

    override fun onErrorCallback(errorCode: Int, msg: String) {
        when (errorCode) {
            0x02 -> isFilterFinishChange = true
        }
    }
}