package com.voidcom.videoproject.ui.rtp

import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.v_base.ui.PermissionRequestActivity
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.visible
import com.voidcom.v_base.viewModel.PermissionRequestViewModel
import com.voidcom.videoproject.R
import com.voidcom.videoproject.databinding.ActivityPushRtmpBinding

/**
 * 实现一个RTMP推流到服务端，然后在电脑拉流播放的功能
 * 摄像头界面，包含推流控制和摄像头切换
 */
class PushRTMPActivity : BaseActivity<ActivityPushRtmpBinding, EmptyViewModel>(),
    OnCheckedChangeListener {
    override val mViewModel: EmptyViewModel by lazy { EmptyViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //请求权限
        PermissionRequestActivity.newInstance(
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                permissionCallback
            ), 1000, AppCode.requestCamera
        )


        mBinding.btnSwitchCamera.setOnClickListener {

        }
        mBinding.tgBtnControl.setOnCheckedChangeListener(this)
        mBinding.tgBtnMute.setOnCheckedChangeListener(this)
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView?.id) {
            R.id.tgBtn_control -> {}
            R.id.tgBtn_Mute -> {}
        }
    }

    private val permissionCallback = ActivityResultCallback<ActivityResult> { result ->
        if (result.resultCode != RESULT_FIRST_USER) return@ActivityResultCallback
        result.data?.getBooleanExtra(
            PermissionRequestViewModel.PERMISSIONS_REQUEST_STATUS,
            false
        ).let {
            if (it == false) finish()
        }
    }
}