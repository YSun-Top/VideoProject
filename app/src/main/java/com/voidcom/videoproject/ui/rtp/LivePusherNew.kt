package com.voidcom.videoproject.ui.rtp

import android.app.Activity
import android.view.SurfaceHolder
import android.view.TextureView
import androidx.annotation.RequiresPermission
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.PermissionsUtils
import java.lang.ref.WeakReference

class LivePusherNew @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
constructor(
    val activity: Activity,
    videoParam: VideoParam,
    audioParam: AudioParam,
    view: TextureView,
    cameraType: CameraType
) : OnFrameDataCallback {
    private var audioStream: AudioStream
    private var videoStream = VideoStreamNew(this, view, videoParam, WeakReference(activity))

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

    fun startPush(path: String) {
        nativeStart(path)
        videoStream.startLive()
        audioStream.startLive()
    }

    fun stopPush() {
        videoStream.stopLive()
        audioStream.stopLive()
        nativeStop()
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
}

enum class CameraType {
    CAMERA1,
    CAMERA2
}