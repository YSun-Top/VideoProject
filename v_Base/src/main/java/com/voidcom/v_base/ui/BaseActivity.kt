package com.voidcom.v_base.ui

import android.os.Handler
import android.os.Looper
import androidx.viewbinding.ViewBinding

/**
 * Created by Void on 2020/8/17 10:30
 *
 */
abstract class BaseActivity<VB : ViewBinding, VM : BaseViewModel> : BaseFrameActivity<VB, VM>() {
    protected val mHandle: Handler by lazy { Handler(Looper.getMainLooper()) }

    override val mViewModel: VM
        get() {
            TODO()
        }

    override fun onInitUI() {
    }

    override fun onInitListener() {
    }
}