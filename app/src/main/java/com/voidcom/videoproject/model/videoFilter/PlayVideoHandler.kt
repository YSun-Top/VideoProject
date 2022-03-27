package com.voidcom.videoproject.model.videoFilter

import android.view.SurfaceHolder

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 */
class PlayVideoHandler : SurfaceHolder.Callback, PlayStateCallback {
    override fun surfaceCreated(holder: SurfaceHolder) {
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    override fun onPrepared() {
    }

    override fun onCompletion() {
    }

    override fun onPlayCancel() {
    }
}