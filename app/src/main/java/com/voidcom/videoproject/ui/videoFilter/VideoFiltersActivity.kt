package com.voidcom.videoproject.ui.videoFilter

import android.text.TextUtils
import androidx.activity.viewModels
import com.voidcom.videoproject.R
import com.voidcom.videoproject.databinding.ActivityVideoFiltersBinding
import com.voidcom.videoproject.model.videoFilter.PlayVideoHandler
import com.voidcom.videoproject.ui.ReadStorageActivity
import com.voidcom.videoproject.viewModel.videoFilter.VideoFiltersViewModel
import kotlinx.coroutines.Runnable

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 * 视频滤镜
 */
class VideoFiltersActivity : ReadStorageActivity<ActivityVideoFiltersBinding, VideoFiltersViewModel>() {
    private val playHandler by lazy { PlayVideoHandler() }
    private lateinit var filtersFragment: FiltersFragment
    private lateinit var playControlFragment: PlayControlFragment

    override val mViewModel: VideoFiltersViewModel by viewModels()

    override fun onInitUI() {
        super.onInitUI()
        setFullscreen()
        filtersFragment =
            supportFragmentManager.findFragmentById(R.id.filtersFragment) as FiltersFragment
        filtersFragment.playHandler = playHandler
        playControlFragment =
            supportFragmentManager.findFragmentById(R.id.playControlFragment) as PlayControlFragment
        playControlFragment.playHandler = playHandler
        playHandler.listener = playControlFragment
    }

    override fun onInitListener() {
        mViewModel.getModel().register =getFilePathCallbackRegister()
    }

    override fun onFilePathCallback(path: String) {
        mViewModel.getModel().pathStr = path
        mHandle.postDelayed(onPlayRunnable, 1500)
    }

    override fun onStart() {
        super.onStart()
        mBinding.surfaceView.holder.addCallback(playHandler)
    }

    override fun onStop() {
        super.onStop()
        if (mViewModel.getModel().pathStr.isEmpty()) return
        playHandler.stopTimeUpdateThread()
        playHandler.plPause()
        playHandler.release()
        mHandle.removeCallbacks(onPlayRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding.surfaceView.holder.removeCallback(playHandler)
    }

    private val onPlayRunnable = Runnable {
        if (TextUtils.isEmpty(mViewModel.getModel().pathStr)) return@Runnable
        playHandler.setDataPath(mViewModel.getModel().pathStr)
    }
}