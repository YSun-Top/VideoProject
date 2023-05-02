package com.voidcom.v_base.ui

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.voidcom.v_base.R
import com.voidcom.v_base.databinding.ActivityEmptyBinding
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.PermissionsUtils
import com.voidcom.v_base.viewModel.PermissionRequestViewModel

/**
 * 用于权限请求的Activity
 *
 * 使用方法：
 * PermissionRequestActivity.newInstance(
 *     registerForActivityResult(
 *         ActivityResultContracts.StartActivityForResult(),
 *         permissionCallback
 *     ),1000,AppCode.requestCamera
 * )
 * private val permissionCallback = ActivityResultCallback<ActivityResult> { result ->
 *     if (result.resultCode == RESULT_FIRST_USER) {
 *
 *     }
 * }
 */
class PermissionRequestActivity :
    BaseActivity<ActivityEmptyBinding, PermissionRequestViewModel>() {
    private val TAG = PermissionRequestActivity::class.java.simpleName
    private var permissionDialog: AlertDialog? = null
    var registerPermission: ActivityResultLauncher<Array<String>>? = null

    override val mViewModel by viewModels<PermissionRequestViewModel>()

    override fun onInitUI() {
        mViewModel.setActivity(this)
    }

    override fun onInitListener() {
        registerPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { map ->
            map.forEach {
                mViewModel.backResult(it.value)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (permissionDialog?.isShowing == true) permissionDialog?.dismiss()
    }

    fun createDialog(
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
                mViewModel.backResult(true)
            }
            .create()
        if (permissionDialog?.isShowing == true) permissionDialog?.dismiss()
        permissionDialog?.show()
    }

    companion object {
        fun newInstance(
            activity: Activity,
            requestCode: Int,
            permissionRequestCode: Int,
        ) {
            newInstance(
                activity, requestCode, PermissionsUtils.getPermissionsFormRequestType(
                    permissionRequestCode
                )
            )
        }

        fun newInstance(
            activity: Activity,
            requestCode: Int,
            permissions: Array<String>
        ) {
            PermissionsUtils.checkPermission(activity, AppCode.requestReadStorage).let {
                //有权限不需要再申请
                if (it.isEmpty()) return
            }
            ActivityCompat.startActivityForResult(activity, Intent().apply {
                action = AppCode.requestPermissionsAction
                putExtra(PermissionRequestViewModel.REQUEST_CODE_FLAG, requestCode)
                putExtra(PermissionRequestViewModel.PERMISSIONS_FLAG, permissions)
            }, requestCode, null)
        }

        /**
         * @param permissionRequestCode {@link com.voidcom.v_base.utils.AppCode}
         */
        fun newInstance(
            context: Context,
            launcher: ActivityResultLauncher<Intent>,
            requestCode: Int,
            permissionRequestCode: Int
        ) {
            newInstance(
                context, launcher, requestCode, PermissionsUtils.getPermissionsFormRequestType(
                    permissionRequestCode
                )
            )
        }

        fun newInstance(
            context: Context,
            launcher: ActivityResultLauncher<Intent>,
            requestCode: Int,
            permissions: Array<String>
        ) {
            PermissionsUtils.checkPermission(context, AppCode.requestReadStorage).let {
                if (it.isEmpty()) return
            }
            launcher.launch(Intent().apply {
                action = AppCode.requestPermissionsAction
                putExtra(PermissionRequestViewModel.REQUEST_CODE_FLAG, requestCode)
                putExtra(PermissionRequestViewModel.PERMISSIONS_FLAG, permissions)
            })
        }
    }
}