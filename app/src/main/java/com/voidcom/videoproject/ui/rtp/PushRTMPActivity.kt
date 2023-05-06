package com.voidcom.videoproject.ui.rtp

import android.media.AudioFormat
import android.util.Log
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.activity.result.contract.ActivityResultContracts
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.EmptyViewModel
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.PermissionsUtils
import com.voidcom.videoproject.R
import com.voidcom.videoproject.databinding.ActivityPushRtmpBinding

/**
 * 实现一个RTMP推流到服务端，然后在电脑拉流播放的功能
 * 摄像头界面，包含推流控制和摄像头切换
 */
class PushRTMPActivity : BaseActivity<ActivityPushRtmpBinding, EmptyViewModel>(),
    OnCheckedChangeListener {
    private lateinit var livePusher: LivePusherNew
    private var isPushing = false

    override val mViewModel: EmptyViewModel by lazy { EmptyViewModel() }

    override fun onInitUI() {
        super.onInitUI()
        supportActionBar?.hide()
        PermissionsUtils.checkPermission(this, AppCode.requestCamera).let {
            if (it.isEmpty()) {
                initPusher()
                return
            }
            //请求权限,registerForActivityResult必须这种onStart()之前执行
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                for (i in result.iterator()) {
                    //摄像头和音频有一个权限没拿到就放弃继续
                    if (!i.value) {
                        finish()
                        return@registerForActivityResult
                    }
                }
                //推流初始化只能在权限拿到后才能开始
                initPusher()
            }.launch(
                PermissionsUtils.getPermissionsFormRequestType(
                    AppCode.requestCamera, AppCode.requestRecordAudio
                )
            )
        }
    }

    override fun onInitListener() {
        super.onInitListener()
        mBinding.btnSwitchCamera.setOnClickListener {
            livePusher.switchCamera()
        }
        mBinding.tgBtnControl.setOnCheckedChangeListener(this)
        mBinding.tgBtnMute.setOnCheckedChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isPushing) {
            isPushing = false
            livePusher.stopPush()
        }
        livePusher.release()
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView?.id) {
            R.id.tgBtn_control -> {
                isPushing = if (isChecked) {
                    livePusher.startPush(LIVE_URL, callback)
                    true
                } else {
                    livePusher.stopPush()
                    false
                }
            }

            R.id.tgBtn_Mute -> {
                Log.i(TAG, "isChecked=$isChecked")
                livePusher.setMute(isChecked)
            }
        }
    }

    private fun initPusher() {
        val videoParam = VideoParam(640, 480, Camera2Helper.CAMERA_ID_BACK.toInt(), 800000, 10)
        val audioParam =
            AudioParam(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, 2)
        if (PermissionsUtils.checkPermission(this, AppCode.requestRecordAudio).isEmpty()) {
            livePusher =
                LivePusherNew(
                    this,
                    videoParam,
                    audioParam,
                    mBinding.surfaceView,
                    CameraType.CAMERA2
                )
        }
    }

    private val callback = object : LivePusherNew.LiveErrorCallback {
        override fun onError(msg: String) {
            Log.e(TAG, "onError:$msg")
        }
    }

    companion object {
        private val TAG = PushRTMPActivity::class.java.simpleName
        private const val LIVE_URL = "rtmp://192.168.20.141/live/stream"
    }
}