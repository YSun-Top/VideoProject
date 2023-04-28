package com.voidcom.videoproject.ui.rtp

import android.os.Bundle
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.videoproject.databinding.ActivityPushRtmpBinding

/**
 * 实现一个RTMP推流到服务端，然后在电脑拉流播放的功能
 */
class PushRTMPActivity:BaseActivity<ActivityPushRtmpBinding,EmptyViewModel>() {
    override val mViewModel: EmptyViewModel by lazy { EmptyViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

}