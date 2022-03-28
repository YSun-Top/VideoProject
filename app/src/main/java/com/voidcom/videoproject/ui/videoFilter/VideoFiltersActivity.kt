package com.voidcom.videoproject.ui.videoFilter

import androidx.activity.viewModels
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.videoproject.databinding.ActivityVideoFiltersBinding
import com.voidcom.videoproject.model.videoFilter.PlayVideoHandler
import com.voidcom.videoproject.viewModel.videoFilter.VideoFiltersViewModel

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 * 视频滤镜
 */
const val KEY_FILE_PATH = "FILEPATH"

class VideoFiltersActivity : BaseActivity<ActivityVideoFiltersBinding, VideoFiltersViewModel>() {
    private val playHandler by lazy { PlayVideoHandler() }

    override val mViewModel: VideoFiltersViewModel by viewModels()

    override fun onInitUI() {
        setFullscreen()
        mBinding.surfaceView.holder.addCallback(playHandler)
        intent.getStringExtra(KEY_FILE_PATH)?.let {
            playHandler.setDataPath(it)
        }
    }

    override fun onInitListener() {
    }

    override fun onInitData() {
    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding.surfaceView.holder.removeCallback(playHandler)
    }
}