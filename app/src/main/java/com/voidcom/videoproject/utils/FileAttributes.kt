package com.voidcom.videoproject.utils

import java.io.File

/**
 * Created by Void on 2020/8/17 18:51
 * 文件属性
 * name 文件名
 * path 路径
 * size 文件大小 单位：bit
 * fileType 文件类型，如：.mp4  .mkv
 */
class FileAttributes {
    var name: String = "-error-"
    var path: String = ""
    var size: Long = 0L
    var fileType: String = ""
    var isValid=false
    lateinit var playFile: File

    /**
     * @param path 路径
     * @return 文件信息解析结果  true成功
     */
    fun initData(path: String) :Boolean{
        if (this.path==path&&isValid)return true
        playFile = File(path)
        if (!playFile.exists())return false
        playFile.name.run {
            val index = lastIndexOf(".")
            if (index==-1)return false
            name = substring(0, index)
            fileType = substring(index, length)
        }
        this.path = path
        size = playFile.length()
        isValid=true
        return true
    }
}