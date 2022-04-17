package com.voidcom.ffmpeglib

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.voidcom.libsdkbase.JniCallback

/**
 * Created by voidcom on 2022/3/28 22:20
 * Description:
 *
 */
//const val TAG = "FFMPEG_DECODER_JNI"

class FFmpegDecoderJni private constructor() {
    private var audioTrack: AudioTrack? = null
    var callback: JniCallback? = null

    /**
     * 创建音轨
     * @param sampleRate 采样率
     * @param channels   频道
     */
    fun createAudioTrack(sampleRate: Int, channels: Int) {
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val channelConfig: Int = when (channels) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> AudioFormat.CHANNEL_OUT_STEREO
        }
        val bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSizeInBytes)
                .build()
        } else {
            @Suppress("DEPRECATION")
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat,
                bufferSizeInBytes, AudioTrack.MODE_STREAM
            )
        }
    }

    fun playAudio() {
        audioTrack?.play()
    }

    fun stopAudio() {
        audioTrack?.stop()
    }

    fun releaseJni() {
        audioTrack?.release()
    }

    /**
     * c层播放状态回调
     * 注: 由c调用改方法
     * @param status 0=Prepared
     */
    fun jniPlayStatusCallback(status: Int) {
        callback?.onPlayStatusCallback(status)
    }

    /**
     * c层错误回调
     * 注: 由c调用改方法
     * 详细请查看错误定义文件：src/main/cpp/ErrorCodeDefine.h
     * @param errorCode
     */
    fun jniErrorCallback(errorCode: Int, msg: String) {
        callback?.onErrorCallback(errorCode, msg)
    }

    fun writeAudioData(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int): Int =
        audioTrack?.write(audioData, offsetInBytes, sizeInBytes)
            ?: AudioTrack.ERROR_INVALID_OPERATION

    external fun initJni()

    external fun setDisplay(surface: Any)

    external fun setDataSource(vPath: String)

    external fun getCurrentPosition(): Long

    external fun getDuration(): Long

    external fun goSelectedTime(t: Int)

    external fun isPlaying(): Boolean

    external fun setPlayState(status: Int)

    external fun setFilter(value: String)

    external fun isPlayAudio(boolean: Boolean)

    companion object {
        init {
            System.loadLibrary("ffmpegSDK")
        }

        val newInstant: FFmpegDecoderJni by lazy { FFmpegDecoderJni() }
    }
}