package com.voidcom.videoproject.ui.rtp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.view.TextureView
import androidx.core.app.ActivityCompat
import java.lang.ref.WeakReference

class Camera2Helper(
    val previewDisplayView: TextureView,
    val cameraId: String,
    val camera2Listener: Camera2Listener? = null,
    val previewViewSize: Point,
    val rotation: Int = 0,
    val rotateDegree: Int = 0,
    val context: WeakReference<Activity>
) {
    private var mCameraDevice: CameraDevice? = null
    private lateinit var mBackgroundThread: HandlerThread
    private lateinit var mBackgroundHandler: Handler
    fun start() {
        if (mCameraDevice != null) return
        startBackgroundThread()
        if (previewDisplayView.isAvailable) {
            openCamera()
        }
    }

    fun stop() {

    }

    fun switchCamera() {

    }

    private fun openCamera() {
        val cameraManager = context.get()?.let {
            it.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        } ?: return

        try {
            if (ActivityCompat.checkSelfPermission(
                    context.get() ?: return,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                cameraManager.openCamera(cameraId, mDevicesStateCallback, mBackgroundHandler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread.start()
        mBackgroundHandler = Handler(mBackgroundThread.looper)
    }

    private fun setUpCameraOutput(){

    }
    private fun configureTransform(){

    }

    private val mDevicesStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {

        }

        override fun onDisconnected(camera: CameraDevice) {

        }

        override fun onError(camera: CameraDevice, error: Int) {

        }

    }

    companion object {
        const val CAMERA_ID_FRONT = "1"
        const val CAMERA_ID_BACK = "0"
    }
}