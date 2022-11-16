package com.voidcom.videoproject

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import com.huantansheng.easyphotos.EasyPhotos
import com.huantansheng.easyphotos.callback.SelectCallback
import com.huantansheng.easyphotos.constant.Type
import com.huantansheng.easyphotos.models.album.entity.Photo
import com.voidcom.ffmpeglib.CommandModeCallback
import com.voidcom.ffmpeglib.FFmpegCmd
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.v_base.utils.KLog
import com.voidcom.videoproject.databinding.ActivityMainBinding
import com.voidcom.videoproject.ui.VideoCutActivity
import com.voidcom.videoproject.ui.VideoProcessActivity
import java.util.ArrayList
import kotlin.concurrent.thread

class MainActivity : BaseActivity<ActivityMainBinding, EmptyViewModel>(), View.OnClickListener {

    override val mViewModel by viewModels<EmptyViewModel>()

    override fun onInitUI() {
//        startActivity(Intent(this, VideoFiltersActivity::class.java))
    }

    override fun onInitListener() {
        mBinding.btnTestCmd.setOnClickListener(this)
        mBinding.btnVideoProcess.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_test_cmd -> {
                FFmpegCmd.getInstance.executeFFmpeg("ffprobe -i /storage/emulated/0/video.mp4 -print_format json",
                    object : CommandModeCallback {
                        override fun onFinish() {
                            KLog.d("--CommandModeCallback-", "onFinish")
                        }

                        override fun onError(msg: String) {
                            KLog.d("--CommandModeCallback-", "onError")
                        }

                    })
//                EasyPhotos.createAlbum(this, true, true, GlideEngine.newInstant)
//                    .setFileProviderAuthority("com.example.demo.fileprovider")
//                    .setCount(1)
//                    .filter(Type.VIDEO)
//                    .start(object : SelectCallback() {
//                        override fun onResult(photos: ArrayList<Photo>?, isOriginal: Boolean) {
//                            if (photos.isNullOrEmpty()) return
//                            startActivity(
//                                Intent(
//                                    this@MainActivity,
//                                    VideoCutActivity::class.java
//                                ).apply {
//                                    this.putExtra("path", photos[0])
//                                })
//                        }
//
//                        override fun onCancel() {
//                        }
//                    })
            }
            R.id.btn_video_process -> startActivity(Intent(this, VideoProcessActivity::class.java))
        }
    }
}