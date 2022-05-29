package com.voidcom.videoproject.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.v_base.ui.PermissionRequestActivity
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.FileTools
import com.voidcom.v_base.utils.PermissionsUtils
import com.voidcom.v_base.utils.ToastUtils
import com.voidcom.videoproject.R
import com.voidcom.videoproject.databinding.ActivityVideoProcessBinding
import com.voidcom.videoproject.ui.videoFilter.KEY_FILE_PATH
import com.voidcom.videoproject.ui.videoFilter.VideoFiltersActivity

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 * 视频处理列表
 */
class VideoProcessActivity : BaseActivity<ActivityVideoProcessBinding, EmptyViewModel>(),
    View.OnClickListener {
    private lateinit var register: ActivityResultLauncher<Intent>
    private val getFileUriCallback = ActivityResultCallback<ActivityResult> {
        when {
            it.resultCode != RESULT_OK -> ToastUtils.showShort(applicationContext, "文件获取失败")
            it.data == null -> ToastUtils.showShort(applicationContext, "文件路径为空")
            else -> {
                var path = ""
                try {
                    val uri = it.data?.data as Uri
                    path = FileTools.getFilePathByUri(applicationContext, uri) ?: ""
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (TextUtils.isEmpty(path)) return@ActivityResultCallback
                startActivity(Intent(this, VideoFiltersActivity::class.java).apply {
                    putExtra(KEY_FILE_PATH, path)
                })
            }
        }
    }
    private val permissionCallback = ActivityResultCallback<ActivityResult> { result ->
        when (result.resultCode) {
            RESULT_FIRST_USER -> {
                result.data?.getBooleanExtra(
                    PermissionRequestActivity.permissionsResultStatus,
                    false
                ).let {
                    if (it == false) finish()
                }
            }
        }
    }

    private val permissions = arrayOf(
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.CAMERA"
    )

    override val mViewModel by viewModels<EmptyViewModel>()

    override fun onInitUI() {
        PermissionsUtils.checkPermission(applicationContext, AppCode.requestReadStorage).let {
            if (it.isEmpty()) return@let
            PermissionRequestActivity.newInstance(
                registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                    permissionCallback
                ), 1000, AppCode.requestReadStorage
            )
        }
    }

    override fun onInitListener() {
        register = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            getFileUriCallback
        )
        mBinding.btnVideoFilter.setOnClickListener(this)
    }

    override fun onInitData() {
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_video_filter -> openSelectFileView()
        }
    }

    private fun openSelectFileView() {
        try {
            register.launch(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, "选择视频文件"))
        } catch (ex: ActivityNotFoundException) {
            ToastUtils.showShort(applicationContext, "没有找到文件管理器！")
        }
    }
}