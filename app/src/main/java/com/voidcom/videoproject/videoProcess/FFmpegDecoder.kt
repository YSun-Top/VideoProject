package com.voidcom.videoproject.videoProcess

import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import com.voidcom.ffmpeglib.FFmpegDecoderJni
import com.voidcom.videoproject.utils.FileAttributes

/**
 * Created by voidcom on 2022/3/28 21:58
 * Description:
 */
class FFmpegDecoder : VideoDecoder() {
    private val mHandler = Handler(Looper.getMainLooper())

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

    override fun isPlaying(): Boolean = FFmpegDecoderJni.newInstant.mIsPlaying()

    override fun getPlayTimeIndex(type: Int): Long = if (type == 1) {
        FFmpegDecoderJni.newInstant.getCurrentPosition()
    } else {
        FFmpegDecoderJni.newInstant.getDuration()
    }
}