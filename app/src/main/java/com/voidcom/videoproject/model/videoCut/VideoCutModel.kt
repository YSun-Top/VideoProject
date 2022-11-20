package com.voidcom.videoproject.model.videoCut

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.voidcom.ffmpeglib.FFmpegCommand
import com.voidcom.v_base.ui.BaseModel
import com.voidcom.v_base.utils.ToastUtils
import java.io.File

class VideoCutModel : BaseModel() {
    lateinit var register: ActivityResultLauncher<Intent>
    var filePathStr = ""
    var fileNameStr = ""

    fun openSelectFileView(context: Context) {
        try {
            register.launch(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, "选择视频文件"))
        } catch (ex: ActivityNotFoundException) {
            ToastUtils.showShort(context, "没有找到文件管理器！")
        }
    }

    fun getVideoDurationCommand(): String = FFmpegCommand.getVideoInfoCommand(filePathStr)

    /**
     * @param time 获取视频帧的时间，单位s
     */
    fun getVideoFrameImageCommand(context: Context, time: Int, width: Int, height: Int): String {
        val folderPath = getVideoFrameImagePath(context)
        val folder = File(folderPath)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return FFmpegCommand.getVideoFrameImageCommand2(
            filePathStr,
            "${width}x${height}",
            "$folderPath/$time.jpg"
        )
    }

    fun getVideoFrameImageCommand(context: Context, width: Int, height: Int): String {
        val folderPath = getVideoFrameImagePath(context)
        val folder = File(folderPath)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return FFmpegCommand.getVideoFrameImageCommand2(
            filePathStr,
            "${width}x${height}",
            "$folderPath/%5d.jpg"
        )
    }

    fun getVideoFrameImagePath(context: Context) =
        "${context.externalCacheDir?.path}/VideoFrameImage"

    fun getVideoFrameOutputPath(context: Context): String =
        "${context.externalCacheDir?.path}/001.jpeg"

    fun checkOutputFile(context: Context): Boolean {
        val outFile = File(getVideoFrameOutputPath(context))
        if (outFile.exists()) {
            return outFile.delete()
        }
        return false
    }

    fun deleteVideoFrameImageCache(context: Context) {
        val folderPath = getVideoFrameImagePath(context)
        val folder = File(folderPath)
        if (!folder.exists()) return
        folder.listFiles()?.forEach {
            it.delete()
        }
        //如果文件夹为空，删除改文件
        if (folder.list()?.isEmpty() == true) {
            folder.delete()
        }
    }

}