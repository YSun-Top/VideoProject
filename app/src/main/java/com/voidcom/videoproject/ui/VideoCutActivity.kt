package com.voidcom.videoproject.ui

import android.app.AlertDialog
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voidcom.ffmpeglib.callback.CommandModeCallback
import com.voidcom.ffmpeglib.FFmpegCmd
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.utils.KLog
import com.voidcom.v_base.utils.ThreadPoolManager
import com.voidcom.v_base.utils.dp2px
import com.voidcom.videoproject.databinding.ActivityTestBinding
import com.voidcom.videoproject.view.RangeSeekBarView
import com.voidcom.videoproject.viewModel.videoCut.VideoCutViewModel
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/**
 *
 * @Description: 视频剪辑
 * @Author: Void
 * @CreateDate: 2022/11/13 21:06
 * @UpdateDate: 2022/11/13 21:06
 */
class VideoCutActivity : BaseActivity<ActivityTestBinding, VideoCutViewModel>() {
    private var TAG = VideoCutActivity::class.java.name
    private var mp: MediaPlayer? = null
    private var mFrames = 0
    private val list = ArrayList<String>()
    private var mWidth = dp2px(35f)
    private var mHeight = dp2px(50f)
    private var mMinTime: Long = 0 * 1000//默认从0s开始
    private var mMaxTime: Long = (MAX_TIME * 1000).toLong()//默认从10s开始，单位是毫秒
    private var mFirstPosition = 0
    private var timer: Timer? = null
    private var timerTaskImp: TimerTaskImp? = null
    private var loadingDialog: AlertDialog? = null
    private val mAdapter by lazy {
        FramesAdapter()
    }

    override val mViewModel: VideoCutViewModel by lazy { VideoCutViewModel() }

    override fun onInitUI() {
        super.onInitUI()
        mViewModel.setActivity(this)
        mBinding.mRangeSeekBarView.post {
            mWidth = mBinding.mRangeSeekBarView.width / MAX_TIME
            mAdapter.setItemWidth(mWidth)//根据seekbar的长度除以我们最大帧数，就是我们每一帧需要的宽度
        }
        mBinding.mRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(this@VideoCutActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = mAdapter
        }
    }

    override fun onInitListener() {
        super.onInitListener()
        mBinding.mRecyclerView.addOnScrollListener(viewScrollListener)
        mBinding.mVideoView.setOnPreparedListener(mediaListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
        timerTaskImp?.cancel()
        timerTaskImp = null
        ThreadPoolManager.instance.executeTask({
            mViewModel.getModel().deleteVideoFrameImageCache(applicationContext)
        })
    }

    /**
     * 播放视频，在视频播放准备完毕后再获取一共有多少帧
     */
    fun initVideo(uri: Uri) {
        mBinding.mVideoView.setVideoURI(uri)
        mBinding.mVideoView.requestFocus()
        mBinding.mVideoView.start()
        startTimer()
    }

    private fun startTimer() {
        if (timer == null) {
            timer = Timer()
            timerTaskImp = TimerTaskImp(this)
            timer?.schedule(timerTaskImp, 0, 100)//数值越小，检查视频播放区间误差越小，但是危害就是性能越卡
        }
    }

    private fun initSeekBar() {
        mBinding.mRangeSeekBarView.selectedMinValue = mMinTime
        mBinding.mRangeSeekBarView.selectedMaxValue = mMaxTime
        mBinding.mRangeSeekBarView.setStartEndTime(mMinTime, mMaxTime)
        mBinding.mRangeSeekBarView.isNotifyWhileDragging = true
        mBinding.mRangeSeekBarView.setOnRangeSeekBarChangeListener(object :
            RangeSeekBarView.OnRangeSeekBarChangeListener {
            override fun onRangeSeekBarValuesChanged(
                bar: RangeSeekBarView,
                minValue: Long,
                maxValue: Long,
                action: Int,
                isMin: Boolean,
                pressedThumb: RangeSeekBarView.Thumb
            ) {
                Log.d("yanjin", "$TAG mMinTime = $minValue mMaxTime = $maxValue")
                mMinTime = minValue + (mFirstPosition * 1000)
                mMaxTime = maxValue + (mFirstPosition * 1000)
                mBinding.mRangeSeekBarView.setStartEndTime(mMinTime, mMaxTime)
                reStartVideo()
            }

        })
    }

    /**
     * 解析视频
     */
    private fun analysisVideo() {
        //先获取多少帧
        mFrames = mBinding.mVideoView.duration / 1000
        Log.d("yanjin", "$TAG mFrames = $mFrames")
        //平凑解析的命令
        gotoGetFrameAtTime(0)
    }

    /**
     * 获取画面帧
     */
    private fun gotoGetFrameAtTime(time: Int) {
        if (time >= mFrames) return //如果超过了就返回，不要截取了
        val cmd = mViewModel.getModel()
            .getVideoFrameImageCommand(applicationContext, mWidth.toInt(), mHeight.toInt())
        thread {
            FFmpegCmd.getInstance.executeFFmpeg(cmd, object : CommandModeCallback {
                override fun onFinish() {
                    Log.d("yanjin", "$TAG 完成")
//                for (x in 0 until mFrames) {
//                    list.add(outfile)
//                }
//                mAdapter.updateList(list)
                    mHandle.post(updatePreviewListRunnable)
                }

                override fun onError(msg: String) {
                }

            })
        }
    }

    /**
     * 重新把视频重头到位播一遍
     */
    private fun reStartVideo() {
        try {
            if (mp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //VideoView.seekTo是有可能不在我们想要的那个时间播放的，因为我们那个时间可能不是关键帧，所以为了解决
                //我们用MediaPlayer.SEEK_CLOSEST，但是这个方法只能在6.0以上
                mp?.seekTo(mMinTime, MediaPlayer.SEEK_CLOSEST)
            } else {
                mBinding.mVideoView.seekTo(mMinTime.toInt())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 每隔1s获取一下视频当前播放位置
     */
    fun getVideoProgress() {
        try {
            val currentPosition = mBinding.mVideoView.currentPosition
//            Log.d("yanjin", "currentPosition = $currentPosition mMaxTime = $mMaxTime")
            if (currentPosition >= mMaxTime) {
                //如果当前时间已经超过我们选取的最大播放位置，那么我们从头播放。
                reStartVideo()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        KLog.d(TAG, "获取画面帧完成")
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
        mAdapter.updateList(pathList)
    }

    private val mediaListener = MediaPlayer.OnPreparedListener {
        mp = it//可以用来seekTo哦
        //设置seekbar
        initSeekBar()
        //解析视频画面帧
        analysisVideo()
    }

    private val viewScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            mFirstPosition =
                (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            Log.d("yanjin", "$TAG mFirstPosition = $mFirstPosition")
            mMinTime = mBinding.mRangeSeekBarView.selectedMinValue + (mFirstPosition * 1000)
            mMaxTime = mBinding.mRangeSeekBarView.selectedMaxValue + (mFirstPosition * 1000)
            mBinding.mRangeSeekBarView.setStartEndTime(mMinTime, mMaxTime)
            mBinding.mRangeSeekBarView.invalidate()
            reStartVideo()
        }
    }

    companion object {
        const val PATH = "path"
        const val MAX_TIME = 10f //最大截取10s，最多展示10帧
    }
}