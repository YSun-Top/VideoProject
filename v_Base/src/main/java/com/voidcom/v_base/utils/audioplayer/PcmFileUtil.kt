package com.voidcom.v_base.utils.audioplayer

import android.os.Environment
import android.text.TextUtils
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by admin on 2019/1/9.
 * 将音频数据写入文件中
 */
class PcmFileUtil {
    private var writePcmDir = Environment.getExternalStorageDirectory().path + "/PCM/"
    private var mFos: FileOutputStream? = null
    private var mFis: FileInputStream? = null

    constructor()
    constructor(writeDir: String) {
        writePcmDir = writeDir
    }

    fun openPcmFile(filePath: String): Boolean {
        return try {
            mFis = FileInputStream(File(filePath))
            true
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            mFis = null
            false
        }
    }

    fun read(buffer: ByteArray): Int {
        return try {
            mFis?.read(buffer) ?: -1
        } catch (e: IOException) {
            e.printStackTrace()
            closeReadFile()
            0
        }
    }

    fun closeReadFile() {
        mFis?.close()
        mFis = null
    }

    /**
     * 通过文件名获取文件，如果文件不存在，返回null
     *
     * @param fileName 用于换取文件的文件名
     * @return file or null
     */
    fun getFile(fileName: String): File? {
        val file = File(writePcmDir + fileName + PCMSuffix)
        return if (file.exists()) file else null
    }

    /**
     * 创建音频文件
     *
     * @param name      文件名，如果为空将使用当前时间为文件名
     * @param isNewFile 是否保存为新的文件。true如果已经存在这个名字的文件，删除重新创建
     */
    @JvmOverloads
    fun createPcmFile(name: String? = null, isNewFile: Boolean = false) {
        if (mFos != null) return
        val dir = File(writePcmDir)
        if (!dir.exists()) dir.mkdirs()
        var filename = name
        if (TextUtils.isEmpty(name)) {
            val df = SimpleDateFormat("MM-dd-hh-mm-ss", Locale.CHINA)
            filename = df.format(Date())
        }
        val pcm = File(writePcmDir + filename + PCMSuffix)
        try {
            if (isNewFile && pcm.exists()) pcm.delete()
            if (pcm.createNewFile()) {
                mFos = FileOutputStream(pcm)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun write(data: ByteArray) {
        write(data, 0, data.size)
    }

    fun write(data: ByteArray, offset: Int, len: Int) {
        synchronized(this@PcmFileUtil) {
            mFos?.write(data, offset, len)
        }
    }

    fun closeWriteFile() {
        synchronized(this@PcmFileUtil) {
            mFos?.close()
            mFos = null
        }
    }

    companion object {
        private const val PCMSuffix = ".pcm"
    }
}