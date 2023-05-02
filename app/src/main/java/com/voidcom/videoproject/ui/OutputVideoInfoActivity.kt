package com.voidcom.videoproject.ui

import com.huantansheng.easyphotos.EasyPhotos
import com.huantansheng.easyphotos.callback.SelectCallback
import com.huantansheng.easyphotos.constant.Type
import com.huantansheng.easyphotos.models.album.entity.Photo
import com.voidcom.ffmpeglib.FFprobeCmd
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.v_base.utils.KLog
import com.voidcom.videoproject.databinding.ActivityOutputVideoInfoBinding
import com.voidcom.videoproject.utils.GlideEngine

class OutputVideoInfoActivity : BaseActivity<ActivityOutputVideoInfoBinding, EmptyViewModel>() {
    private var filePath = ""

    override val mViewModel: EmptyViewModel by lazy { EmptyViewModel() }

    override fun onInitUI() {
        super.onInitUI()
        mBinding.btnSelectFile.setOnClickListener {
            //EasyPhotos内部做了权限处理，这里不需要申请权限
            EasyPhotos.createAlbum(this, true, true, GlideEngine.newInstant)
                .setCount(1)
                .filter(Type.VIDEO)
                .start(object : SelectCallback() {
                    override fun onResult(photos: ArrayList<Photo>?, isOriginal: Boolean) {
                        if (photos.isNullOrEmpty()) return
                        filePath = photos[0].path
                        runCMD()
                    }

                    override fun onCancel() {
                    }

                })
        }
    }

    private fun runCMD() {
        val cmd = "ffprobe -i $filePath -show_streams -show_format -print_format json"
        FFprobeCmd.getInstance.executeFFprobe(cmd)?.let {
            KLog.d("OutputVideoInfoActivity", it)
            mBinding.tvOutput.text = it
        }
    }
}