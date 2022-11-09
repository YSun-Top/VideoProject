package com.voidcom.v_base.viewModel

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.voidcom.v_base.BuildConfig
import com.voidcom.v_base.R
import com.voidcom.v_base.ui.BaseModel
import com.voidcom.v_base.ui.BaseViewModel
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
class PermissionRequestViewModel : BaseViewModel() {
    private val TAG = PermissionRequestViewModel::class.simpleName
    private lateinit var permissions: Array<String>
    private var permissionRequestCode = -1

    override fun getModel(): BaseModel? = null

    override fun onInit(context: Context) {

    }

    override fun onInitData() {
        getActivity().let {
            if (it == null) {
                KLog.w(TAG, "activity 不能为空")
                return@let
            }
            permissionRequestCode = it.intent.getIntExtra(requestCodeFlag, -1)
            if (permissionRequestCode == -1)
                throw RuntimeException("请通过 newInstance 跳转该页面")
            permissions = it.intent.getStringArrayExtra(permissionsFlag).run {
                if (!this.isNullOrEmpty()) return@run this
                PermissionsUtils.getPermissionsFormRequestType(permissionRequestCode)
            }
            checkPermission(permissions)
        }
    }

    private fun checkPermission(array: Array<String>) {
        val context = getActivity()?.applicationContext ?: return
        array.forEach { permissionStr ->
            if (ContextCompat.checkSelfPermission(context, permissionStr) == PackageManager.PERMISSION_DENIED
            ) {
                getActivity()?.let {activity->
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionStr)) {
                        KLog.i(TAG, "已设置拒绝授予权限且不在显示，请前往设置手动设置权限:$permissionStr")
                        (activity as PermissionRequestActivity).apply {
                            createDialog(
                                getString(R.string.requestPermissionTitle),
                                getString(R.string.goToSetting),
                                getString(R.string.accept)
                            ) { _: DialogInterface?, _: Int ->
                                PermissionsUtils.gotoPermissionSetting(
                                    applicationContext,
                                    BuildConfig.LIBRARY_PACKAGE_NAME
                                )
                            }
                            backResult(false)
                        }
                    } else {
                        KLog.d(TAG, "没有读写或录音权限，请求权限")
                        (activity as PermissionRequestActivity).apply {
                            createDialog(
                                getString(R.string.requestPermissionTitle),
                                PermissionsUtils.getStringFormRequestType(
                                    applicationContext,
                                    permissionRequestCode
                                ),
                                getString(R.string.goToApprovePermissions)
                            ) { _: DialogInterface?, _: Int ->
                                activity.registerPermission?.launch(array)
//                        ActivityCompat.requestPermissions(this, array, permissionRequestCode)
                            }
                        }
                    }
                }
            } else KLog.d(TAG, "checkPermission-拿到所有权限")
        }
    }

    fun backResult(state: Boolean, msg: String? = null) {
        getActivity()?.setResult(AppCompatActivity.RESULT_FIRST_USER, Intent().apply {
            putExtra(permissionsResultStatus, state)
            if (msg.isNullOrEmpty()) {
                putExtra("message", msg)
            }
        })
        getActivity()?.finish()
    }

    companion object {
        const val permissionsRequestCodeFlag = "PERMISSIONS_REQUEST_CODE"
        const val requestCodeFlag = "REQUEST_CODE"
        const val permissionsFlag = "PERMISSIONS"
        const val permissionsResultCodeFlag = 0x01
        const val permissionsResultStatus = "PERMISSIONS_RESULT_STATUS"
    }
}