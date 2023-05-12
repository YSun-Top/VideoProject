package com.example.libpushvideo

import android.app.Activity
import android.view.SurfaceHolder
import android.view.TextureView
import androidx.annotation.RequiresPermission
import com.voidcom.v_base.utils.audioplayer.InnerAudioRecorder
import java.lang.ref.WeakReference

class LivePusherNew @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
constructor(
    activity: Activity,
    videoParam: VideoParam,
    audioParam: AudioParam,
    view: TextureView
) : OnFrameDataCallback {

    private var audioStream: AudioStream
    private var videoStream = VideoStreamNew(this, view, videoParam, WeakReference(activity))

    init {
        NativeLivePusherHelper.getInstant().nativeInit()
        InnerAudioRecorder.instance().startRecorder()
        audioStream = AudioStream(this, audioParam)
        startPreview()
    }

    override fun getInputSamples(): Int {
        return 0
    }

    override fun onAudioFrame(pcm: ByteArray?) {
    }

    override fun onAudioCodecInfo(sampleRate: Int, channelCount: Int) {
        NativeLivePusherHelper.getInstant().nativeSetAudioCodecInfo(sampleRate, channelCount)
    }

    override fun onVideoFrame(yuv: ByteArray, cameraType: Int) {
        NativeLivePusherHelper.getInstant().nativePushVideo(yuv, cameraType)
    }

    override fun onVideoCodecInfo(width: Int, height: Int, frameRate: Int, bitrate: Int) {
        NativeLivePusherHelper.getInstant()
            .nativeSetVideoCodecInfo(width, height, frameRate, bitrate)
    }

    fun setPreviewDisplay(holder: SurfaceHolder) {
        videoStream.setPreviewDisplay(holder)
    }

    fun startPush(path: String, callback: NativeLivePusherHelper.LiveErrorCallback) {
        NativeLivePusherHelper.getInstant().nativeStart(path)
        NativeLivePusherHelper.getInstant().setCallback(callback)
        videoStream.startLive()
        audioStream.startLive()
    }

    fun stopPush() {
        videoStream.stopLive()
        audioStream.stopLive()
        NativeLivePusherHelper.getInstant().nativeStop()
    }

    fun startPreview() {
        videoStream.startPreview()
    }

    fun stopPreview() {
        videoStream.stopPreview()
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
        NativeLivePusherHelper.getInstant().nativeRelease()
    }

    /**
     * 内部实现方式就是：停止摄像头然后判断当前摄像头ID，使用新的摄像头ID启动摄像头
     */
    fun switchCamera() {
        videoStream.switchCamera()
    }
}
