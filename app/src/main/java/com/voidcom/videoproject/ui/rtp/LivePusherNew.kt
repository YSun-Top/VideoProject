package com.voidcom.videoproject.ui.rtp

import android.app.Activity
import android.util.Log
import android.view.SurfaceHolder
import android.view.TextureView
import androidx.annotation.RequiresPermission
import java.lang.ref.WeakReference

class LivePusherNew @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
constructor(
    val activity: Activity,
    videoParam: VideoParam,
    audioParam: AudioParam,
    view: TextureView,
    cameraType: CameraType
) : OnFrameDataCallback {
    private val ERROR_VIDEO_ENCODER_OPEN = 0x01
    private val ERROR_VIDEO_ENCODER_ENCODE = 0x02
    private val ERROR_AUDIO_ENCODER_OPEN = 0x03
    private val ERROR_AUDIO_ENCODER_ENCODE = 0x04
    private val ERROR_RTMP_CONNECT_SERVER = 0x05
    private val ERROR_RTMP_CONNECT_STREAM = 0x06
    private val ERROR_RTMP_SEND_PACKET = 0x07

    private var audioStream: AudioStream
    private var videoStream = VideoStreamNew(this, view, videoParam, WeakReference(activity))
    private var callback: LiveErrorCallback? = null

    init {
        nativeInit()
        audioStream = AudioStream(this, audioParam)
    }

    override fun getInputSamples(): Int {
        return 0
    }

    override fun onAudioFrame(pcm: ByteArray?) {
    }

    override fun onAudioCodecInfo(sampleRate: Int, channelCount: Int) {
    }

    override fun onVideoFrame(yuv: ByteArray, cameraType: Int) {
        nativePushVideo(yuv, cameraType)
    }

    override fun onVideoCodecInfo(width: Int, height: Int, frameRate: Int, bitrate: Int) {
    }

    fun setPreviewDisplay(holder: SurfaceHolder) {
        videoStream.setPreviewDisplay(holder)
    }

    fun startPush(path: String, callback: LiveErrorCallback) {
        this.callback = callback
        nativeStart(path)
        videoStream.startLive()
        audioStream.startLive()
    }

    fun stopPush() {
        videoStream.stopLive()
        audioStream.stopLive()
        nativeStop()
    }

    /**
     * setting mute
     *
     * @param isMute is mute or not
     */
    fun setMute(isMute: Boolean) {
        audioStream.setMute(isMute)
    }

    fun release() {
        videoStream.release()
        audioStream.release()
        nativeRelease()
    }

    fun switchCamera() {
        videoStream.switchCamera()
    }

    fun errorFromNative(errCode: Int) {
        //stop pushing stream
        stopPush()
        callback?.onError(
            when (errCode) {
                ERROR_VIDEO_ENCODER_OPEN -> "打开视频编码器失败"
                ERROR_VIDEO_ENCODER_ENCODE -> "视频编码失败"
                ERROR_AUDIO_ENCODER_OPEN -> "打开音频编码器失败"
                ERROR_AUDIO_ENCODER_ENCODE -> "音频编码失败"
                ERROR_RTMP_CONNECT_SERVER -> "RTMP连接服务器失败"
                ERROR_RTMP_CONNECT_STREAM -> "RTMP连接流失败"
                ERROR_RTMP_SEND_PACKET -> "RTMP发送数据包失败"
                else -> ""
            }
        )
    }

    private external fun nativeInit()

    private external fun nativePushVideo(yuv: ByteArray, cameraType: Int)

    private external fun nativeStart(path: String)

    private external fun nativeStop()

    private external fun nativeRelease()

    companion object {
        init {
            System.loadLibrary("libPushVideo")
        }
    }

    interface LiveErrorCallback {
        fun onError(msg: String)
    }
}

enum class CameraType {
    CAMERA1,
    CAMERA2
}