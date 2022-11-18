package com.voidcom.videoproject.ui

import java.lang.ref.WeakReference
import java.util.*

/**
 *
 * @Description: java类作用描述
 * @Author: Void
 * @CreateDate: 2022/11/13 21:20
 * @UpdateDate: 2022/11/13 21:20
 */
class TimerTaskImp(activity: VideoCutActivity?) : TimerTask() {
    private val weakReference: WeakReference<VideoCutActivity>?

    init {
        weakReference = WeakReference(activity)
    }

    override fun run() {
        weakReference?.get()?.getVideoProgress()
    }
}