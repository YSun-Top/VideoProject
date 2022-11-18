package com.voidcom.videoproject.ui

import android.net.Uri
import android.view.View
import androidx.activity.viewModels
import com.huantansheng.easyphotos.EasyPhotos
import com.huantansheng.easyphotos.callback.SelectCallback
import com.huantansheng.easyphotos.constant.Type
import com.huantansheng.easyphotos.models.album.entity.Photo
import com.voidcom.ffmpeglib.FFmpegCmd
import com.voidcom.ffmpeglib.FFprobeCmd
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.utils.GsonUtils
import com.voidcom.v_base.utils.KLog
import com.voidcom.v_base.utils.ToastUtils
import com.voidcom.videoproject.GlideEngine
import com.voidcom.videoproject.databinding.ActivityVideoCropBinding
import com.voidcom.videoproject.viewModel.videoCut.VideoCutViewModel
import kotlin.concurrent.thread

class VideoCropActivity : BaseActivity<ActivityVideoCropBinding, VideoCutViewModel>() {
    private val TAG = VideoCropActivity::class.simpleName
    private var timeDuration = 0f   //视频时长，单位s

    override val mViewModel by viewModels<VideoCutViewModel>()

    override fun onInitUI() {
        EasyPhotos.createAlbum(this, true, true, GlideEngine.newInstant)
            .setFileProviderAuthority("com.voidcom.videoproject.fileprovider")
            .setCount(1)
            .filter(Type.VIDEO)
            .start(SelectFileCallback())
    }

    override fun onInitListener() {
        mBinding.btnExecute.setOnClickListener(btnExecuteClick)
    }

    private fun getTime(type: Int): Int {
        val time = when (type) {
            0 -> mBinding.etStartTime.text.toString().toInt()
            1 -> mBinding.etEndTime.text.toString().toInt()
            else -> -1
        }
        return time
    }

    private fun searchVideoDuration() {
        val json = FFprobeCmd.getInstance.executeFFprobe(mViewModel.getModel().getVideoDurationCommand())
        if (json == null) {
            KLog.w(TAG, "获取视频时长失败，返回数据为空")
            return
        }
        val format = GsonUtils.getStringFromJSON(json, "format")
        timeDuration = GsonUtils.getStringFromJSON(format, "duration").toFloatOrNull() ?: 0f

        if (timeDuration > 0L) {
            mHandle.post {
                mBinding.btnExecute.isClickable = true
                mBinding.etEndTime.hint = timeDuration.toString()
            }
        }
        if (mViewModel.getModel().checkOutputFile(applicationContext)) {
            KLog.w(TAG, "删除旧视频预览图片文件")
        }
        FFmpegCmd.getInstance.executeFFmpeg(
            mViewModel.getModel().getVideoFrameImageCommand(10, applicationContext)
        )
        mHandle.post {
            val uri =
                Uri.parse(mViewModel.getModel().getVideoFrameOutputPath(applicationContext))
            mBinding.ivFrameImage.setImageURI(uri)
        }

    }

    private val btnExecuteClick = View.OnClickListener {
    }

    inner class SelectFileCallback : SelectCallback() {
        override fun onResult(photos: ArrayList<Photo>?, isOriginal: Boolean) {
            if (photos.isNullOrEmpty()) {
                ToastUtils.showShort(applicationContext, "文件获取失败")
                return
            }
            photos[0].run {
                KLog.d(msg = "fileName:${name}; path:${path}")
                mBinding.tvFileName.text =
                    path.substring(path.indexOfLast { it == '/' } + 1, path.length)
                mViewModel.getModel().filePathStr = path
            }
            thread {
                searchVideoDuration()
            }
        }

        override fun onCancel() {
        }
    }
}