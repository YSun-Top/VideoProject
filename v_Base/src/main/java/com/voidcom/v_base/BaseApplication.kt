package com.voidcom.v_base

import android.app.Application
import android.util.DisplayMetrics

/**
 *
 * @Description: java类作用描述
 * @Author: Void
 * @CreateDate: 2022/11/17 14:45
 * @UpdateDate: 2022/11/17 14:45
 */
open class BaseApplication: Application() {
    companion object {
        lateinit var displayMetrics: DisplayMetrics
    }

    override fun onCreate() {
        super.onCreate()
        displayMetrics=resources.displayMetrics
    }
}