package com.voidcom.videoproject.viewModel.videoCut

import android.content.Context
import android.net.Uri
import com.huantansheng.easyphotos.EasyPhotos
import com.huantansheng.easyphotos.callback.SelectCallback
import com.huantansheng.easyphotos.constant.Type
import com.huantansheng.easyphotos.models.album.entity.Photo
import com.voidcom.v_base.ui.BaseActivityViewModel
import com.voidcom.videoproject.GlideEngine
import com.voidcom.videoproject.model.videoCut.VideoCutModel
import com.voidcom.videoproject.ui.VideoCutActivity

class VideoCutViewModel : BaseActivityViewModel<VideoCutActivity>() {
    private val m_Model: VideoCutModel by lazy { VideoCutModel() }

    override fun getModel(): VideoCutModel = m_Model

    override fun onInit(context: Context) {
        EasyPhotos.createAlbum(getActivity(), true, true, GlideEngine.newInstant)
            .setFileProviderAuthority("com.example.demo.fileprovider")
            .setCount(1)
            .filter(Type.VIDEO)
            .start(object : SelectCallback() {
                override fun onResult(photos: java.util.ArrayList<Photo>?, isOriginal: Boolean) {
                    if (photos.isNullOrEmpty()) return
                    photos[0].let {
                        m_Model.fileNameStr = it.name
                        m_Model.filePathStr = it.path
                    }
                    getActivity()?.initVideo(Uri.parse(m_Model.filePathStr))
                }

                override fun onCancel() {
                }
            })
    }

    override fun onInitData() {

    }
}