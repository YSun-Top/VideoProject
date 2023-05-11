package com.example.libpushvideo

import android.app.Activity
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import com.voidcom.v_base.utils.KLog
import java.lang.ref.WeakReference

class VideoStreamNew(
    val callback: OnFrameDataCallback,
    val mTextureView: TextureView,
    val videoParam: VideoParam,
    val context: WeakReference<Activity>
) : VideoStreamBase(), Camera2Listener {
    /**
     * 当前屏幕的一个旋转状态，如果屏幕没有旋转是默认的状态，值为{@link Surface#ROTATION_0}。
     * 当屏幕旋转了90°，则返回值{@link Surface#ROTATION_90}或{@link Surface#ROTATION_270}，
     * 这取决于旋转的方向
     */
    private var rotation = 0
    private var isLiving = false
    private var cameraHelper: CameraHelper? = null
    private var previewSize:Size?=null

    override fun startLive() {
        isLiving = true
    }

    override fun setPreviewDisplay(surfaceHolder: SurfaceHolder?) {
    }

    override fun switchCamera() {
        cameraHelper?.switchCamera()
    }

    override fun stopLive() {
        isLiving = false
    }

    override fun release() {
        cameraHelper?.stop()
        cameraHelper?.release()
        cameraHelper = null
    }

    override fun onPreviewDegreeChanged(degree: Int) {
        updateVideoCodecInfo(degree)
    }

    override fun onCameraOpened(previewSize: Size?, displayOrientation: Int) {
        Log.i(TAG, "onCameraOpened previewSize=" + previewSize.toString())
        this.previewSize=previewSize
        updateVideoCodecInfo(getPreviewDegree(rotation))
    }

    override fun onPreviewFrame(yuvData: ByteArray) {
        if (isLiving) {
            callback.onVideoFrame(yuvData, 2)
        }
    }

    override fun onCameraClosed() {

    }

    override fun onCameraError(e: Exception?) {

    }

    private fun getPreviewDegree(rotation: Int): Int {
        return when (rotation) {
            Surface.ROTATION_0 -> 90
            Surface.ROTATION_90 -> 0
            Surface.ROTATION_180 -> 270
            Surface.ROTATION_270 -> 180
            else -> -1
        }
    }

    private fun updateVideoCodecInfo(degree: Int) {
        cameraHelper?.updatePreviewDegree(degree)
        var width = previewSize!!.width
        var height = previewSize!!.height
        if (degree == 90 || degree == 270) {
            val temp = width
            width = height
            height = temp
        }
        callback.onVideoCodecInfo(
            width,
            height,
            videoParam.frameRate,
            videoParam.bitRate
        )
    }

    fun startPreview() {
        KLog.d(TAG, "开始预览")
        rotation = context.get()?.windowManager?.defaultDisplay?.rotation ?: 0
        cameraHelper = CameraHelper(
            mTextureView,
            CameraHelper.CAMERA_ID_BACK,
            this,
            Size(videoParam.width, videoParam.height),
            rotation,
            getPreviewDegree(rotation),
            context
        ).apply { start() }
    }

    fun stopPreview() {
        KLog.d(TAG, "停止预览")
        cameraHelper?.stop()
    }

    companion object {
        private const val TAG = "VideoStreamNew"
    }

}