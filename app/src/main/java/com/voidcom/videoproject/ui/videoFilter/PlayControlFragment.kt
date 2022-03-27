package com.voidcom.videoproject.ui.videoFilter

import com.voidcom.v_base.ui.BaseDefaultFragment
import com.voidcom.videoproject.databinding.FragmentPlayControlBinding

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 * 播放控制
 */
class PlayControlFragment : BaseDefaultFragment<FragmentPlayControlBinding, PlayControlViewModel>() {

    override val mViewModel: PlayControlViewModel by lazy { PlayControlViewModel() }

    override fun onInitUI() {
    }

    override fun onInitListener() {
    }

    override fun onInitData() {
    }
}