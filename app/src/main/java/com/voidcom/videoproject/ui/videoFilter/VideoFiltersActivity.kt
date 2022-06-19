package com.voidcom.videoproject.ui.videoFilter

import android.text.TextUtils
import androidx.activity.viewModels
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.videoproject.R
import com.voidcom.videoproject.databinding.ActivityVideoFiltersBinding
import com.voidcom.videoproject.model.videoFilter.PlayVideoHandler
import com.voidcom.videoproject.viewModel.videoFilter.VideoFiltersViewModel
import kotlinx.coroutines.Runnable

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 * 视频滤镜
 */
class VideoFiltersActivity : BaseActivity<ActivityVideoFiltersBinding, VideoFiltersViewModel>() {
    private val playHandler by lazy { PlayVideoHandler() }
    private lateinit var filtersFragment: FiltersFragment
    private lateinit var playControlFragment: PlayControlFragment

    private var pathStr = ""

    override val mViewModel: VideoFiltersViewModel by viewModels()

    override fun onInitUI() {
        setFullscreen()
        mBinding.surfaceView.holder.addCallback(playHandler)
        pathStr = intent.getStringExtra(KEY_FILE_PATH) ?: ""
        mHandle.postDelayed(onPlayRunnable, 1500)
        filtersFragment =
            supportFragmentManager.findFragmentById(R.id.filtersFragment) as FiltersFragment
        filtersFragment.playHandler = playHandler
        playControlFragment =
            supportFragmentManager.findFragmentById(R.id.playControlFragment) as PlayControlFragment
        playControlFragment.playHandler = playHandler
        playHandler.listener = playControlFragment
    }

    override fun onInitListener() {
    }

    override fun onInitData() {
    }

    override fun onStop() {
        super.onStop()
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
        if (TextUtils.isEmpty(pathStr)) return@Runnable
        playHandler.setDataPath(pathStr)
    }
    companion object{
        const val KEY_FILE_PATH = "FILE_PATH"
    }
}