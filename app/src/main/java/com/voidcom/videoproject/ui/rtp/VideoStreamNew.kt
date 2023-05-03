package com.voidcom.videoproject.ui.rtp

import android.app.Activity
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import java.lang.ref.WeakReference

class VideoStreamNew(
    val callback: AudioStream.OnFrameDataCallback,
    val mTextureView: TextureView,
    val videoParam: VideoParam,
    val context: WeakReference<Activity>
) : VideoStreamBase(), SurfaceTextureListener, Camera2Listener {
    /**
     * 当前屏幕的一个旋转状态，如果屏幕没有旋转是默认的状态，值为{@link Surface#ROTATION_0}。
     * 当屏幕旋转了90°，则返回值{@link Surface#ROTATION_90}或{@link Surface#ROTATION_270}，
     * 这取决于旋转的方向
     */
    private var rotation = 0
    private lateinit var camera2Helper: Camera2Helper

    init {
        mTextureView.surfaceTextureListener = this
    }

    override fun startLive() {
    }

    override fun setPreviewDisplay(surfaceHolder: SurfaceHolder?) {

    }

    override fun switchCamera() {
        camera2Helper.switchCamera()
    }

    override fun stopLive() {

    }

    override fun release() {

    }

    override fun onPreviewDegreeChanged(degree: Int) {

    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.i(TAG, "onSurfaceTextureAvailable...")
        startPreview()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.i(TAG, "onSurfaceTextureDestroyed...")
        stopPreview()
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

    }

    override fun onCameraOpened(previewSize: Size?, displayOrientation: Int) {
        Log.i(TAG, "onCameraOpened previewSize=" + previewSize.toString())

    }

    override fun onPreviewFrame(yuvData: ByteArray?) {

    }

    override fun onCameraClosed() {

    }

    override fun onCameraError(e: Exception?) {

    }

    private fun startPreview() {
        rotation = context.get()?.windowManager?.defaultDisplay?.rotation ?: 0
        camera2Helper = Camera2Helper(
            previewDisplayView = mTextureView,
            specificCameraId = Camera2Helper.CAMERA_ID_BACK,
            camera2Listener = this,
            previewViewSize = Size(videoParam.width, videoParam.height),
            rotation = rotation,
            rotateDegree = getPreviewDegree(rotation),
            context = context
        )
        camera2Helper.start()
    }

    private fun stopPreview() {
        camera2Helper.stop()
    }

    private fun getPreviewDegree( rotation: Int): Int {
        return when (rotation) {
            Surface.ROTATION_0 -> 90
            Surface.ROTATION_90 -> 0
            Surface.ROTATION_180 -> 270
            Surface.ROTATION_270 -> 180
            else -> -1
        }
    }

    companion object{
        private const val TAG = "VideoStreamNew"
    }

}