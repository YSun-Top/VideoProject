package com.voidcom.v_base.ui

import android.os.Handler
import android.os.Looper
import androidx.viewbinding.ViewBinding
import com.voidcom.v_base.ui.BaseFrameActivity
import com.voidcom.v_base.ui.BaseViewModel

/**
 * Created by Void on 2020/8/17 10:30
 *
 */
abstract class BaseActivity<VB : ViewBinding, VM : BaseViewModel> : BaseFrameActivity<VB, VM>() {
    protected val mHandle: Handler by lazy { Handler(Looper.getMainLooper()) }
}