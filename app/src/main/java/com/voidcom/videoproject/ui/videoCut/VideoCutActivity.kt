package com.voidcom.videoproject.ui.videoCut

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.voidcom.ffmpeglib.FFmpegCmd
import com.voidcom.ffmpeglib.callback.CommandModeCallback
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.utils.*
import com.voidcom.videoproject.databinding.ActivityVideoCutBinding
import com.voidcom.videoproject.view.PreviewSeekbar
import com.voidcom.videoproject.viewModel.videoCut.VideoCutViewModel
import java.io.File
import kotlin.concurrent.thread

/**
 *
 * @Description: 视频剪辑
 * @Author: Void
 * @CreateDate: 2022/11/13 21:06
 * @UpdateDate: 2022/11/13 21:06
 */
class VideoCutActivity : BaseActivity<ActivityVideoCutBinding, VideoCutViewModel>(),
    PreviewSeekbar.SeekbarChangeListener {
    private var TAG = VideoCutActivity::class.java.name
    private var mp: MediaPlayer? = null
    private var mFrames = 0
    private var mWidth = 100f
    private var mHeight = 50f
    private var mFirstPosition = 0
    private val MAX_TIME = 10 //最大截取10s，最多展示10帧
    private var leftProcess = 0L
    private var rightProcess = 0L

    override val mViewModel: VideoCutViewModel by lazy { VideoCutViewModel() }

    override fun onInitUI() {
        super.onInitUI()
        mViewModel.setActivity(this)
    }

    override fun onInitListener() {
        super.onInitListener()
        mBinding.mVideoView.setOnPreparedListener(mediaListener)
        mBinding.mPreviewSeekBarView.setChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ThreadPoolManager.instance.executeTask({
            mViewModel.getModel().deleteVideoFrameImageCache(applicationContext)
        })
    }

    override fun onChange(type: PreviewSeekbar.ClickIconType, leftValue: Float, rightValue: Float) {
    }

    override fun onChangeComplete(leftValue: Float, rightValue: Float) {
        leftProcess = leftValue.toLong()
        rightProcess = rightValue.toLong()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            mp?.seekTo(leftProcess*1000,MediaPlayer.SEEK_CLOSEST)
        }else{
            mp?.seekTo((leftProcess*1000).toInt())
        }
    }

    /**
     * 播放视频，在视频播放准备完毕后再获取一共有多少帧
     */
    fun initVideo(uri: Uri) {
        mBinding.mVideoView.setVideoURI(uri)
        mBinding.mVideoView.requestFocus()
    }

    /**
     * 解析视频
     */
    private fun analysisVideo() {
        //先获取多少帧
        mFrames = mBinding.mVideoView.duration / 1000
        Log.d(TAG, "当前视频帧数:$mFrames")
        mBinding.mPreviewSeekBarView.setMaxValue(mFrames)
        mBinding.mPreviewSeekBarView.setPreviewImgWidth(mWidth, mHeight)
        thread {
            gotoGetFrameAtTime()
        }
    }

    /**
     * 获取画面帧
     */
    private fun gotoGetFrameAtTime() {
        if (isMainThread()) {
            throw RuntimeException("获取图像帧是耗时操作，请勿在主线程执行")
        }
        val cmd = mViewModel.getModel()
            .getVideoFrameImageCommand(applicationContext, mWidth.toInt(), mHeight.toInt())
        FFmpegCmd.getInstance.executeFFmpeg(cmd, object : CommandModeCallback {
            override fun onFinish() {
                mHandle.post(updatePreviewListRunnable)
            }

            override fun onError(msg: String) {
            }

        })
    }

    /**
     * 执行剪辑视频；作
     */
    private fun trimVideo() {
//        if (mCurrentSubscriber != null && !mCurrentSubscriber?.isDisposed!!) {
//            mCurrentSubscriber?.dispose()
//        }

//        var outfile = "$mCacheRootPath/${Utils.getFileName(resouce.name)}_trim.mp4 "
//        var start: Float = mMinTime / 1000f
//        var end: Float = mMaxTime / 1000f
//        var cmd =
//            "ffmpeg -ss " + start + " -to " + end + " -accurate_seek" + " -i " + resouce?.path + " -to " + (end - start) + " -preset " + "superfast" + " -crf 23 -c:a copy -avoid_negative_ts 0 -y " + outfile;
//        val commands = cmd.split(" ").toTypedArray()
        try {
//            RxFFmpegInvoke.getInstance()
//                .runCommandRxJava(commands)
//                .subscribe(object : MyRxFFmpegSubscriber() {
//                    override fun onFinish() {
//                        if (loadingDialog != null && loadingDialog?.isShowing!!) {
//                            loadingDialog?.dismiss()
//                        }
//                        finish()
//                        Log.d("yanjin", "$TAG 完成截取 outfile = ${outfile}")
//                    }
//
//                    override fun onProgress(progress: Int, progressTime: Long) {
//                        Log.d("yanjin", "$TAG 截取进度 progress = ${progress}")
//                    }
//                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val updatePreviewListRunnable = Runnable {
        ToastUtils.showShort(applicationContext, "获取视频预览图片完成")
        val folder = File(mViewModel.getModel().getVideoFrameImagePath(applicationContext))
        val fileList = folder.listFiles()
        if (!folder.exists() || fileList.isNullOrEmpty()) {
            KLog.e(TAG, "视频预览图片不存在")
            return@Runnable
        }
        val pathList = ArrayList<String>()
        fileList.forEach {
            pathList.add(it.path)
        }
        mBinding.mVideoView.start()
        mBinding.mProgressBar.gone()
        mBinding.mPreviewSeekBarView.visible()
        mBinding.mPreviewSeekBarView.setFilePathArray(pathList)
    }

    private val mediaListener = MediaPlayer.OnPreparedListener {
        mp = it
        //解析视频画面帧
        analysisVideo()
    }
}