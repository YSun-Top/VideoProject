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
    private val decoderJni = FFmpegDecoderJni.newInstant
    private val mHandler = Handler(Looper.getMainLooper())

    override fun setDisPlay(holder: SurfaceHolder?, fileInfo: FileAttributes) {
        mHolder = holder
    }

    override fun setDataSource(path: String) {
        mHolder?.let { decoderJni.setDisplay(it.surface) }
        decoderJni.setDataSource(path)
    }

    override fun start() {
        decoderJni.setPlayState(1)
    }

    override fun pause() {
        decoderJni.setPlayState(2)
    }

    override fun seekTo(time: Int) {
        decoderJni.goSelectedTime(time)
    }

    override fun release() {
        decoderJni.setPlayState(5)
    }

    override fun isPlaying(): Boolean = decoderJni.mIsPlaying()

    override fun getPlayTimeIndex(type: Int): Long = if (type == 1) {
        decoderJni.getCurrentPosition()
    } else {
        decoderJni.getDuration()
    }
}