package com.voidcom.v_base.utils.audioplayer

/**
 * 音频配置
 */
data class AudioParam(
    var sampleRate: Int,
    var channelConfig: Int,
    var audioFormat: Int,
    var numChannels: Int
)
