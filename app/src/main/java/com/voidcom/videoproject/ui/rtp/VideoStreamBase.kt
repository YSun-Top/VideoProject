package com.voidcom.videoproject.ui.rtp

import android.view.SurfaceHolder

abstract class VideoStreamBase {

    abstract fun startLive()

    abstract fun setPreviewDisplay(surfaceHolder: SurfaceHolder?)

    abstract fun switchCamera()

    abstract fun stopLive()

    abstract fun release()

    abstract fun onPreviewDegreeChanged(degree: Int)
}