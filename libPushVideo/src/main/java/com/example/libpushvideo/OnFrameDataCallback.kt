package com.example.libpushvideo

interface OnFrameDataCallback {
    fun getInputSamples(): Int
    fun onAudioFrame(pcm: ByteArray?)
    fun onAudioCodecInfo(sampleRate: Int, channelCount: Int)
    fun onVideoFrame(yuv: ByteArray, cameraType: Int)
    fun onVideoCodecInfo(width: Int, height: Int, frameRate: Int, bitrate: Int)
}