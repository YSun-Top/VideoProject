package com.example.libpushvideo

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.voidcom.v_base.utils.KLog
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class AudioStream @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
constructor(callback: OnFrameDataCallback, audioParam: AudioParam) {
    @set:Synchronized
    private var isMute = false

    @set:Synchronized
    private var isLiving = false
    private val inputSamples: Int
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val audioRecord: AudioRecord
    private val mCallback: OnFrameDataCallback = callback

    init {
        val channelConfig: Int = if (audioParam.numChannels == 2) {
            AudioFormat.CHANNEL_IN_STEREO
        } else {
            AudioFormat.CHANNEL_IN_MONO
        }
        mCallback.onAudioCodecInfo(audioParam.sampleRate, audioParam.numChannels)
        inputSamples = mCallback.getInputSamples() * 2
        val minBufferSize = AudioRecord.getMinBufferSize(
            audioParam.sampleRate,
            channelConfig, audioParam.audioFormat
        ) * 2
        audioParam.audioFormat
        val bufferSizeInBytes = minBufferSize.coerceAtLeast(inputSamples)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, audioParam.sampleRate,
            channelConfig, audioParam.audioFormat, bufferSizeInBytes
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


    fun startLive() {
        isLiving = true
        if (isMute) return
        executor.submit(audioRunnable)
    }

    fun stopLive() {
        isLiving = false
    }

    fun release() {
        audioRecord.release()
        executor.shutdown()
    }

    private val audioRunnable = Runnable {
        audioRecord.startRecording()
        val bytes = ByteArray(inputSamples)
        var len: Int
        while (isLiving) {
            if (isMute) {
                KLog.d(TAG, )
                break
            }
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
