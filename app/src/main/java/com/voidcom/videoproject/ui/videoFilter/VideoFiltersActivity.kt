package com.voidcom.videoproject.ui.videoFilter

import android.text.TextUtils
import androidx.activity.viewModels
import com.huantansheng.easyphotos.EasyPhotos
import com.huantansheng.easyphotos.callback.SelectCallback
import com.huantansheng.easyphotos.constant.Type
import com.huantansheng.easyphotos.models.album.entity.Photo
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.utils.ToastUtils
import com.voidcom.videoproject.GlideEngine
import com.voidcom.videoproject.R
import com.voidcom.videoproject.databinding.ActivityVideoFiltersBinding
import com.voidcom.videoproject.model.videoFilter.PlayVideoHandler
import com.voidcom.videoproject.viewModel.videoFilter.VideoFiltersViewModel
import kotlinx.coroutines.Runnable
import java.util.ArrayList

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 * 视频滤镜
 */
class VideoFiltersActivity : BaseActivity<ActivityVideoFiltersBinding, VideoFiltersViewModel>() {
    private val playHandler by lazy { PlayVideoHandler() }
    private lateinit var filtersFragment: FiltersFragment
    private lateinit var playControlFragment: PlayControlFragment

    override val mViewModel: VideoFiltersViewModel by viewModels()

    override fun onInitUI() {
        EasyPhotos.createAlbum(this, true, true, GlideEngine.newInstant)
            .setFileProviderAuthority("com.voidcom.videoproject.fileprovider")
            .setCount(1)
            .filter(Type.VIDEO)
            .start(SelectFileCallback())
        setFullscreen()
        filtersFragment =
            supportFragmentManager.findFragmentById(R.id.filtersFragment) as FiltersFragment
        filtersFragment.playHandler = playHandler
        playControlFragment =
            supportFragmentManager.findFragmentById(R.id.playControlFragment) as PlayControlFragment
        playControlFragment.playHandler = playHandler
        playHandler.listener = playControlFragment
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

    inner class SelectFileCallback : SelectCallback() {
        override fun onResult(photos: ArrayList<Photo>?, isOriginal: Boolean) {
            if (photos.isNullOrEmpty()) {
                ToastUtils.showShort(applicationContext, "文件获取失败")
                return
            }
            mViewModel.getModel().pathStr = photos[0].path
            mHandle.postDelayed(onPlayRunnable, 1500)
        }

        override fun onCancel() {
        }

    }

    private val onPlayRunnable = Runnable {
        if (TextUtils.isEmpty(mViewModel.getModel().pathStr)) return@Runnable
        playHandler.setDataPath(mViewModel.getModel().pathStr)
    }
}