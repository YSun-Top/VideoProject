package com.voidcom.videoproject.viewModel.videoFilter

import android.content.Context
import com.voidcom.v_base.ui.BaseViewModel
import com.voidcom.videoproject.model.videoCut.VideoCutModel

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 */
class VideoFiltersViewModel : BaseViewModel() {
    private val filtersModel: VideoCutModel by lazy { VideoCutModel() }

    override fun getModel(): VideoCutModel = filtersModel

    override fun onInit(context: Context) {
    }

    override fun onInitData() {
    }

}
