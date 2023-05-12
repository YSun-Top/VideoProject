package com.example.libpushvideo

interface OnFrameDataCallback {
    fun getInputSamples(): Int
    fun onAudioCodecInfo(sampleRate: Int, channelCount: Int)
    fun onAudioFrame(pcm: ByteArray?)
    fun onVideoCodecInfo(width: Int, height: Int, frameRate: Int, bitrate: Int)
    fun onVideoFrame(yuv: ByteArray, cameraType: Int)
}