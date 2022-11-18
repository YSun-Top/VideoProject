package com.voidcom.videoproject

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import com.huantansheng.easyphotos.EasyPhotos
import com.huantansheng.easyphotos.callback.SelectCallback
import com.huantansheng.easyphotos.constant.Type
import com.huantansheng.easyphotos.models.album.entity.Photo
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.v_base.utils.KLog
import com.voidcom.videoproject.databinding.ActivityMainBinding
import com.voidcom.videoproject.ui.VideoCutActivity
import com.voidcom.videoproject.ui.VideoProcessActivity
import com.voidcom.videoproject.view.PreviewSeekbar
import java.io.File
import java.util.ArrayList

class MainActivity : BaseActivity<ActivityMainBinding, EmptyViewModel>(), View.OnClickListener {
    private val filePathList = arrayListOf<String>()

    override val mViewModel by viewModels<EmptyViewModel>()

    override fun onInitUI() {
//        startActivity(Intent(this, VideoFiltersActivity::class.java))
        val folderPath = "${applicationContext.cacheDir.path}/VideoFrameImage"
        File(folderPath).list()?.forEach {
            filePathList.add("$folderPath/$it")
        }
        filePathList.forEach {
            KLog.d(msg = it)
        }
        mBinding.seekBar.setFilePathArray(filePathList)
//        mBinding.ivImage.setImageURI(Uri.parse(filePathList[0]))
//        mBinding.seekBar.setOnSeekBarChangeListener(seekBarListener)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            mBinding.seekBar.min = 0
//            mBinding.seekBar.max = filePathList.size-1
//        }
    }

    override fun onInitListener() {
        mBinding.btnTestCmd.setOnClickListener(this)
        mBinding.btnVideoProcess.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_test_cmd -> {
//                FFmpegCmd.getInstance.executeFFmpeg("ffprobe -i /storage/emulated/0/video.mp4 -print_format json",
//                    object : CommandModeCallback {
//                        override fun onFinish() {
//                            KLog.d("--CommandModeCallback-", "onFinish")
//                        }
//
//                        override fun onError(msg: String) {
//                            KLog.d("--CommandModeCallback-", "onError")
//                        }
//
//                    })
                startActivity(Intent(this, VideoCutActivity::class.java))
            }
            R.id.btn_video_process -> startActivity(Intent(this, VideoProcessActivity::class.java))
        }
    }

    private val listener = object : PreviewSeekbar.SeekbarChangeListener {
        override fun onChange(
            type: PreviewSeekbar.ClickIconType,
            leftValue: Float,
            rightValue: Float
        ) {
//            mBinding.ivImage.setImageURI(Uri.parse(filePathList[progress]))
        }
        override fun onChangeComplete(leftValue: Int, rightValue: Int) {
        }

    }
}