package com.voidcom.videoproject.model.videoCrop

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.huantansheng.easyphotos.callback.SelectCallback
import com.huantansheng.easyphotos.models.album.entity.Photo
import com.voidcom.v_base.ui.BaseModel
import com.voidcom.v_base.utils.KLog
import com.voidcom.v_base.utils.ToastUtils
import java.io.File
import java.util.ArrayList

class VideoCropModel : BaseModel() {
    lateinit var register: ActivityResultLauncher<Intent>
    var pathStr: String = ""

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

    fun getVideoDuration(): String = "ffprobe -i $pathStr -show_format -print_format json"

    /**
     * @param time 获取视频帧的时间，单位s
     * @param outputPath 输出路径(路径应包含文件名)
     */
    fun getVideoFrameImage(time: Int, outputPath: String): String {
        var flag = false
        arrayOf(".jpeg", ".jpg", ".img", ".png").forEach {
            if (outputPath.contains(it)) {
                flag = true
                return@forEach
            }
        }
        if (!flag) throw RuntimeException("outputPath format error")
        return "ffmpeg -i $pathStr -threads 1 -ss $time -f image2 -r 1 -t 1 $outputPath"
    }

    fun getVideoFrameOutputPath(context: Context): String = "${context.cacheDir.path}/001.jpeg"

    fun getVideoFrameImage(time: Int, context: Context): String {
        return getVideoFrameImage(time, getVideoFrameOutputPath(context))
    }

    fun checkOutputFile(context: Context): Boolean {
        val outFile = File(getVideoFrameOutputPath(context))
        if (outFile.exists()) {
            return outFile.delete()
        }
        return false
    }

}