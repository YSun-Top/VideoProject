# VideoProject
> 视频学习项目

## 视频信息输出
<img src="https://github.com/ExistNotSee/VideoProject/blob/main/img/video_info_output.png" width="200" height="400">
使用ffmpeg输出视频信息

命令：
```
ffprobe -i $filePath -show_streams -show_format -print_format json
```
> 具体逻辑请看：OutputVideoInfoActivity和ffprobe_cmd.cpp

## 滤镜
<img src="https://github.com/ExistNotSee/VideoProject/blob/main/img/video_filters.png" width="200" height="400">
使用ffmpeg解码并播放视频，通过滤镜参数改变滤镜效果。
如：
```
素描：lutyuv='u=128:v=128'
旋转：transpose=2
```
> UI 代码在com.voidcom.videoproject.ui.videoFilter目录下
> 视频代码在 NativePlayer.cpp 和 ffmpeg_decoder_jni.cpp

## 视频预览和裁剪
<img src="https://github.com/ExistNotSee/VideoProject/blob/main/img/video_preview.png" width="200" height="400">
视频预览是在初始化时拿到视频路径后，使用ffmpeg命令获取视频帧图片 (每隔一秒获取一帧) ，然后将图片列表放入自定义view：PreviewSeekbar

> 获取视频帧图片和裁剪的命令请看 FFmpegCommand
