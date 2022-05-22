package com.voidcom.v_base.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.voidcom.v_base.BuildConfig
import com.voidcom.v_base.R
import com.voidcom.v_base.databinding.ActivityPermissionBinding
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.PermissionsUtils
import com.voidcom.v_base.utils.ToastUtils
import com.voidcom.v_base.utils.log.KLog

class PermissionRequestActivity : BaseActivity<ActivityPermissionBinding, EmptyViewModel>() {
    private val TAG = PermissionRequestActivity::class.java.simpleName
    private var registerPermission: ActivityResultLauncher<Array<String>>? = null
    private var permissionDialog: AlertDialog? = null
    private var permissionRequestCode = -1
    private var permissions: Array<String>? = null

    override val mViewModel by viewModels<EmptyViewModel>()

    override fun onInitUI() {
    }

    override fun onInitListener() {
        registerPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { map ->
            for (i in map) {
                backResult(i.value)
            }
        }
    }

    override fun onInitData() {
        permissionRequestCode = intent.getIntExtra(requestCodeFlag, -1)
        if (permissionRequestCode == -1)
            throw RuntimeException("请通过 newInstance 跳转该页面")
        permissions = intent.getStringArrayExtra(permissionsFlag)
        if (permissions == null || permissions?.isEmpty() == true)
            permissions = PermissionsUtils.getPermissionsFormRequestType(permissionRequestCode)
        checkPermission(permissions)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (permissionDialog?.isShowing == true) permissionDialog?.dismiss()
    }

    private fun checkPermission(array: Array<String>?) {
        array?.forEach {
            if (ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED) {
                KLog.d(TAG, "没有读写或录音权限，请求权限")
                createDialog(
                    getString(R.string.requestPermissionTitle),
                    PermissionsUtils.getStringFormRequestType(
                        applicationContext,
                        permissionRequestCode
                    ),
                    getString(R.string.goToApprovePermissions)
                ) { _: DialogInterface?, _: Int ->
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, it)) {
                        KLog.i(TAG, "已设置拒绝授予权限且不在显示，请前往设置手动设置权限:$it")
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
                    } else {
                        registerPermission?.launch(array)
//                        ActivityCompat.requestPermissions(this, array, permissionRequestCode)
                    }
                }
            } else KLog.d(TAG, "checkPermission-拿到所有权限")
        }
    }

    private fun createDialog(
        title: String,
        msg: String,
        positiveBtn: String,
        positiveAction: (dialog: DialogInterface?, which: Int) -> Unit
    ) {
        permissionDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton(positiveBtn, positiveAction)
            .setNegativeButton(getString(R.string.cancel)) { _: DialogInterface, _: Int ->
                backResult(true)
            }
            .create()
        if (permissionDialog?.isShowing == true) permissionDialog?.dismiss()
        permissionDialog?.show()
    }

    private fun backResult(state: Boolean, msg: String? = null) {
        setResult(permissionsResultCodeFlag, Intent().apply {
            putExtra(permissionsResultStatus, state)
            if (msg.isNullOrEmpty()) {
                putExtra("message", msg)
            }
        })
        finish()
    }

    companion object {
        private const val permissionsRequestCodeFlag = "PERMISSIONS_REQUEST_CODE"
        private const val requestCodeFlag = "REQUEST_CODE"
        private const val permissionsFlag = "PERMISSIONS"
        const val permissionsResultCodeFlag = 0x01
        const val permissionsResultStatus = "PERMISSIONS_RESULT_STATUS"

        fun newInstance(
            activity: Activity,
            requestCode: Int,
            permissionRequestCode: Int,
            permissions: Array<String>? = PermissionsUtils.getPermissionsFormRequestType(
                permissionRequestCode
            )
        ) {
            ActivityCompat.startActivityForResult(activity, Intent().apply {
                action = AppCode.requestPermissionsAction
                putExtra(permissionsRequestCodeFlag, requestCode)
                putExtra(requestCodeFlag, permissionRequestCode)
                putExtra(permissionsFlag, permissions)
            }, requestCode, null)
        }

        fun newInstance(
            launcher: ActivityResultLauncher<Intent>,
            requestCode: Int,
            permissionRequestCode: Int,
            permissions: Array<String>? = PermissionsUtils.getPermissionsFormRequestType(
                permissionRequestCode
            )
        ) {
            launcher.launch(Intent().apply {
                action = AppCode.requestPermissionsAction
                putExtra(permissionsRequestCodeFlag, requestCode)
                putExtra(requestCodeFlag, permissionRequestCode)
                putExtra(permissionsFlag, permissions)
            })
        }
    }
}