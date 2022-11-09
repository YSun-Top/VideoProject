package com.voidcom.v_base.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import java.lang.ref.WeakReference

abstract class BaseViewModel() : ViewModel() {
    private lateinit var mActivity: WeakReference<AppCompatActivity>

    abstract fun getModel(): BaseModel?

    fun setActivity(activity: AppCompatActivity) {
        mActivity = WeakReference(activity)
    }

    abstract fun onInit(context: Context)

    abstract fun onInitData()

    fun getActivity(): AppCompatActivity? = mActivity.get()
}