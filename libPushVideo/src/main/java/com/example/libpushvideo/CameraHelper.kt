package com.example.libpushvideo

import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.PermissionsUtils
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs

/**
 * 摄像头助手，用于管理摄像头的开关、切换等操作
 * @param previewDisplayView 摄像头预览的view
 * @param specificCameraId 希望开启的摄像头
 * @param cameraListener 摄像头回调，如开启、关闭、错误等
 * @param previewViewSize
 * @param rotation 角度信息，默认为0，当设备旋转了值不为零
 */
class CameraHelper(
    private val previewDisplayView: TextureView,
    private var specificCameraId: String,
    private var cameraListener: Camera2Listener? = null,
    private val previewViewSize: Size,
    private val rotation: Int,
    private var rotateDegree: Int = 0,
    private val context: WeakReference<Activity>
) {
    private var mCameraDevice: CameraDevice? = null
    private var mCameraId: String = ""

    //region ---摄像头控制---
    @Synchronized
    fun start() {
        if (mCameraDevice != null) return
        startBackgroundThread()
        if (previewDisplayView.isAvailable) {
            openCamera()
        } else {
            // 当TextureView内部的SurfaceTexture为空时，
            // 证明其不是通过surfaceTextureListener回调调用的start()方法
            previewDisplayView.surfaceTextureListener = mSurfaceTextureListener
        }
    }

    @Synchronized
    fun stop() {
        if (mCameraDevice == null) return
        closeCamera()
        stopBackgroundThread()
    }

    fun release() {
        stop()
        cameraListener = null
        context.clear()
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
            cameraListener?.onCameraClosed()
        } catch (e: InterruptedException) {
            cameraListener?.onCameraError(e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }
    //endregion

    //创建一个线程，用于在该线程执行摄像头相关任务
    //如：openCamera();setOnImageAvailableListener()等方法，如果传入的handler为空，会在当前线程执行循环
    private lateinit var mBackgroundThread: HandlerThread
    private lateinit var mBackgroundHandler: Handler
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread.start()
        mBackgroundHandler = Handler(mBackgroundThread.looper)
    }

    /**
     * 安全退出线程，并使用join方法插队等线程退出完成当前现场再继续
     */
    private fun stopBackgroundThread() {
        mBackgroundThread.quitSafely()
        try {
            mBackgroundThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * 设置摄像头输出
     * 默认配置构造方法传过来的摄像头，如果设置失败，将通过CameraManager获取摄像头列表，然后遍历摄像头直到成功
     */
    private fun setUpCameraOutput(cm: CameraManager) {
        try {
            if (configCameraParams(cm, specificCameraId)) return
            for (i in cm.cameraIdList) {
                if (configCameraParams(cm, i)) return
            }
        } catch (e: NullPointerException) {
            cameraListener?.onCameraError(e)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private lateinit var mPreviewSize: Size
    private lateinit var mImageReader: ImageReader
    private var mSensorOrientation = 0

    /**
     * 配置摄像头参数
     * @return false,获取相机输出流配置信息失败
     */
    private fun configCameraParams(cm: CameraManager, cameraID: String): Boolean {
        //获取相机的属性集
        val characteristics = cm.getCameraCharacteristics(cameraID)
        //获取相机输出流配置信息
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return false

        mPreviewSize = getBestSupportedSize(map.getOutputSizes(SurfaceTexture::class.java))

        // ImageReader可以访问Surface中的数据，用于获取摄像头数据并将其放入推流
        mImageReader = ImageReader.newInstance(
            mPreviewSize.width,
            mPreviewSize.height,
            ImageFormat.YUV_420_888,
            2
        )
        //设置一个监听，当新的图像可用时会调用
        mImageReader.setOnImageAvailableListener(imageAvailableListenerImpl, mBackgroundHandler)
        //获取图像需要顺时针旋转多少角度，才能以图像原始的方向显示
        mSensorOrientation = characteristics[CameraCharacteristics.SENSOR_ORIENTATION] ?: 0
        mCameraId = cameraID
        return true
    }

    /**
     * 设置Matrix，当设备旋转时，使画面保持一致
     */
    private fun configureTransform(viewW: Float, viewH: Float) {
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewW, viewH)
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        when (rotation) {
            Surface.ROTATION_90,
            Surface.ROTATION_270 -> {
                val bufferRect =
                    RectF(0f, 0f, mPreviewSize.height.toFloat(), mPreviewSize.width.toFloat())
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                val scale = (viewH / mPreviewSize.height).coerceAtLeast(viewW / mPreviewSize.width)
                matrix.postScale(scale, scale, centerX, centerY)
                matrix.postRotate(90f * (rotation - 2) % 360, centerX, centerY)
            }

            //使画面旋转180度
            Surface.ROTATION_180 -> matrix.postRotate(180f, centerX, centerY)
        }
        previewDisplayView.setTransform(matrix)
    }

    /**
     * 获取最佳的支持尺寸
     * 计算逻辑是：
     * 计算Size宽高的积，并和预览view宽高的积对比，值越接近越合适。
     */
    private fun getBestSupportedSize(sizes: Array<Size>): Size {
        var defaultSize = sizes[0]
        var defaultDelta =
            abs(defaultSize.width * defaultSize.height - previewViewSize.width * previewViewSize.height)
        for (size in sizes) {
            val currentDelta =
                abs(size.width * size.height - previewViewSize.width * previewViewSize.height)
            if (currentDelta < defaultDelta) {
                defaultDelta = currentDelta
                defaultSize = size
            }
        }
        return defaultSize
    }

    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null

    /**
     * 创建摄像头预览
     * 设置两个输出目标，一个是ImageReader用于推流,一个是TextureView用于预览
     */
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

            //设置自动对焦
            mPreviewRequestBuilder?.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            //给捕获对象设置输出目标
            //这里有两个，一个是ImageReader用于推流，一个是TextureView用于预览
            mPreviewRequestBuilder?.addTarget(surface)
            mPreviewRequestBuilder?.addTarget(mImageReader.surface)

            // Here, we create a CameraCaptureSession for camera preview.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mCameraDevice?.createCaptureSession(
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        listOf(
                            OutputConfiguration(surface),
                            OutputConfiguration(mImageReader.surface)
                        ),
                        Executors.newSingleThreadExecutor(),
                        cameraStateCallback
                    )
                )
            } else {
                mCameraDevice?.createCaptureSession(
                    listOf(surface, mImageReader.surface),
                    cameraStateCallback,
                    mBackgroundHandler
                )
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getCameraOrientation(rotation: Int, cameraId: String): Int {
        val degree = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> rotation * 90
        }
        val result = if (CAMERA_ID_FRONT == cameraId) {
            (360 - (mSensorOrientation + degree) % 360) % 360
        } else {
            (mSensorOrientation - degree + 360) % 360
        }
        Log.i(TAG, "getCameraOrientation, result=$result")
        return result
    }

    fun updatePreviewDegree(degree: Int) {
        this.rotateDegree = degree
    }

    private val mCameraOpenCloseLock = Semaphore(1)

    private val mDevicesStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.i(TAG, "onOpened: ")
            mCameraOpenCloseLock.release()
            mCameraDevice = camera
            createCameraPreviewSession()
            cameraListener?.onCameraOpened(mPreviewSize, getCameraOrientation(rotation, mCameraId))
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.i(TAG, "onDisconnected: ")
            mCameraOpenCloseLock.release()
            camera.close()
            mCameraDevice = null
            cameraListener?.onCameraClosed()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.i(TAG, "onError: ")
            mCameraOpenCloseLock.release()
            camera.close()
            mCameraDevice = null
            cameraListener?.onCameraError(Exception("error occurred, code is $error"))

        }
    }

    /**
     * 通过ImageReader获得摄像头数据，然后将得到的帧数据通过Camera2Listener接口传递给推流
     */
    private val imageAvailableListenerImpl = object : OnImageAvailableListener {
        private var temp = ByteArray(0)
        private lateinit var yuvData: ByteArray
        private var dstData: ByteArray? = null
        private val lock = ReentrantLock()

        override fun onImageAvailable(reader: ImageReader) {
            //获取一个完整图形缓冲数据，可以理解为一帧图像数据
            val image = reader.acquireNextImage()
            if (image.format == ImageFormat.YUV_420_888) {
                //获取图像的像素平面数组，平面数量由图像格式决定。使用错误的格式有可能返回空数组
                val planes = image.planes
                lock.lock()
                val len = image.width * image.height
                yuvData = ByteArray(len * 3 / 2)
                planes[0].buffer[yuvData, 0, len]
                var offset = len
                for (i in 1 until planes.size) {
                    var srcIndex = 0
                    var dstIndex = 0
                    val rowStride = planes[i].rowStride
                    val pixelsStride = planes[i].pixelStride
                    val buffer = planes[i].buffer
                    if (temp.size != buffer.capacity()) {
                        temp = ByteArray(buffer.capacity())
                    }
                    buffer[temp]
                    //逐个复制像素
                    for (j in 0 until image.height / 2) {
                        for (k in 0 until image.width / 2) {
                            yuvData[offset + dstIndex++] = temp[srcIndex]
                            srcIndex += pixelsStride
                        }
                        when (pixelsStride) {
                            1 -> srcIndex += rowStride - image.width / 2
                            2 -> srcIndex += rowStride - image.width
                        }
                    }
                    offset += len / 4
                }
                //如果设备旋转了，还需要对数据处理，使之显示正常
                if (rotateDegree == 90 || rotateDegree == 180) {
                    if (dstData == null) {
                        dstData = ByteArray(len * 3 / 2)
                    }
                    dstData?.let {
                        if (rotateDegree == 90) {
                            YUVUtil.YUV420pRotate90(it, yuvData, image.width, image.height)
                        } else {
                            YUVUtil.YUV420pRotate180(it, yuvData, image.width, image.height)
                        }
                        cameraListener?.onPreviewFrame(it)
                    }
                } else {
                    cameraListener?.onPreviewFrame(yuvData)
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
            cameraListener?.onCameraError(Exception("configureFailed"))
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
        private val TAG = CameraHelper::class.java.simpleName
    }
}