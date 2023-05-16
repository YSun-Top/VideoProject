package com.voidcom.v_base.utils.audioplayer

/**
 * 音频配置
 */
data class AudioParam(
    val audioSource:Int,
    var sampleRate: Int,
    var channelConfig: Int,
    var audioFormat: Int
)
