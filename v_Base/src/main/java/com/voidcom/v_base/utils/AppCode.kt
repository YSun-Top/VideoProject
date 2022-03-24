package com.voidcom.v_base.utils

import android.Manifest

/**
 * Created by Void on 2020/7/10 14:22
 *
 */
object AppCode {

    /*
    * 运行app所需的基本权限 todo
    * 目前的逻辑是没有这些权限将无法使用2020-06-04 10:47
    * 将权限声明在这里是因为不仅仅是在权限请求布局需要权限判断，在其他的地方，
    * 如:WakeupService 也会有需要判断权限的情况。这是历史代码遗留的问题，不方便且没时间修改。
    * */
    val basePermissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,//读写储存
            Manifest.permission.RECORD_AUDIO,//录音
            Manifest.permission.READ_PHONE_STATE,//读取手机状态
//            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.MODIFY_AUDIO_SETTINGS//修改音频设置
    )

    //region ------------------ 一些通用的值
    //获取麦克风失败时最大的尝试次数
    const val tryGetRecorderFailMaxCount = 10
    //选择文件后返回结果时的code
    const val selectFileResultCode = 1001
    //endregion

    //region ------------------ 用于储存SharedPreferences数据所使用的key

    //endregion
}