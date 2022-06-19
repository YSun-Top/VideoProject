package com.voidcom.v_base.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ArrayRes
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.voidcom.v_base.utils.BindingReflex

/**
 * Created by Void on 2020/12/4 13:42
 *
 */
abstract class BaseDefaultFragment<VB : ViewBinding, VM : BaseViewModel> : Fragment() {
    protected val mHandler = Handler(Looper.getMainLooper())
    protected lateinit var mBinding: VB

    protected abstract val mViewModel: VM

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = BindingReflex.reflexViewBinding(javaClass, layoutInflater)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onInitUI()
        onInitListener()
        onInitData()
    }

    abstract fun onInitUI()

    abstract fun onInitListener()

    abstract fun onInitData()

    open fun getArrayRes(@ArrayRes id: Int): Array<String> {
        return requireActivity().resources.getStringArray(id)
    }
}

