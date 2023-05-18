package com.example.libpushvideo

interface OnFrameDataCallback {
    fun onAudioCodecInfo(sampleRate: Int, channelCount: Int)
    fun onVideoCodecInfo(wHArray:IntArray, frameRate: Int, bitrate: Int)
    fun onVideoFrame(yuv: ByteArray, cameraType: Int)
}