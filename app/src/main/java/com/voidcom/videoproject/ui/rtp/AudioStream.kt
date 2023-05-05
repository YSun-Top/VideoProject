package com.voidcom.videoproject.ui.rtp

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class AudioStream @RequiresPermission(android.Manifest.permission.RECORD_AUDIO) constructor(
    callback: OnFrameDataCallback,
    audioParam: AudioParam
) {
    private var isMute = false
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

    fun startLive() {
        isLiving = true
        executor.submit(AudioTask())
    }

    fun stopLive() {
        isLiving = false
    }

    fun release() {
        audioRecord.release()
    }

    internal inner class AudioTask : Runnable {
        override fun run() {
            audioRecord.startRecording()
            val bytes = ByteArray(inputSamples)
            while (isLiving) {
                if (!isMute) {
                    val len = audioRecord.read(bytes, 0, bytes.size)
                    if (len > 0) {
                        mCallback.onAudioFrame(bytes)
                    }
                }
            }
            audioRecord.stop()
        }
    }

    /**
     * Setting mute or not
     *
     * @param isMute isMute
     */
    fun setMute(isMute: Boolean) {
        this.isMute = isMute
    }
}
