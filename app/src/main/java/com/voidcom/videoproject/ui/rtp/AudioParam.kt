package com.voidcom.videoproject.ui.rtp

/**
 * 推流音频配置
 */
data class AudioParam(
    var sampleRate: Int,
    var channelConfig: Int,
    var audioFormat: Int,
    var numChannels: Int
)
