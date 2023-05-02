package com.voidcom.videoproject.ui.rtp

data class AudioParam(
    var sampleRate: Int,
    var channelConfig: Int,
    var audioFormat: Int,
    var numChannels: Int
)
