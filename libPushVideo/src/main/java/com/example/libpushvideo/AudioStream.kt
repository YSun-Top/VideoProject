package com.example.libpushvideo

import android.media.AudioRecord
import androidx.annotation.RequiresPermission
import com.voidcom.v_base.utils.audioplayer.AudioParam
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class AudioStream @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
constructor(callback: OnFrameDataCallback, audioParam: AudioParam) {
    @set:Synchronized
    private var isMute = false

    @set:Synchronized
    private var isLiving = false
    private var minBufferSize = 0
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val audioRecord: AudioRecord
    private val mCallback: OnFrameDataCallback = callback

    init {
        mCallback.onAudioCodecInfo(audioParam.sampleRate, audioParam.numChannels)
        minBufferSize = AudioRecord.getMinBufferSize(
            audioParam.sampleRate,
            audioParam.channelConfig, audioParam.audioFormat
        ) * 2
        audioParam.audioFormat
        audioRecord = AudioRecord(
            audioParam.audioSource, audioParam.sampleRate,
            audioParam.channelConfig, audioParam.audioFormat, minBufferSize
        )
    }

    /**
     * 设置是否静音
     *
     * @param m isMute
     */
    fun setMute(m: Boolean) {
        //满足 1.设置为非静音模式 2.当前是静音模式(防止重复开启线程) 3.正在直播 三个状态时开启新的录音线程
        if (!m && this.isMute && isLiving) {
            executor.submit(audioRunnable)
        }
        this.isMute = m
    }


    fun startRecorder() {
        isLiving = true
        if (isMute) return
        executor.submit(audioRunnable)
    }

    fun stopRecorder() {
        isLiving = false
    }

    fun release() {
        audioRecord.release()
        executor.shutdown()
    }

    private val audioRunnable = Runnable {
        audioRecord.startRecording()
        val bytes = ByteArray(minBufferSize)
        var len: Int
        while (isLiving) {
            if (isMute) continue
            len = audioRecord.read(bytes, 0, bytes.size)
            if (len > 0) {
                mCallback.onAudioFrame(bytes)
            }
        }
        audioRecord.stop()
    }

    companion object {
        val TAG: String = AudioStream::class.java.simpleName
    }
}
