package com.voidcom.videoproject

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import com.voidcom.ffmpeglib.FFmpegCmd
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.videoproject.databinding.ActivityMainBinding
import com.voidcom.videoproject.ui.VideoProcessActivity
import com.voidcom.videoproject.ui.videoFilter.VideoFiltersActivity

class MainActivity : BaseActivity<ActivityMainBinding, EmptyViewModel>(), View.OnClickListener {

    override val mViewModel by viewModels<EmptyViewModel>()

    override fun onInitUI() {
//        startActivity(Intent(this, VideoFiltersActivity::class.java))
    }

    override fun onInitListener() {
        mBinding.btnTestCmd.setOnClickListener(this)
        mBinding.btnVideoProcess.setOnClickListener(this)
    }

    override fun onInitData() {
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_test_cmd -> FFmpegCmd.getInstance().executeFFmpeg(arrayOf("ffmpeg", "-hwaccels"))
            R.id.btn_video_process -> startActivity(Intent(this, VideoProcessActivity::class.java))
        }
    }
}