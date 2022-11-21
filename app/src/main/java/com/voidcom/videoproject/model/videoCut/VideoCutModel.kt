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
        val folderPath = createFolder(getVideoFrameImagePath(context))
        return FFmpegCommand.getVideoFrameImageCommand2(
            filePathStr,
            "${width}x${height}",
            "$folderPath/$time.jpg"
        )
    }

    fun getVideoFrameImageCommand(context: Context, width: Int, height: Int): String {
        val folderPath = createFolder(getVideoFrameImagePath(context))
        return FFmpegCommand.getVideoFrameImageCommand2(
            filePathStr,
            "${width}x${height}",
            "$folderPath/%5d.jpg"
        )
    }

    fun videoCutCommand(context: Context, leftTime: Long, rightTime: Long): String {
        val folderPath = createFolder(getVideoCutPath(context))
        return FFmpegCommand.videoCutCommand(
            filePathStr,
            leftTime,
            rightTime,
            "$folderPath/cutVideoFile.mp4"
        )
    }

    fun getVideoFrameImagePath(context: Context) =
        "${context.externalCacheDir?.path}/VideoFrameImage"

    fun getVideoCutPath(context: Context) = "${context.externalCacheDir?.path}/VideoCut"

    fun createFolder(path: String): String {
        val folder = File(path)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return path
    }

    fun deleteFileAndFolder(path: String) {
        val file = File(path)
        if (!file.exists()) return
        if (file.isFile) {
            file.delete()
        } else {
            file.listFiles()?.forEach {
                it.delete()
            }
            file.delete()
        }
    }
}