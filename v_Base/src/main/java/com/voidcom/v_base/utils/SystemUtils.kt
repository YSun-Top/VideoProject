package com.voidcom.v_base.utils

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

/**
 * Created by Void on 2020/8/17 15:10
 * 系统功能入口
 */
object SystemUtils {
    /**
     * 前往当前app的设置界面
     */
    fun goToCurrentAppSetting(
        appCompatActivity: AppCompatActivity,
        requestCode: Int,
        packageName: String
    ) {
        ActivityCompat.startActivityForResult(
            appCompatActivity, Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            ), requestCode, null
        )
    }

}