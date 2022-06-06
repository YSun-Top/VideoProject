package com.voidcom.libsdkbase

/**
 * Created by voidcom on 2022/4/17 15:39
 * Description:
 */
interface JniCallback {

    /**
     * c层播放状态回调
     * 注: 由c调用改方法
     * 详细请查看状态定义文件：src/main/cpp/define/default_code.h
     * @param status 0=Prepared
     */
    fun onPlayStatusCallback(status: Int)

    /**
     * c层错误回调
     * 注: 由c调用改方法
     * 详细请查看错误定义文件：src/main/cpp/ErrorCodeDefine.h
     * @param errorCode
     */
    fun onErrorCallback(errorCode: Int, msg: String)
}