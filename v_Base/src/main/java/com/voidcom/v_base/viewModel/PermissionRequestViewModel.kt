package com.voidcom.v_base.viewModel

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.voidcom.v_base.BuildConfig
import com.voidcom.v_base.R
import com.voidcom.v_base.ui.BaseActivityViewModel
import com.voidcom.v_base.ui.BaseModel
import com.voidcom.v_base.ui.PermissionRequestActivity
import com.voidcom.v_base.utils.KLog
import com.voidcom.v_base.utils.PermissionsUtils

/**
 *
 * @Description: java类作用描述
 * @Author: Void
 * @CreateDate: 2022/11/11 12:13
 * @UpdateDate: 2022/11/11 12:13
 */
class PermissionRequestViewModel : BaseActivityViewModel<PermissionRequestActivity>() {
    private lateinit var permissions: Array<String>
    private var permissionRequestCode = -1
    private var requestCodeFlag = -1

    override fun getModel(): BaseModel? = null

    override fun onInit(context: Context) {
    }

    override fun onInitData() {
        getActivity().let {
            if (it == null) {
                KLog.w(TAG, "activity 不能为空")
                return@let
            }
            requestCodeFlag = it.intent.getIntExtra(REQUEST_CODE_FLAG, -1)
            permissions = it.intent.getStringArrayExtra(PERMISSIONS_FLAG).run {
                if (!this.isNullOrEmpty()) return@run this
                PermissionsUtils.getPermissionsFormRequestType(permissionRequestCode)
            }
            checkPermission(it, permissions)
        }
    }

    private fun checkPermission(activity: PermissionRequestActivity, array: Array<String>) {
        array.forEach { permissionStr ->
            if (PermissionsUtils.checkPermission(activity, arrayOf(permissionStr)).isNotEmpty()) {
                if (PermissionsUtils.doNotShowAgain(activity,permissionStr)) {
                    KLog.i(TAG, "已设置拒绝授予权限且不在显示，请前往设置手动设置权限:$permissionStr")
                    activity.createDialog(
                        activity.getString(R.string.requestPermissionTitle),
                        activity.getString(R.string.goToSetting),
                        activity.getString(R.string.accept)
                    ) { _: DialogInterface?, _: Int ->
                        PermissionsUtils.gotoPermissionSetting(
                            activity.applicationContext,
                            BuildConfig.LIBRARY_PACKAGE_NAME
                        )
                    }
                    backResult(false)
                } else {
                    KLog.d(TAG, "没有读写或录音权限，请求权限")
                    activity.createDialog(
                        activity.getString(R.string.requestPermissionTitle),
                        PermissionsUtils.getStringFormRequestType(
                            activity.applicationContext,
                            permissionRequestCode
                        ),
                        activity.getString(R.string.goToApprovePermissions)
                    ) { _: DialogInterface?, _: Int ->
                        activity.registerPermission?.launch(array)
//                        ActivityCompat.requestPermissions(this, array, permissionRequestCode)
                    }
                }
            } else KLog.d(TAG, "checkPermission-拿到所有权限")
        }
    }

    fun backResult(state: Boolean, msg: String? = null) {
        getActivity()?.setResult(AppCompatActivity.RESULT_FIRST_USER, Intent().apply {
            putExtra(REQUEST_CODE_FLAG, requestCodeFlag)
            putExtra(PERMISSIONS_REQUEST_STATUS, state)
            if (msg.isNullOrEmpty()) {
                putExtra("message", msg)
            }
        })
        getActivity()?.finish()
    }

    companion object {
        private val TAG = PermissionRequestViewModel::class.simpleName
        //请求代码，会随Result一起传回回调。应该用该值在回调中判断是哪条请求
        const val REQUEST_CODE_FLAG = "REQUEST_CODE"
        const val PERMISSIONS_FLAG = "PERMISSIONS"
        const val PERMISSIONS_REQUEST_STATUS = "PERMISSIONS_RESULT_STATUS"
    }
}