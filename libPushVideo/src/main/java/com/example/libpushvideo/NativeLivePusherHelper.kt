package com.example.libpushvideo

class NativeLivePusherHelper {
    private var callback: LiveErrorCallback? = null

    external fun nativeInit()

    external fun nativePushVideo(yuv: ByteArray, cameraType: Int)

    external fun nativeStart(path: String)

    external fun nativeStop()

    external fun nativeRelease()

    external fun nativeSetVideoCodecInfo(width: Int, height: Int, fps: Int, bitrate: Int)

    external fun nativeSetAudioCodecInfo(sampleRateInHz: Int, channels: Int)

    fun setCallback(callback: LiveErrorCallback) {
        this.callback = callback
    }

    fun errorFromNative(errCode: Int) {
        callback?.onError(
            when (errCode) {
                ERROR_VIDEO_ENCODER_OPEN -> "打开视频编码器失败"
                ERROR_VIDEO_ENCODER_ENCODE -> "视频编码失败"
                ERROR_AUDIO_ENCODER_OPEN -> "打开音频编码器失败"
                ERROR_AUDIO_ENCODER_ENCODE -> "音频编码失败"
                ERROR_RTMP_CONNECT_SERVER -> "RTMP连接服务器失败"
                ERROR_RTMP_CONNECT_STREAM -> "RTMP连接流失败"
                ERROR_RTMP_SEND_PACKET -> "RTMP发送数据包失败"
                else -> ""
            }
        )
    }

    interface LiveErrorCallback {
        fun onError(msg: String)
    }

    companion object {
        const val ERROR_VIDEO_ENCODER_OPEN = 0x01
        const val ERROR_VIDEO_ENCODER_ENCODE = 0x02
        const val ERROR_AUDIO_ENCODER_OPEN = 0x03
        const val ERROR_AUDIO_ENCODER_ENCODE = 0x04
        const val ERROR_RTMP_CONNECT_SERVER = 0x05
        const val ERROR_RTMP_CONNECT_STREAM = 0x06
        const val ERROR_RTMP_SEND_PACKET = 0x07

        init {
            System.loadLibrary("libPushVideo")
        }

        private class InstanceHolder {
            companion object {
                val helper = NativeLivePusherHelper()
            }
        }

        fun getInstant(): NativeLivePusherHelper = InstanceHolder.helper
    }
}