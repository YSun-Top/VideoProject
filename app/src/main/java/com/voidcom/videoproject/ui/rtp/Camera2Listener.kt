package com.voidcom.videoproject.ui.rtp

import android.util.Size

interface Camera2Listener {

    fun onCameraOpened(previewSize: Size?, displayOrientation: Int)

    fun onPreviewFrame(yuvData: ByteArray)

    fun onCameraClosed()

    fun onCameraError(e: Exception?)
}