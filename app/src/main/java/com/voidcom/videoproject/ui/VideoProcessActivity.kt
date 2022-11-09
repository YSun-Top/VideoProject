package com.voidcom.videoproject.ui

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.videoproject.R
import com.voidcom.videoproject.databinding.ActivityVideoProcessBinding
import com.voidcom.videoproject.ui.videoFilter.VideoFiltersActivity

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 * 视频处理列表
 */
class VideoProcessActivity : BaseActivity<ActivityVideoProcessBinding, EmptyViewModel>(),
    View.OnClickListener {

    override val mViewModel by viewModels<EmptyViewModel>()

    override fun onInitUI() {
    }

    override fun onInitListener() {
        mBinding.btnVideoFilter.setOnClickListener(this)
    }

    override fun onInitData() {
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_video_filter -> startActivity(Intent(this, VideoFiltersActivity::class.java))
            R.id.btn_video_crop->{}
        }
    }
}