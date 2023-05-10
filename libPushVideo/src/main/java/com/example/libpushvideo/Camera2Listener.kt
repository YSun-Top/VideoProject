package com.example.libpushvideo

import android.util.Size

interface Camera2Listener {

    fun onCameraOpened(previewSize: Size?, displayOrientation: Int)

    fun onPreviewFrame(yuvData: ByteArray)

    fun onCameraClosed()

    fun onCameraError(e: Exception?)
}