package com.voidcom.videoproject.ui.rtp

import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.PermissionsUtils
import java.lang.ref.WeakReference
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs

/**
 * 摄像头助手，用于管理摄像头的开关、切换等操作
 */
class Camera2Helper(
    val previewDisplayView: TextureView,
    var specificCameraId: String,
    val camera2Listener: Camera2Listener? = null,
    val previewViewSize: Point,
    val rotation: Int = 0,
    val rotateDegree: Int = 0,
    val context: WeakReference<Activity>
) {
    private var mCameraDevice: CameraDevice? = null
    private lateinit var mBackgroundThread: HandlerThread
    private lateinit var mBackgroundHandler: Handler
    private var mCameraId: String = ""

    //region ---摄像头控制---
    fun start() {
        if (mCameraDevice != null) return
        startBackgroundThread()
        if (previewDisplayView.isAvailable) {
            openCamera()
        } else {
            previewDisplayView.surfaceTextureListener = mSurfaceTextureListener
        }
    }

    fun stop() {
        if (mCameraDevice == null) return
        closeCamera()
        stopBackgroundThread()
    }

    fun switchCamera() {
        if (CAMERA_ID_BACK == mCameraId) {
            specificCameraId = CAMERA_ID_FRONT
        } else if (CAMERA_ID_FRONT == mCameraId) {
            specificCameraId = CAMERA_ID_BACK
        }
        stop()
        start()
    }

    private fun openCamera() {
        val cameraManager = context.get()?.let {
            it.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        } ?: return
        setUpCameraOutput(cameraManager)
        configureTransform(previewDisplayView.width.toFloat(), previewDisplayView.height.toFloat())
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MICROSECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            if (PermissionsUtils.checkPermission(context.get() ?: return, AppCode.requestCamera)
                    .isEmpty()
            ) {
                cameraManager.openCamera(mCameraId, mDevicesStateCallback, mBackgroundHandler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            mCaptureSession.close()
            mCameraDevice?.close()
            mCameraDevice = null
            mImageReader.close()
            camera2Listener?.onCameraClosed()
        } catch (e: InterruptedException) {
            camera2Listener?.onCameraError(e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }
    //endregion

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread.start()
        mBackgroundHandler = Handler(mBackgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        mBackgroundThread.quitSafely()
        try {
            mBackgroundThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun setUpCameraOutput(cm: CameraManager) {
        try {
            if (configCameraParams(cm, specificCameraId)) return
            for (i in cm.cameraIdList) {
                if (configCameraParams(cm, i)) return
            }
        } catch (e: NullPointerException) {
            camera2Listener?.onCameraError(e)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private lateinit var mPreviewSize: Size
    private lateinit var mImageReader: ImageReader
    private var mSensorOrientation = 0
    private fun configCameraParams(cm: CameraManager, cameraID: String): Boolean {
        val characteristics = cm.getCameraCharacteristics(cameraID)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return false

        mPreviewSize =
            getBestSupportedSize(ArrayList(listOf(*map.getOutputSizes(SurfaceTexture::class.java))))
        mImageReader = ImageReader.newInstance(
            mPreviewSize.width,
            mPreviewSize.height,
            ImageFormat.YUV_420_888,
            2
        )
        mImageReader.setOnImageAvailableListener(imageAvailableListenerImpl, mBackgroundHandler)
        mSensorOrientation = characteristics[CameraCharacteristics.SENSOR_ORIENTATION] ?: 0
        mCameraId = cameraID
        return true
    }

    private fun configureTransform(w: Float, h: Float) {
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, w, h)
        val bufferRect = RectF(0f, 0f, mPreviewSize.height.toFloat(), mPreviewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale: Float = (h / mPreviewSize.height).coerceAtLeast(w / mPreviewSize.width)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2) % 360).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        previewDisplayView.setTransform(matrix)
    }

    private fun getBestSupportedSize(sizes: List<Size>): Size {
        var defaultSize = sizes[0]
        var defaultDelta =
            abs(defaultSize.width * defaultSize.height - previewViewSize.x * previewViewSize.y)
        for (size in sizes) {
            val currentDelta = abs(size.width * size.height - previewViewSize.x * previewViewSize.y)
            if (currentDelta < defaultDelta) {
                defaultDelta = currentDelta
                defaultSize = size
            }
        }
        return defaultSize
    }

    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null
    private fun createCameraPreviewSession() {
        try {
            val texture = previewDisplayView.surfaceTexture ?: return

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.width, mPreviewSize.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder =
                mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder?.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            mPreviewRequestBuilder?.addTarget(surface)
            mPreviewRequestBuilder?.addTarget(mImageReader.surface)

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice?.createCaptureSession(
                listOf(surface, mImageReader.surface),
                cameraStateCallback,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getCameraOrientation(rotation: Int, cameraId: String): Int {
        val degree = when (rotation * 90) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> rotation * 90
        }
        var result: Int
        if (CAMERA_ID_FRONT == cameraId) {
            result = (mSensorOrientation + degree) % 360
            result = (360 - result) % 360
        } else {
            result = (mSensorOrientation - degree + 360) % 360
        }
        Log.i(TAG, "getCameraOrientation, result=$result")
        return result
    }

    private val mCameraOpenCloseLock = Semaphore(1)

    private val mDevicesStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.i(TAG, "onOpened: ")
            mCameraOpenCloseLock.release()
            mCameraDevice = camera
            createCameraPreviewSession()
            camera2Listener?.onCameraOpened(mPreviewSize, getCameraOrientation(rotation, mCameraId))
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.i(TAG, "onDisconnected: ")
            mCameraOpenCloseLock.release()
            camera.close()
            mCameraDevice = null
            camera2Listener?.onCameraClosed()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.i(TAG, "onError: ")
            mCameraOpenCloseLock.release()
            camera.close()
            mCameraDevice = null
            camera2Listener?.onCameraError(Exception("error occurred, code is $error"))

        }
    }

    private val imageAvailableListenerImpl = object : OnImageAvailableListener {
        private var temp: ByteArray? = null
        private lateinit var yuvData: ByteArray
        private var dstData: ByteArray? = null
        private val lock = ReentrantLock()

        override fun onImageAvailable(reader: ImageReader) {
            val image = reader.acquireNextImage()
            if (image.format == ImageFormat.YUV_420_888) {
                val planes = image.planes
                lock.lock()
                var offset = 0
                val width = image.width
                val height = image.height
                val len = width * height
                yuvData = ByteArray(len * 3 / 2)
                planes[0].buffer[yuvData, offset, len]
                offset += len
                for (i in 1 until planes.size) {
                    var srcIndex = 0
                    var dstIndex = 0
                    val rowStride = planes[i].rowStride
                    val pixelsStride = planes[i].pixelStride
                    val buffer = planes[i].buffer
                    if (temp?.size != buffer.capacity()) {
                        temp = ByteArray(buffer.capacity())
                    }
                    temp?.let {
                        buffer[it]
                        for (j in 0 until height / 2) {
                            for (k in 0 until width / 2) {
                                yuvData[offset + dstIndex++] = it[srcIndex]
                                srcIndex += pixelsStride
                            }
                            if (pixelsStride == 2) {
                                srcIndex += rowStride - width
                            } else if (pixelsStride == 1) {
                                srcIndex += rowStride - width / 2
                            }
                        }
                    }
                    offset += len / 4
                }
                if (rotateDegree == 90 || rotateDegree == 180) {
                    if (dstData == null) {
                        dstData = ByteArray(len * 3 / 2)
                    }
                    dstData?.let {
                        if (rotateDegree == 90) {
                            YUVUtil.YUV420pRotate90(it, yuvData, width, height)
                        } else {
                            YUVUtil.YUV420pRotate180(it, yuvData, width, height)
                        }
                    }
                    camera2Listener?.onPreviewFrame(dstData)
                } else {
                    camera2Listener?.onPreviewFrame(yuvData)
                }
                lock.unlock()
            }
            image.close()
        }

    }

    private lateinit var mCaptureSession: CameraCaptureSession

    private val cameraStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            Log.i(TAG, "onConfigured: ")
            if (mCameraDevice == null) return
            mCaptureSession = session
            try {
                mPreviewRequestBuilder?.build()?.let {
                    mCaptureSession.setRepeatingRequest(
                        it,
                        null,
                        mBackgroundHandler
                    )
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.i(TAG, "onConfigureFailed: ")
            camera2Listener?.onCameraError(Exception("configureFailed"))
        }
    }

    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            TODO("Not yet implemented")
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            TODO("Not yet implemented")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            TODO("Not yet implemented")
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            TODO("Not yet implemented")
        }

    }

    companion object {
        const val CAMERA_ID_FRONT = "1"
        const val CAMERA_ID_BACK = "0"
        private val TAG = Camera2Helper::class.java.simpleName
    }
}