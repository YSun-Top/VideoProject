package com.voidcom.videoproject

import android.view.View
import androidx.activity.viewModels
import com.voidcom.ffmpeglib.FFmpegCmd
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.utils.KLog
import com.voidcom.videoproject.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(),View.OnClickListener {

    override val mViewModel by viewModels<MainViewModel>()

    override fun onInitUI() {
    }

    override fun onInitListener() {
        mBinding.btn.setOnClickListener(this)
    }

    override fun onInitData() {
    }

    override fun onClick(v: View?) {
        KLog.d("onClick")
        val cmdArray = arrayOf("ffmpeg", "-hwaccels")
        FFmpegCmd.getInstance().executeFFmpeg(cmdArray)
    }
}