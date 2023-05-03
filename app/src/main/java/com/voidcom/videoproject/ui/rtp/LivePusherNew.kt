package com.voidcom.videoproject.ui.rtp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.SurfaceHolder
import android.view.TextureView
import androidx.core.app.ActivityCompat
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.PermissionsUtils
import java.lang.ref.WeakReference

class LivePusherNew(
    val activity: Activity,
    videoParam: VideoParam,
    audioParam: AudioParam,
    view: TextureView,
    cameraType: CameraType
) : AudioStream.OnFrameDataCallback {
    private var audioStream: AudioStream? = null
    private var videoStream = VideoStreamNew(this, view, videoParam, WeakReference(activity))

    init {
//        native_init()
        if (PermissionsUtils.checkPermission(activity,AppCode.requestRecordAudio).isEmpty()) {
            audioStream = AudioStream(this, audioParam)
        }
    }

    override fun getInputSamples(): Int {
        return 0
    }

    override fun onAudioFrame(pcm: ByteArray?) {
    }

    override fun onAudioCodecInfo(sampleRate: Int, channelCount: Int) {
    }

    override fun onVideoFrame(yuv: ByteArray?, cameraType: Int) {
    }

    override fun onVideoCodecInfo(width: Int, height: Int, frameRate: Int, bitrate: Int) {
    }

    fun setPreviewDisplay(holder: SurfaceHolder) {
        videoStream.setPreviewDisplay(holder)
    }

    fun switchCamera() {
        videoStream.switchCamera()
    }
}

enum class CameraType {
    CAMERA1,
    CAMERA2
}