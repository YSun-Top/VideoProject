package com.voidcom.videoproject.ui.rtp

import android.media.AudioFormat
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
import com.voidcom.v_base.utils.PermissionsUtils
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
    private var livePusher: LivePusherNew? = null

    override val mViewModel: EmptyViewModel by lazy { EmptyViewModel() }

    override fun onInitUI() {
        super.onInitUI()
        supportActionBar?.hide()
        val requestArray = PermissionsUtils.getPermissionsFormRequestType(
            AppCode.requestCamera, AppCode.requestRecordAudio
        )
        PermissionsUtils.checkPermission(this, AppCode.requestReadStorage).let {
            if (it.isEmpty()){
                initPusher()
                return
            }
            //请求权限
            PermissionRequestActivity.newInstance(
                this,
                registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                    permissionCallback
                ), 1000, requestArray
            )
        }
    }

    override fun onInitListener() {
        super.onInitListener()
        mBinding.btnSwitchCamera.setOnClickListener {
            livePusher?.switchCamera()
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

    private fun initPusher() {
        val videoParam = VideoParam(640, 480, Camera2Helper.CAMERA_ID_BACK.toInt(), 800000, 10)
        val audioParam =
            AudioParam(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, 2)
        livePusher =
            LivePusherNew(this, videoParam, audioParam, mBinding.surfaceView, CameraType.CAMERA2)
    }

    private val permissionCallback = ActivityResultCallback<ActivityResult> { result ->
        if (result.resultCode != RESULT_FIRST_USER) return@ActivityResultCallback
        result.data?.getBooleanExtra(
            PermissionRequestViewModel.PERMISSIONS_REQUEST_STATUS,
            false
        ).let {
            if (it == false) {
                finish()
            } else {
                //推流初始化只能在权限拿到后才能开始
                initPusher()
            }
        }
    }
}