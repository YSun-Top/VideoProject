package com.voidcom.v_base.utils.audioplayer

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresPermission
import com.voidcom.v_base.BuildConfig
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.KLog
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by Void on 2020/4/20 15:35
 * 内置音频录制器
 */
class InnerAudioRecorder {
    //缓冲区大小
    private var bufferSize = 0

    //音频录制器
    private var audioRecorder: AudioRecord? = null
    private val listeners = LinkedList<AudioRecorderListener>()
    private val pcmFileUtil: PcmFileUtil by lazy { PcmFileUtil() }
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()


    private var isMute = false
    private var isRun = true
    private var isWritePCMFile = false

    private val isDebug = BuildConfig.DEBUG
//    private val isDebug = false

    //尝试重新获取麦克风的次数，当成功获取到麦克风或停止录音时，应当对这个值归0
    private var tryReInitNum = 0

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    constructor() {
        InnerAudioRecorder(
            AudioParam(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ), false
        )
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    constructor(audioParam: AudioParam, isWritePCMFile: Boolean) {
        if (audioRecorder != null) return
        if (isWritePCMFile) {
            pcmFileUtil.createPcmFile("InnerAudioRecorder_output_pcm", true)
        }
        bufferSize = AudioRecord.getMinBufferSize(
            audioParam.sampleRate,
            audioParam.channelConfig,
            audioParam.audioFormat
        ) * 2
        try {
            audioRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioRecord.Builder()
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioParam.audioFormat)
                            .setSampleRate(audioParam.sampleRate)
                            .setChannelMask(audioParam.channelConfig)
                            .build()
                    )
                    .setAudioSource(audioParam.audioSource)
                    .setBufferSizeInBytes(bufferSize)
                    .build()
            } else {
                AudioRecord(
                    audioParam.audioSource,
                    audioParam.sampleRate,
                    audioParam.channelConfig,
                    audioParam.audioFormat,
                    bufferSize
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        isRun = true
        if (audioRecorder?.state == AudioRecord.STATE_INITIALIZED && !isRecording()) {
            audioRecorder?.startRecording()
        }
        executor.submit(recorderRunnable)
    }

    fun stopRecorder() {
        isRun = false
        if (audioRecorder?.state == AudioRecord.STATE_INITIALIZED) {
            try {
                audioRecorder?.stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    fun release() {
        stopRecorder()
        audioRecorder?.release()
        audioRecorder = null
        executor.shutdown()
    }

    /**
     * @return Boolean  当前是否正在录制
     */
    fun isRecording(): Boolean = isRun

    fun setMute(m: Boolean) {
        this.isMute = m
        if (!isMute && !isRun) {
            startRecorder()
        }
    }

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
        stopRecorder()
        outLog("Error### ReInit audioRecorder")
        Thread.sleep(500)
        startRecorder()
    }

    private fun outLog(string: String) {
        if (isDebug) KLog.d(TAG, "系统音频录制器-$string")
    }

    interface AudioRecorderListener {
        fun onAudioData(data: ByteArray, start: Int, end: Int)
        fun onInitError(message: String)
    }

    private val recorderRunnable = Runnable {
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
        while (isRun) {
            //当设置为静音时，停止捕获音频，在取消静音后再启动一个新的录音线程
            if (isMute) {
                outLog("isMute = true")
                stopRecorder()
                break
            }
            if (audioRecorder?.state != AudioRecord.STATE_INITIALIZED) {
                for (l in listeners.iterator()) {
                    l.onInitError("系统音频录制器初始化失败")
                }
                outLog("初始化失败")
                //尝试重新获取麦克风
                tryReInitRecorder()
                Thread.sleep(500)
                continue
            }
            readLength = audioRecorder?.read(buffer, 0, bufferSize) ?: AudioRecord.ERROR
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
            when (readLength) {
                AudioRecord.ERROR,
                AudioRecord.ERROR_BAD_VALUE,
                AudioRecord.ERROR_INVALID_OPERATION -> {
                    for (l in listeners.iterator()) {
                        l.onInitError("Error### System AudioRecord ERROR:$readLength")
                    }
                    outLog("Error### System AudioRecord ERROR:$readLength")
                    tryReInitRecorder()
                    continue
                }
            }
            tryReInitNum = 0
            if (readLength <= 0) continue
            listeners.forEach {
                it.onAudioData(buffer, 0, readLength)
            }
            if (isWritePCMFile) {
                pcmFileUtil.write(buffer)
            }
        }
    }

    companion object {
        val TAG: String = InnerAudioRecorder::class.java.simpleName
    }
}