package com.voidcom.videoproject.ui.videoCut

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import com.voidcom.ffmpeglib.FFmpegCmd
import com.voidcom.ffmpeglib.callback.CommandModeCallback
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.utils.*
import com.voidcom.videoproject.R
import com.voidcom.videoproject.databinding.ActivityVideoCutBinding
import com.voidcom.videoproject.ui.VideoPlayerActivity
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
    PreviewSeekbar.SeekbarChangeListener, OnClickListener {
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
        mBinding.btnCut.setOnClickListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ThreadPoolManager.instance.executeTask({
            mViewModel.getModel().let {
                it.deleteFileAndFolder(it.getVideoFrameImagePath(applicationContext))
//                it.deleteFileAndFolder(it.getVideoCutPath(applicationContext))
            }
        })
    }

    override fun onChange(type: PreviewSeekbar.ClickIconType, leftValue: Float, rightValue: Float) {
    }

    override fun onChangeComplete(leftValue: Float, rightValue: Float) {
        leftProcess = leftValue.toLong()
        rightProcess = rightValue.toLong()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mp?.seekTo(leftProcess * 1000, MediaPlayer.SEEK_CLOSEST)
        } else {
            mp?.seekTo((leftProcess * 1000).toInt())
        }
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.btn_cut) {
            trimVideo()
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
                ToastUtils.showShortInMainThread(applicationContext, "获取预览图片失败")
            }

        })
    }

    /**
     * 执行剪辑视频；作
     */
    private fun trimVideo() {
        mBinding.mProgressBar.visible()
        val cmd = mViewModel.getModel().run {
            //如果文件存在，删除该视频文件
            this.deleteFileAndFolder(this.getVideoCutPath(applicationContext))
            this.videoCutCommand(applicationContext, leftProcess, rightProcess)
        }
        thread {
            FFmpegCmd.getInstance.executeFFmpeg(cmd, object : CommandModeCallback {
                override fun onFinish() {
                    mHandle.post(videoCutRunnable)
                }

                override fun onError(msg: String) {
                    ToastUtils.showShortInMainThread(applicationContext, "视频剪辑失败")
                }
            })
        }
    }

    private val mediaListener = MediaPlayer.OnPreparedListener {
        mp = it
        //解析视频画面帧
        analysisVideo()
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

    private val videoCutRunnable = Runnable {
        mBinding.mProgressBar.gone()
        ToastUtils.showShort(applicationContext, "视频剪辑成功")
        startActivity(Intent(this, VideoPlayerActivity::class.java).apply {
            putExtra(
                VideoPlayerActivity.PATH,
                mViewModel.getModel().getVideoCutPath(applicationContext) + "/cutVideoFile.mp4"
            )
        })
    }
}