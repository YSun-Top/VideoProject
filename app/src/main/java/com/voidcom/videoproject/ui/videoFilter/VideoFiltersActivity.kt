package com.voidcom.videoproject.ui.videoFilter

import androidx.activity.viewModels
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.videoproject.databinding.ActivityVideoFiltersBinding
import com.voidcom.videoproject.model.videoFilter.PlayVideoHandler

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 * 视频播放器
 */
const val KEY_FILE_PATH = "FILEPATH"

class VideoFiltersActivity : BaseActivity<ActivityVideoFiltersBinding, VideoFiltersViewModel>() {
    private val playHandler by lazy { PlayVideoHandler() }

    override val mViewModel: VideoFiltersViewModel by viewModels()

    override fun onInitUI() {
        mBinding.surfaceView.holder.addCallback(playHandler)
        val filePath = intent.getStringExtra(KEY_FILE_PATH)
//        playHandler.
    }

    override fun onInitListener() {
    }

    override fun onInitData() {
    }

}