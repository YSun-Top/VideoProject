package com.voidcom.videoproject

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.activity.viewModels
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.videoproject.databinding.ActivityMainBinding
import com.voidcom.videoproject.ui.OutputVideoInfoActivity
import com.voidcom.videoproject.ui.VideoProcessActivity

class MainActivity : BaseActivity<ActivityMainBinding, EmptyViewModel>(), View.OnClickListener {
    private val filePathList = arrayListOf<String>()

    override val mViewModel by viewModels<EmptyViewModel>()

    override fun onInitUI() {
//        startActivity(Intent(this, VideoCutActivity::class.java))
    }

    override fun onInitListener() {
        mBinding.btnTestCmd.setOnClickListener(this)
        mBinding.btnOutputFileInfo.setOnClickListener(this)
        mBinding.btnVideoProcess.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_test_cmd -> {
//                startActivity(Intent(this, VideoCutActivity::class.java))

                val intent = Intent(Intent.ACTION_VIEW)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                intent.setDataAndType(Uri.parse("content://media/external/video/media/59"), "video/mp4")
                startActivity(intent)
            }
            R.id.btn_output_file_info -> startActivity(
                Intent(this, OutputVideoInfoActivity::class.java)
            )
            R.id.btn_video_process -> startActivity(Intent(this, VideoProcessActivity::class.java))
        }
    }
}