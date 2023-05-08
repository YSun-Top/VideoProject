package com.voidcom.videoproject.ui.rtp

import android.app.Activity
import android.util.Log
import android.util.Size
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
    private var camera2Helper: Camera2Helper?=null

    override fun startLive() {
    }

    override fun setPreviewDisplay(surfaceHolder: SurfaceHolder?) {

    }

    override fun switchCamera() {
        camera2Helper?.switchCamera()
    }

    override fun stopLive() {

    }

    override fun release() {

    }

    override fun onPreviewDegreeChanged(degree: Int) {

    }

    override fun onCameraOpened(previewSize: Size?, displayOrientation: Int) {
        Log.i(TAG, "onCameraOpened previewSize=" + previewSize.toString())

    }

    override fun onPreviewFrame(yuvData: ByteArray) {
        if (isLiving){
            callback.onVideoFrame(yuvData,2)
        }
    }

    override fun onCameraClosed() {

    }

    override fun onCameraError(e: Exception?) {

    }

    fun startPreview() {
        KLog.d(TAG,"开始预览")
        rotation = context.get()?.windowManager?.defaultDisplay?.rotation ?: 0
        camera2Helper = Camera2Helper(
            mTextureView,
            Camera2Helper.CAMERA_ID_BACK,
            this,
            Size(videoParam.width, videoParam.height),
            rotation,
            context
        ).apply { start() }
    }

    fun stopPreview() {
        KLog.d(TAG,"停止预览")
        camera2Helper?.stop()
    }

    companion object{
        private const val TAG = "VideoStreamNew"
    }

}