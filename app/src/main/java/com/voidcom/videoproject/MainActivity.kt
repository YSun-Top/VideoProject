package com.voidcom.videoproject

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.videoproject.databinding.ActivityMainBinding
import com.voidcom.videoproject.ui.VideoProcessActivity

class MainActivity : BaseActivity<ActivityMainBinding, EmptyViewModel>(), View.OnClickListener {

    override val mViewModel by viewModels<EmptyViewModel>()

    override fun onInitUI() {
//        startActivity(Intent(this, VideoFiltersActivity::class.java))
    }

    override fun onInitListener() {
        mBinding.btnTestCmd.setOnClickListener(this)
        mBinding.btnVideoProcess.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_test_cmd -> {
//                thread {
//                    val a =
//                        FFmpegCmd.getInstance.executeFFmpeg("ffmpeg -i storage/emulated/0/video.mp4 -threads 1 -ss 100 -f image2 -r 1 -t 1 storage/emulated/0/%4d.jpeg")
//                    KLog.debug("--MainActivity--", a)
//                }
            }
            R.id.btn_video_process -> startActivity(Intent(this, VideoProcessActivity::class.java))
        }
    }
}