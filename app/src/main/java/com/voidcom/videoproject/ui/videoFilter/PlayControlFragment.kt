package com.voidcom.videoproject.ui.videoFilter

import android.os.Handler
import android.os.Looper
import android.view.View
import com.voidcom.v_base.ui.BaseDefaultFragment
import com.voidcom.v_base.utils.KLog
import com.voidcom.v_base.utils.TimeUtils
import com.voidcom.videoproject.R
import com.voidcom.videoproject.databinding.FragmentPlayControlBinding
import com.voidcom.videoproject.model.videoFilter.PlayVideoHandler
import com.voidcom.videoproject.videoProcess.PlayStateListener
import com.voidcom.videoproject.viewModel.videoFilter.PlayControlViewModel

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 * 播放控制
 */
class PlayControlFragment : BaseDefaultFragment<FragmentPlayControlBinding, PlayControlViewModel>(),
    PlayStateListener, View.OnClickListener {
    lateinit var playHandler: PlayVideoHandler
    private val mHandler = Handler(Looper.getMainLooper())

    override val mViewModel: PlayControlViewModel by lazy { PlayControlViewModel() }

    override fun onInitUI() {
    }

    override fun onInitListener() {
        mBinding.playerBtn.setOnClickListener(this)
    }

    override fun onInitData() {
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.playerBtn -> {
                //添加handler防止短时间内多次点击产生无效操作
                mHandler.removeCallbacks(changePlayStatus)
                mHandler.postDelayed(changePlayStatus, 200)
            }
        }
    }

    override fun onPlayStart() {
        setPlayProgress()
        Thread(testTimeRunnable).start()
    }

    override fun onPlayPaused() {
    }

    override fun onPlayStop() {
    }

    override fun onPlayEnd() {
        KLog.d(tag,"-------"+TimeUtils.formatTimeS(timeID))
    }

    override fun onPlayRelease() {
    }

    override fun onPlayTime(time: Long) {
//        KLog.d(tag, "time:$time")
        mHandler.post {
            setPlayProgress()
        }
    }

    /**
     * 设置播放进度
     */
    private fun setPlayProgress() {
        if (!playHandler.isReadyPlay()) return
        context?.let {
            mBinding.videoInfoTv.text = it.getString(
                R.string.playTime,
                TimeUtils.formatTimeS(playHandler.getCurrentTime()),
                TimeUtils.formatTimeS(playHandler.getMaxTime())
            )
            mBinding.timeTv.text = TimeUtils.formatTimeS(timeID)
        }
    }

    private val changePlayStatus = Runnable {
        if (playHandler.isPlaying()) {
            playHandler.plPause()
        } else {
            playHandler.plStart()
        }
    }

    @Volatile
    private var timeID = 0L
    val testTimeRunnable = Runnable {
        while (true){
            timeID += 1000
            Thread.sleep(1000)
        }
    }
}