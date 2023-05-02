package com.voidcom.v_base.utils

import android.Manifest

/**
 * Created by Void on 2020/7/10 14:22
 *
 */
object AppCode {

    //region 权限
    const val requestReadStorage = 1001
    const val requestWriteStorage = 1002
    const val requestReadPhoneState = 1003
    const val requestRecordAudio = 1004
    const val requestAudioSettings = 1005
    const val requestCamera = 1006
    const val requestLocation = 1007

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