package com.voidcom.v_base.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.voidcom.v_base.R
import com.voidcom.v_base.databinding.ActivityPermissionBinding
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.PermissionsUtils
import com.voidcom.v_base.viewModel.PermissionRequestViewModel

class PermissionRequestActivity :
    BaseActivity<ActivityPermissionBinding, PermissionRequestViewModel>() {
    private val TAG = PermissionRequestActivity::class.java.simpleName
    private var permissionDialog: AlertDialog? = null
    var registerPermission: ActivityResultLauncher<Array<String>>? = null

    override val mViewModel by viewModels<PermissionRequestViewModel>()

    override fun onInitUI() {
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
            permissions: Array<String>? = PermissionsUtils.getPermissionsFormRequestType(
                permissionRequestCode
            )
        ) {
            ActivityCompat.startActivityForResult(activity, Intent().apply {
                action = AppCode.requestPermissionsAction
                putExtra(PermissionRequestViewModel.permissionsRequestCodeFlag, requestCode)
                putExtra(PermissionRequestViewModel.requestCodeFlag, permissionRequestCode)
                putExtra(PermissionRequestViewModel.permissionsFlag, permissions)
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
                putExtra(PermissionRequestViewModel.permissionsRequestCodeFlag, requestCode)
                putExtra(PermissionRequestViewModel.requestCodeFlag, permissionRequestCode)
                putExtra(PermissionRequestViewModel.permissionsFlag, permissions)
            })
        }
    }
}