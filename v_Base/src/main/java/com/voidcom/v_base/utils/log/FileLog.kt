package com.voidcom.v_base.utils.log

import android.util.Log
import com.voidcom.v_base.utils.log.FileLog
import java.io.*
import java.lang.Exception
import java.util.*

/**
 * Created by zhaokaiqiang on 15/11/18.
 */
object FileLog {
    private const val FILE_PREFIX = "KLog_"
    private const val FILE_FORMAT = ".log"
    fun printFile(
        tag: String?,
        targetDirectory: File,
        fileName: String?,
        headString: String?,
        msg: String
    ) {
        val fileNameStr = fileName ?: FileLog.fileName
        if (save(targetDirectory, fileNameStr, msg)) {
            Log.d(
                tag,
                headString + " save log success ! location is >>>" + targetDirectory.absolutePath + "/" + fileNameStr
            )
        } else {
            Log.e(tag, headString + "save log fails !")
        }
    }

    private fun save(dic: File, fileName: String, msg: String): Boolean {
        val file = File(dic, fileName)
        return try {
            val outputStream: OutputStream = FileOutputStream(file)
            val outputStreamWriter = OutputStreamWriter(outputStream, "UTF-8")
            outputStreamWriter.write(msg)
            outputStreamWriter.flush()
            outputStream.close()
            true
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            false
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            false
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private val fileName: String
        get() {
            return FILE_PREFIX + (System.currentTimeMillis() + Random().nextInt(
                10000
            )).toString().substring(4) + FILE_FORMAT
        }
}