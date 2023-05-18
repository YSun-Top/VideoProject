package com.example.libpushvideo

/**
 * 推流视频配置信息
 * @param cameraId 摄像头ID，一般是前置摄像头 (1) 和后置摄像头 (0) 两种
 * @param bitRate 码率
 * @param frameRate 帧率
 */
data class VideoParam(
    var width: Int,
    var height: Int,
    var cameraId: Int,
    var bitRate: Int,
    var frameRate: Int
)