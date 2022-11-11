package com.voidcom.videoproject.ui

import android.net.Uri
import android.view.View
import androidx.activity.viewModels
import com.voidcom.ffmpeglib.FFmpegCmd
import com.voidcom.ffmpeglib.FFprobeCmd
import com.voidcom.v_base.utils.GsonUtils
import com.voidcom.v_base.utils.KLog
import com.voidcom.videoproject.databinding.ActivityVideoCropBinding
import com.voidcom.videoproject.viewModel.videoCrop.VideoCropViewModel
import kotlin.concurrent.thread

class VideoCropActivity : ReadStorageActivity<ActivityVideoCropBinding, VideoCropViewModel>() {
    private val TAG = VideoCropActivity::class.simpleName
    private var timeDuration = 0f   //视频时长，单位s

    override val mViewModel by viewModels<VideoCropViewModel>()

    override fun onInitListener() {
        mViewModel.getModel().register = getFilePathCallbackRegister()
        mBinding.btnExecute.setOnClickListener(btnExecuteClick)
    }

    override fun onFilePathCallback(path: String) {
        mBinding.tvFileName.text = path.substring(path.indexOfLast { it == '/' } + 1, path.length)
        mViewModel.getModel().pathStr = path
        thread {
            val json =
                FFprobeCmd.getInstance.executeFFprobe(mViewModel.getModel().getVideoDuration())
            if (json == null) {
                KLog.w(TAG, "获取视频时长失败，返回数据为空")
                return@thread
            }
            val format = GsonUtils.getStringFromJSON(json, "format")
            timeDuration = GsonUtils.getStringFromJSON(format, "duration").toFloatOrNull() ?: 0f

            if (timeDuration > 0L) {
                mHandle.post {
                    mBinding.btnExecute.isClickable = true
                    mBinding.etEndTime.hint = timeDuration.toString()
                }
            }
            if (mViewModel.getModel().checkOutputFile(applicationContext)){
                KLog.w(TAG,"删除旧文件")
            }
            val result = FFmpegCmd.getInstance.executeFFmpeg(
                mViewModel.getModel().getVideoFrameImage(10, applicationContext)
            )
//            if (result != 0) return@thread
            mHandle.post {
                val uri =
                    Uri.parse(mViewModel.getModel().getVideoFrameOutputPath(applicationContext))
                mBinding.ivFrameImage.setImageURI(uri)
            }
        }
    }

    private fun getTime(type: Int): Int {
        val time = when (type) {
            0 -> mBinding.etStartTime.text.toString().toInt()
            1 -> mBinding.etEndTime.text.toString().toInt()
            else -> -1
        }
        return time
    }

    private val btnExecuteClick = View.OnClickListener {
    }
}