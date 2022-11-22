package com.voidcom.videoproject.ui

import android.media.MediaPlayer
import android.os.Build
import android.widget.SeekBar
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.v_base.utils.KLog
import com.voidcom.videoproject.databinding.ActivityVideoPlayerBinding
import kotlinx.coroutines.Runnable

class VideoPlayerActivity : BaseActivity<ActivityVideoPlayerBinding, EmptyViewModel>() {
    private lateinit var playPath: String
    private var mp: MediaPlayer? = null
    private var isPlay = false

    override val mViewModel: EmptyViewModel by lazy { EmptyViewModel() }

    override fun onInitUI() {
        super.onInitUI()
        playPath = intent.getStringExtra(PATH) ?: ""
        KLog.d("VideoPlayerActivity", "视频播放地址:$playPath")
        mBinding.mVideoView.setVideoPath(playPath)
        mBinding.mVideoView.requestFocus()
        mBinding.mVideoView.start()
    }

    override fun onInitListener() {
        super.onInitListener()
        mBinding.mVideoView.setOnPreparedListener(mediaListener)
        mBinding.mVideoView.setOnCompletionListener {
            isPlay=false
        }
        mBinding.mSeekbar.setOnSeekBarChangeListener(seekBarListener)
    }

    private val mediaListener = MediaPlayer.OnPreparedListener {
        mp = it
        mBinding.mSeekbar.max = mBinding.mVideoView.duration / 1000
        isPlay = true
        Thread(processUpdateRunnable).start()
    }

    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mp?.seekTo(progress * 1000L, MediaPlayer.SEEK_CLOSEST)
            } else {
                mp?.seekTo(progress * 1000)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }

    }

    private val processUpdateRunnable = Runnable {
        while (isPlay) {
            mBinding.mSeekbar.progress = mBinding.mVideoView.currentPosition / 1000
            Thread.sleep(500)
        }
    }

    companion object {
        const val PATH = "PATH"
    }
}