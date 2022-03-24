package com.voidcom.v_base.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.voidcom.v_base.utils.BindingReflex

abstract class BaseFrameActivity<VB : ViewBinding, VM : BaseViewModel> : AppCompatActivity() {

    protected val mBinding: VB by lazy(LazyThreadSafetyMode.NONE) {
        BindingReflex.reflexViewBinding(javaClass, layoutInflater)
    }

    protected abstract val mViewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        onInitUI()
        onInitListener()
        onInitData()
    }

    abstract fun onInitUI()

    abstract fun onInitListener()

    abstract fun onInitData()

    /**
     * 设置 ActionBar的标题和返回键
     */
    open fun setActionBar(mTitle: String, hasBackBtn: Boolean = false) {
        supportActionBar?.run {
            setHomeButtonEnabled(hasBackBtn)
            setDisplayHomeAsUpEnabled(hasBackBtn)
            title = mTitle
        }
    }
}