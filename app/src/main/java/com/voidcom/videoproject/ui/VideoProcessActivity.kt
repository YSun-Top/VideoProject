package com.voidcom.videoproject.ui

import android.content.Intent
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.v_base.ui.PermissionRequestActivity
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.PermissionsUtils
import com.voidcom.v_base.utils.visible
import com.voidcom.v_base.viewModel.PermissionRequestViewModel
import com.voidcom.videoproject.R
import com.voidcom.videoproject.databinding.ActivityVideoProcessBinding
import com.voidcom.videoproject.ui.videoFilter.VideoFiltersActivity

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 * 视频处理列表
 */
class VideoProcessActivity : BaseActivity<ActivityVideoProcessBinding, EmptyViewModel>(),
    View.OnClickListener {

    override val mViewModel by viewModels<EmptyViewModel>()

    override fun onInitUI() {
        //申请检查权限，没有权限就申请权限
        PermissionsUtils.checkPermission(applicationContext, AppCode.requestReadStorage).let {
            if (it.isEmpty()){
                mBinding.btnVideoFilter.visible()
                mBinding.btnVideoCrop.visible()
                return@let
            }
            PermissionRequestActivity.newInstance(
                registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                    permissionCallback
                ), 1000, AppCode.requestReadStorage
            )
        }
    }

    override fun onInitListener() {
        mBinding.btnVideoFilter.setOnClickListener(this)
        mBinding.btnVideoCrop.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_video_filter -> startActivity(Intent(this, VideoFiltersActivity::class.java))
            R.id.btn_video_crop -> startActivity(Intent(this, VideoCropActivity::class.java))
        }
    }

    private val permissionCallback = ActivityResultCallback<ActivityResult> { result ->
        if (result.resultCode == RESULT_FIRST_USER) {
            result.data?.getBooleanExtra(
                PermissionRequestViewModel.permissionsResultStatus,
                false
            ).let {
                if (it == false)
                    finish()
                else {
                    mBinding.btnVideoFilter.visible()
                    mBinding.btnVideoCrop.visible()
                }
            }
        }
    }
}