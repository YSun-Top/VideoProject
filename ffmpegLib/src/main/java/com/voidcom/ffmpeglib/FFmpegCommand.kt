package com.voidcom.ffmpeglib

/**
 * @Description:ffmpeg 命令集合
 */
object FFmpegCommand {
    //region ----- -----

    /**
     * 将视频指定时间的图像转为图片
     * 注意：指定的时间不能大于视频时长
     */
    fun getVideoFrameImageCommand(
        pathStr: String,
        time: Int,
        frameSize: String,
        outputPath: String
    ): String {
        if (!imageFormatCheck(outputPath)) throw IllegalArgumentException("outputPath 路径不完整!")
        return "ffmpeg -i $pathStr -ss $time -s $frameSize -f image2 -preset ultrafast -vframes 1 -y $outputPath"
    }

    /**
     * 视频每隔一秒拿取一张图片
     * @param frameSize 分辨率(WxH)
     */
    fun getVideoFrameImageCommand2(pathStr: String, frameSize: String, outputPath: String): String {
        if (!imageFormatCheck(outputPath)) throw IllegalArgumentException("outputPath 路径不完整!")
        return "ffmpeg -i $pathStr -threads 2 -s $frameSize -f image2 -r 1 -preset ultrafast -y $outputPath"
    }

    /**
     * 视频每隔n秒拿去一张图片
     * @param fps 当fps=1时每秒获取一帧。如果每隔5秒获取一帧,即fps=1/5
     */
    fun getVideoFrameImageCommand3(
        pathStr: String,
        frameSize: String,
        outputPath: String,
        fps: Int
    ): String {
        if (!imageFormatCheck(outputPath)) throw IllegalArgumentException("outputPath 路径不完整!")
        return "ffmpeg -i $pathStr -threads 2 -s $frameSize -f image2 -preset ultrafast -vf fps=(1/$fps) -y $outputPath"
    }
    //ffmpeg -i input.mp4 -ss 10 -t 80 -vcodec copy -acodec copy output.mp4
    /**
     * 在指定时间段剪辑视频一份新的视频
     */
    fun videoCutCommand(
        pathStr: String,
        leftTime: Long,
        rightTime: Long,
        outputPath: String
    ): String {
        if (!videoFormatCheck(outputPath)) throw IllegalArgumentException("outputPath 路径不完整!")
        return "ffmpeg -i $pathStr -ss $leftTime -t $rightTime -vcodec copy -acodec copy $outputPath"
    }
    //endregion

    //region ----- ffprobe -----

    /**
     * 打印视频信息
     */
    fun getVideoInfoCommand(pathStr: String): String {
        if (pathStr.isEmpty()) throw IllegalArgumentException("pathStr 不能为空!")
        return "ffprobe -i $pathStr -show_format -print_format json"
    }
    //endregion

    /**
     * 图片格式检查
     */
    private fun imageFormatCheck(outputPath: String): Boolean {
        arrayOf(".jpeg", ".jpg", ".img", ".png").forEach {
            if (outputPath.endsWith(it)) return true
        }
        return false
    }

    private fun videoFormatCheck(outputPath: String): Boolean {
        arrayOf(".avi", ".mp4").forEach {
            if (outputPath.endsWith(it)) return true
        }
        return false
    }
}