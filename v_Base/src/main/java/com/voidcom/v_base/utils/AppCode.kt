package com.voidcom.v_base.utils

import android.Manifest

/**
 * Created by Void on 2020/7/10 14:22
 *
 */
object AppCode {

    //region 权限
    const val requestReadStorage = 1001
    const val requestReadPhoneState = 1002
    const val requestRecordAudio = 1003
    const val requestAudioSettings = 1004
    const val requestCamera = 1005
    const val requestLocation = 1006

    val readStoragePermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    val readPhoneStatePermissions = arrayOf(
        Manifest.permission.READ_PHONE_STATE
    )
    val recordAudioPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )
    val audioSettingsPermissions = arrayOf(
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    )
    val cameraPermissions = arrayOf(
        Manifest.permission.CAMERA
    )
    val locationPermission = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    //endregion

    //region ------------------ 一些通用的值
    //获取麦克风失败时最大的尝试次数
    const val tryGetRecorderFailMaxCount = 10

    //选择文件后返回结果时的code
    const val selectFileResultCode = 1001
    //endregion

    //region ------------------ 用于储存SharedPreferences数据所使用的key

    //endregion

    //region
    const val log_videoProcess = "Log-VideoProcess"
    //endregion

    const val requestPermissionsAction = "com.voidcom.v_base.requestPermission"
}