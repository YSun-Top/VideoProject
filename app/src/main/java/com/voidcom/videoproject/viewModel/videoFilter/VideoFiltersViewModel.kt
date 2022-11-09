package com.voidcom.videoproject.viewModel.videoFilter

import android.content.Context
import com.voidcom.v_base.ui.BaseViewModel
import com.voidcom.videoproject.model.videoCrop.VideoCropModel

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 */
class VideoFiltersViewModel : BaseViewModel() {
    private val filtersModel: VideoCropModel by lazy { VideoCropModel() }

    override fun getModel(): VideoCropModel = filtersModel

    override fun onInit(context: Context) {
        filtersModel.openSelectFileView(context)
    }

    override fun onInitData() {
    }

}
