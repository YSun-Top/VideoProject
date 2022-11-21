package com.voidcom.videoproject.ui

import android.media.MediaPlayer
import android.os.Build
import android.widget.SeekBar
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.videoproject.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity : BaseActivity<ActivityVideoPlayerBinding, EmptyViewModel>() {
    private lateinit var playPath: String
    private var mp: MediaPlayer? = null

    override val mViewModel: EmptyViewModel by lazy { EmptyViewModel() }

    override fun onInitUI() {
        super.onInitUI()
        playPath = intent.getStringExtra(PATH) ?: ""
        mBinding.mVideoView.setVideoPath(playPath)
        mBinding.mVideoView.requestFocus()
        mBinding.mVideoView.start()
    }

    override fun onInitListener() {
        super.onInitListener()
        mBinding.mVideoView.setOnPreparedListener(mediaListener)
        mBinding.mSeekbar.setOnSeekBarChangeListener(seekBarListener)
    }

    private val mediaListener = MediaPlayer.OnPreparedListener {
        mp = it
        mBinding.mSeekbar.max = mBinding.mVideoView.duration / 1000
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

    companion object {
        const val PATH = "PATH"
    }
}