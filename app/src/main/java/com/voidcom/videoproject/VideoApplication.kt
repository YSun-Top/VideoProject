package com.voidcom.videoproject

import android.app.Application
import android.content.Context

/**
 * Created by voidcom on 2022/4/17 16:00
 * Description:
 */
class VideoApplication : Application() {
    companion object {
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}