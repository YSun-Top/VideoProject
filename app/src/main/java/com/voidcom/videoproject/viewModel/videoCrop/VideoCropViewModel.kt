package com.voidcom.videoproject.viewModel.videoCrop

import android.content.Context
import com.voidcom.v_base.ui.BaseViewModel
import com.voidcom.videoproject.model.videoCrop.VideoCropModel

class VideoCropViewModel : BaseViewModel() {
    private val m_Model: VideoCropModel by lazy { VideoCropModel() }

    override fun getModel(): VideoCropModel = m_Model

    override fun onInit(context: Context) {
        getModel().openSelectFileView(context)
    }

    override fun onInitData() {

    }

}