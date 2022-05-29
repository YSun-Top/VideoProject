package com.voidcom.v_base.utils.audioplayer

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import com.voidcom.v_base.BuildConfig
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.KLog
import java.util.*

/**
 * Created by Void on 2020/4/20 15:35
 * 内置音频录制器
 */
class InnerAudioRecorder {
    private val TAG = InnerAudioRecorder::class.java.simpleName

    companion object {
        private var instance: InnerAudioRecorder? = null
            get() {
                if (field == null) field = InnerAudioRecorder()
                return field
            }

        fun get(): InnerAudioRecorder = instance!!
    }

    //缓存获取的采样率  32000、16000、8000
    private val bufferSampleRate = 16000

    //缓冲区大小
    private val bufferSize: Int = AudioRecord.getMinBufferSize(
        bufferSampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    //音频录制器
    private var audioRecorder: AudioRecord? = null
    private var audioThread: AudioRecorderThread? = null
    private val listeners = LinkedList<AudioRecorderListener>()
    private val pcmFileUtil = PcmFileUtil()

    //是否通过 listeners 向外输出音频
    private var outputAudioFlag = false

    private val isDebug = BuildConfig.DEBUG
//    private val isDebug = false

    //尝试重新获取麦克风的次数，当成功获取到麦克风或停止录音时，应当对这个值归0
    private var tryReInitNum = 0

    init {
        pcmFileUtil.createPcmFile("InnerAudioRecorder_output_pcm", true)
        createRecorder()
        if (audioThread == null) {
            audioThread = AudioRecorderThread()
            audioThread?.start()
        }
    }

    private fun getAudioSource(): Int = MediaRecorder.AudioSource.DEFAULT

    private fun createRecorder() {
        if (audioRecorder != null) return
        audioRecorder = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioRecord.Builder()
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(bufferSampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .build()
                    )
                    .setAudioSource(getAudioSource())
                    .setBufferSizeInBytes(bufferSize)
                    .build()
            } else {
                AudioRecord(
                    getAudioSource(),
                    bufferSampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun addListener(listener: AudioRecorderListener) {
        if (listeners.contains(listener)) return//防止重复添加
        listeners.add(listener)
    }

    fun removeListener(listener: AudioRecorderListener) {
        if (listeners.contains(listener)) listeners.remove(listener)
    }

    fun startRecorder() {
        createRecorder()
        outputAudioFlag = true
        if (audioRecorder?.state == AudioRecord.STATE_INITIALIZED && !isRecording()) {
            audioRecorder?.startRecording()
        }
        if (audioThread == null) {
            audioThread = AudioRecorderThread()
            audioThread?.start()
        }
    }

    fun stopRecorder() {
        outputAudioFlag = false
        if (audioRecorder?.state == AudioRecord.STATE_INITIALIZED) {
            audioRecorder?.stop()
        }
        audioRecorder = null
    }

    /**
     * 结束并释放录音
     */
    fun kill() {
        outputAudioFlag = false
        if (audioRecorder?.state == AudioRecord.STATE_INITIALIZED) {
            audioRecorder?.stop()
        }
        audioRecorder?.release()
        audioThread?.stopNow()
        audioRecorder = null
        audioThread = null
        instance = null
    }

    /**
     * @return Boolean  当前是否正在录制
     */
    fun isRecording(): Boolean = audioRecorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    /**
     * 尝试重新获取麦克风
     */
    @Synchronized
    private fun tryReInitRecorder() {
        if (tryReInitNum > AppCode.tryGetRecorderFailMaxCount) {
            tryReInitNum = 0
            return
        }
        tryReInitNum++
        try {
            stopRecorder()
            outLog("Error### ReInit audioRecorder")
        } catch (e: Exception) {
        }
        Thread.sleep(500)
        startRecorder()
    }

    private fun outLog(string: String) {
        if (isDebug) KLog.d(TAG, "系统音频录制器-$string")
    }

    interface AudioRecorderListener {
        fun onAudioData(data: ByteArray, start: Int, length: Int)
        fun onInitError(message: String)
    }

    inner class AudioRecorderThread : Thread() {
        private var isKeepRunning: Boolean = true

        fun stopNow() {
            isKeepRunning = false
            tryReInitNum = 0
            try {
                interrupt()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun run() {
            super.run()
            //设置线程运行的优先级
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            //缓冲长度声明
            var readLength: Int
            //用于显示是否正在运行Log的时间标记
            var logTimeMark = 0L
            //线程标记
            val threadTag: Long = Random(System.currentTimeMillis()).nextLong()
            //缓冲区创建
            val buffer = ByteArray(bufferSize)
            try {
                while (isKeepRunning) {
                    if (audioRecorder?.state != AudioRecord.STATE_INITIALIZED) {
//                        for (l in listeners.iterator()) {
//                            l.onInitError("系统音频录制器初始化失败")
//                        }
                        outLog("初始化失败")
                        //尝试重新获取麦克风
                        tryReInitRecorder()
                        sleep(500)
                        continue
                    }
                    readLength = audioRecorder?.read(buffer, 0, bufferSize) ?: -1
                    //打印运行日志
                    if (System.currentTimeMillis() - logTimeMark > 5000L) {
                        outLog(
                            "Recorder thread is running\n" +
                                    "Pid:${Process.myPid()};\n" +
                                    "Tid:${Process.myTid()};\n" +
                                    "Tag:$threadTag"
                        )
                        logTimeMark = System.currentTimeMillis()
                    }
                    if (readLength == AudioRecord.ERROR_INVALID_OPERATION ||
                        readLength == AudioRecord.ERROR_BAD_VALUE ||
                        readLength == AudioRecord.ERROR
                    ) {
                        outLog("Error### System AudioRecord ERROR:$readLength")
                        tryReInitRecorder()
                        continue
                    }
                    tryReInitNum = 0
                    if (readLength > 0 && outputAudioFlag) {
                        for (l in listeners.iterator()) {
                            l.onAudioData(buffer, 0, readLength)
                        }
                        pcmFileUtil.write(buffer)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            //由于异常情况才会走到这个方法
            if (isKeepRunning) kill()
        }
    }
}