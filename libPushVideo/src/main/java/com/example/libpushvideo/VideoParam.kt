package com.example.libpushvideo

/**
 * 推流视频配置信息
 */
data class VideoParam(
    var width: Int,
    var height: Int,
    var cameraId: Int,
    var bitRate: Int,
    var frameRate: Int
)